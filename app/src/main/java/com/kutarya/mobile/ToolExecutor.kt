package com.kutarya.mobile

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

interface Interaction {
    suspend fun request(request: UiRequest): UiReply
    suspend fun research(query: String): ToolResult
}
class ToolExecutor(private val context: Context, private val store: LocalStore,
    private val settings: ()->AppSettings, private val ui: Interaction) {

    suspend fun execute(call: ToolCall): ToolResult {
        ToolRegistry.validate(call)?.let { return ToolResult(ResultState.BLOCKED,it) }
        val spec=ToolRegistry.tools.getValue(call.name)
        val locked=context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked==true
        if(locked && (spec.writes || spec.privateRead)) return ToolResult(ResultState.BLOCKED,"Bu işlem için telefonun kilidini aç.")
        val a=call.args
        LocalTools(context,store,ui).execute(call)?.let { return it }
        return when(call.name) {
            "device_status" -> {
                val battery=context.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val date=LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy EEEE, HH:mm",Locale.forLanguageTag("tr-TR")))
                ToolResult(ResultState.VERIFIED,"$date\nPil: ${if(battery!=null && battery in 0..100) "%$battery" else "bilinmiyor"}",
                    JSONObject().put("date",date).put("battery_percent",battery ?: JSONObject.NULL))
            }
            "calculate" -> {
                try { val value=SafeMath.calculate(a.getString("expression")); ToolResult(ResultState.VERIFIED,value,JSONObject().put("result",value)) }
                catch(e:IllegalArgumentException) { ToolResult(ResultState.FAILED,e.message ?: "İşlem geçersiz.") }
            }
            "create_note" -> {
                val id=store.put("note",JSONObject().put("text",a.getString("text")))
                val verified=store.list("note").any { it.id==id && it.value.getString("text")==a.getString("text") }
                ToolResult(if(verified) ResultState.VERIFIED else ResultState.FAILED,
                    if(verified) "Notu kaydettim." else "Not kaydı doğrulanamadı.",JSONObject().put("id",id))
            }
            "list_notes" -> {
                val notes=store.list("note",30)
                val rows=JSONArray().also { list -> notes.forEach { list.put(JSONObject().put("id",it.id).put("text",it.value.optString("text"))) } }
                ToolResult(ResultState.VERIFIED,if(notes.isEmpty()) "Henüz notun yok." else notes.joinToString("\n\n") { "#${it.id} · ${it.value.optString("text")}" },
                    JSONObject().put("notes",rows),true)
            }
            "delete_note" -> {
                val notes=store.list("note",1000)
                val note=if(a.getLong("id")==0L) notes.firstOrNull() else notes.firstOrNull { it.id==a.getLong("id") }
                if(note==null) ToolResult(ResultState.FAILED,"Bu not bulunamadı.")
                else {
                    val approved=ui.request(UiRequest.Launch(DeviceAction("approve",JSONObject()),null,
                        Approval("Notu sil",note.value.optString("text").take(500),"Sil"))).allowed
                    if(!approved) ToolResult(ResultState.CANCELLED,"Not silme iptal edildi.")
                    else if(store.delete("note",note.id)) ToolResult(ResultState.VERIFIED,"Not silindi.")
                    else ToolResult(ResultState.FAILED,"Not silinemedi.")
                }
            }
            "create_reminder" -> {
                if(Build.VERSION.SDK_INT>=33 && !permission(Manifest.permission.POST_NOTIFICATIONS)) return ToolResult(ResultState.BLOCKED,"Hatırlatma için bildirim izni gerekli.")
                val id=store.put("reminder",JSONObject().put("text",a.getString("text")).put("at",a.getLong("at")).put("delivered",false))
                try {
                    ReminderScheduler.schedule(context,id,a.getLong("at"))
                    ToolResult(ResultState.VERIFIED,"Hatırlatma kaydedildi. Sistem pil koşullarına göre gecikebilir.",JSONObject().put("id",id).put("at",a.getLong("at")))
                } catch(_:Exception) { store.delete("reminder",id); ToolResult(ResultState.FAILED,"Hatırlatma planlanamadı.") }
            }
            "set_alarm" -> handoff("alarm",a,"Saat uygulamasına ${"%02d:%02d".format(a.getInt("hour"),a.getInt("minute"))} alarm isteğini ilettim. Tarihi ve kaydı Saat uygulamasında kontrol et.")
            "set_timer" -> handoff("timer",a,"Sayaç isteğini Saat uygulamasına ilettim.")
            "open_app" -> openApp(a.getString("app"))
            "call_contact", "compose_message" -> {
                val contact=resolveContact(a.getString("contact"))
                if(contact==null) ToolResult(ResultState.FAILED,"Kişi seçilmedi veya bulunamadı. Numarayı söyleyerek de deneyebilirsin.")
                else {
                    val args=JSONObject(a.toString()).put("number",contact.value).put("display",contact.label)
                    val direct=call.name=="call_contact" && settings().directCall
                    if(direct && !permission(Manifest.permission.CALL_PHONE)) return ToolResult(ResultState.BLOCKED,"Doğrudan arama için telefon izni gerekli.")
                    args.put("direct",direct)
                    val detail=if(call.name=="call_contact") "${contact.label}\n${contact.value}"
                    else "${contact.label}\n\n${a.getString("text")}\n\nMesaj uygulamasında gönderimi sen tamamlayacaksın."
                    handoff(if(call.name=="call_contact") "call" else "message",args,
                        if(call.name=="call_contact") (if(direct) "Aramayı başlatma isteğini telefona ilettim." else "Arama ekranını açtım; aramayı oradan başlatabilirsin.")
                        else "Mesaj taslağını açtım. Henüz gönderilmedi.",
                        Approval(if(call.name=="call_contact") "Arama" else "Mesaj taslağı",detail,if(direct) "Ara" else "Aç"),true)
                }
            }
            "compose_email" -> handoff("email",a,"E-posta taslağını açtım. Henüz gönderilmedi.",
                Approval("E-posta taslağı","${a.getString("to")}\n${a.getString("subject")}\n\n${a.getString("body")}","Taslağı aç"))
            "calendar_event" -> handoff("calendar",a,"Takvim taslağını açtım. Kaydı takvim uygulamasında tamamla.")
            "open_map" -> handoff("map",a,"Haritada arama isteğini açtım.")
            "open_settings" -> handoff("settings",a,"Ayar ekranını açtım.")
            "research" -> ui.research(a.getString("query"))
            "read_notifications" -> {
                if(!settings().notifications) ToolResult(ResultState.BLOCKED,"Ayarlar'dan bildirim özeti tercihini ve Android bildirim erişimini aç.")
                else {
                    val rows=store.list("notification",20)
                    ToolResult(ResultState.VERIFIED,if(rows.isEmpty()) "Kaydedilmiş uygun bildirim yok." else rows.joinToString("\n\n") {
                        "${it.value.optString("app")} · ${it.value.optString("title")}\n${it.value.optString("text")}"
                    },JSONObject().put("notifications",JSONArray(rows.map { it.value })),true)
                }
            }
            else -> ToolResult(ResultState.BLOCKED,"Araç desteklenmiyor.")
        }
    }
    private suspend fun permission(name:String):Boolean {
        if(ContextCompat.checkSelfPermission(context,name)==PackageManager.PERMISSION_GRANTED) return true
        return ui.request(UiRequest.Launch(DeviceAction("permission",JSONObject()),name,null)).allowed
    }
    private suspend fun handoff(type:String,args:JSONObject,message:String,approval:Approval?=null,private:Boolean=false):ToolResult {
        val reply=ui.request(UiRequest.Launch(DeviceAction(type,args),null,approval))
        return if(reply.allowed) ToolResult(ResultState.HANDOFF,message,privateData=private)
        else ToolResult(if(reply.value=="cancel") ResultState.CANCELLED else ResultState.FAILED,
            if(reply.value=="cancel") "İşlem iptal edildi." else reply.value.ifBlank { "Bu işlemi açabilecek uygulama bulunamadı." })
    }
    private suspend fun openApp(name:String):ToolResult {
        val query=LocalParser.normalize(name).substringBefore("'")
        val aliases=mapOf("whatsapp" to "com.whatsapp", "youtube" to "com.google.android.youtube",
            "instagram" to "com.instagram.android", "spotify" to "com.spotify.music",
            "chrome" to "com.android.chrome", "gmail" to "com.google.android.gm",
            "ayarlar" to "com.android.settings", "hesap makinesi" to "com.google.android.calculator",
            "kamera" to "com.android.camera2", "saat" to "com.google.android.deskclock")
        val pm=context.packageManager
        val launch=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps=pm.queryIntentActivities(launch,0).map {
            val label=it.loadLabel(pm).toString(); Choice(label,it.activityInfo.packageName)
        }.distinctBy { it.value }
        val candidates=apps.filter { LocalParser.normalize(it.label)==query || it.value==aliases[query] }.ifEmpty {
            apps.filter { LocalParser.normalize(it.label).contains(query) && query.length>=2 }
        }
        val app=when(candidates.size) {
            0 -> return ToolResult(ResultState.FAILED,"“$name” cihazda bulunamadı.")
            1 -> candidates.first()
            else -> {
                val selected=ui.request(UiRequest.Select("Hangi uygulama?",candidates.take(20)))
                if(!selected.allowed) return ToolResult(ResultState.CANCELLED,"Uygulama seçimi iptal edildi.")
                candidates.firstOrNull { it.value==selected.value } ?: return ToolResult(ResultState.BLOCKED,"Uygulama seçimi geçersiz.")
            }
        }
        return handoff("app",JSONObject().put("package",app.value),"${app.label} uygulamasını açtım.")
    }
    private suspend fun resolveContact(value:String):Choice? {
        if(Regex("^[+0-9() -]{3,40}$").matches(value)) {
            val number=value.filter { it.isDigit() || it=='+' }
            return if(Regex("^\\+?[0-9]{3,18}$").matches(number)) Choice(value,number) else null
        }
        var n=LocalParser.normalize(value).substringBefore("'")
        val variants=(listOf(n)+listOf("yi","yu","i","u").filter(n::endsWith).map { n.dropLast(it.length) }).filter { it.length>=2 }
        for(alias in variants) {
            val saved=store.vault.get("contact:$alias")
            if(saved.isNotEmpty()) {
                val j=JSONObject(saved); val phone=j.optString("phone")
                if(Regex("^\\+?[0-9]{3,18}$").matches(phone)) return Choice(j.optString("name",value),phone)
            }
        }
        if(!permission(Manifest.permission.READ_CONTACTS)) return null
        val choices=mutableListOf<Choice>()
        context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,null)?.use { c ->
            var scanned=0
            while(c.moveToNext() && scanned++<10000) {
                val label=c.getString(0).orEmpty(); val number=c.getString(1).orEmpty().filter { it.isDigit() || it=='+' }
                val normalized=LocalParser.normalize(label)
                if(variants.any { normalized.contains(it) } && Regex("^\\+?[0-9]{3,18}$").matches(number)) choices+=Choice(label,number)
            }
        }
        val unique=choices.distinctBy { it.label+it.value }.sortedBy { if(LocalParser.normalize(it.label) in variants) 0 else 1 }
        if(unique.size==1) return unique.first()
        if(unique.isEmpty()) return null
        val selected=ui.request(UiRequest.Select("Hangi kişi?",unique.take(30).map { Choice("${it.label} · ${it.value}",it.value) }))
        return if(selected.allowed) unique.firstOrNull { it.value==selected.value } else null
    }
}
