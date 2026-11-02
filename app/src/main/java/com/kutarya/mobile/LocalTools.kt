package com.kutarya.mobile

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Kalıcı işlemler aynı şifreli depoyu ve görünür onay kapısını kullanır. */
class LocalTools(private val context:Context,private val store:LocalStore,private val ui:Interaction) {
    suspend fun execute(call:ToolCall):ToolResult? {
        val a=call.args
        return when(call.name) {
            "daily_summary" -> {
                val day=DailyOverview.build(store.list("reminder",1000),store.list("checklist",1000))
                ToolResult(ResultState.VERIFIED,day.answer,JSONObject().put("answer",day.answer),true)
            }
            "offline_utility" -> try {
                val answer=OfflineUtilities.reply(a.getString("request"))
                if(answer==null) ToolResult(ResultState.FAILED,"Bu yerel hesabı anlayamadım.")
                else ToolResult(ResultState.VERIFIED,answer,JSONObject().put("result",answer))
            } catch(e:IllegalArgumentException) { ToolResult(ResultState.FAILED,e.message ?: "Hesap geçersiz.") }
            "search_notes" -> {
                val terms=LocalParser.normalize(a.getString("query")).split(" ").filter(String::isNotBlank)
                val rows=store.list("note",1000).filter { note -> terms.all { LocalParser.normalize(note.value.optString("text")).contains(it) } }.take(30)
                ToolResult(ResultState.VERIFIED,if(rows.isEmpty()) "Eşleşen not yok." else rows.joinToString("\n\n") { "#"+it.id+" · "+it.value.optString("text") },
                    JSONObject().put("notes",JSONArray(rows.map { JSONObject().put("id",it.id).put("text",it.value.optString("text")) })),true)
            }
            "edit_note" -> {
                val note=store.list("note",1000).firstOrNull { it.id==a.getLong("id") }
                if(note==null) ToolResult(ResultState.FAILED,"Bu not bulunamadı.")
                else {
                    val approved=ui.request(UiRequest.Launch(DeviceAction("approve",JSONObject()),null,
                        Approval("Notu güncelle",a.getString("text").take(1000),"Kaydet"))).allowed
                    if(!approved) ToolResult(ResultState.CANCELLED,"Not düzenleme iptal edildi.")
                    else {
                        store.put("note",JSONObject(note.value.toString()).put("text",a.getString("text")),note.id)
                        val verified=store.list("note",1000).any { it.id==note.id && it.value.optString("text")==a.getString("text") }
                        ToolResult(if(verified) ResultState.VERIFIED else ResultState.FAILED,if(verified) "Not güncellendi." else "Not güncellenemedi.",JSONObject().put("id",note.id))
                    }
                }
            }
            "list_reminders" -> {
                val rows=store.list("reminder",1000).filter { !it.value.optBoolean("delivered") }.sortedBy { it.value.optLong("at") }.take(30)
                ToolResult(ResultState.VERIFIED,if(rows.isEmpty()) "Bekleyen hatırlatman yok." else rows.joinToString("\n\n") {
                    "#"+it.id+" · "+time(it.value.getLong("at"))+"\n"+it.value.optString("text")
                },JSONObject().put("reminders",JSONArray(rows.map { JSONObject(it.value.toString()).put("id",it.id) })),true)
            }
            "cancel_reminder" -> {
                val rows=store.list("reminder",1000).filter { !it.value.optBoolean("delivered") }
                val row=if(a.getLong("id")==0L) rows.firstOrNull() else rows.firstOrNull { it.id==a.getLong("id") }
                if(row==null) ToolResult(ResultState.FAILED,"Bu hatırlatma bulunamadı.")
                else {
                    val approved=ui.request(UiRequest.Launch(DeviceAction("approve",JSONObject()),null,
                        Approval("Hatırlatmayı iptal et",time(row.value.getLong("at"))+"\n"+row.value.optString("text"),"İptal et"))).allowed
                    if(!approved) ToolResult(ResultState.CANCELLED,"Hatırlatma korundu.")
                    else {
                        ReminderScheduler.cancel(context,row.id)
                        val removed=store.delete("reminder",row.id)
                        ToolResult(if(removed) ResultState.VERIFIED else ResultState.FAILED,if(removed) "Hatırlatma iptal edildi." else "Hatırlatma kaldırılamadı.")
                    }
                }
            }
            "create_list" -> {
                val title=a.getString("title").trim()
                if(store.list("checklist",1000).any { same(it.value.optString("title"),title) }) ToolResult(ResultState.FAILED,"Bu adla bir listen var. Listeye madde ekleyebilirsin.")
                else {
                    val entries=a.optString("entries").split(Regex("""[,;\n]+""")).map(String::trim).filter(String::isNotBlank).distinct()
                    if(entries.size>60 || entries.any { it.length>200 }) ToolResult(ResultState.FAILED,"Liste en fazla 60 kısa madde içerebilir.")
                    else {
                        val data=JSONObject().put("title",title).put("items",JSONArray(entries.map { JSONObject().put("text",it).put("done",false) }))
                        val id=store.put("checklist",data)
                        val row=store.list("checklist",1000).firstOrNull { it.id==id }
                        if(row==null) ToolResult(ResultState.FAILED,"Liste kaydı doğrulanamadı.")
                        else ToolResult(ResultState.VERIFIED,"Listeyi oluşturdum.\n\n"+render(row),JSONObject().put("id",id),true)
                    }
                }
            }
            "read_lists" -> {
                val rows=store.list("checklist",1000).filter { !a.has("title") || same(it.value.optString("title"),a.getString("title")) }.take(20)
                ToolResult(ResultState.VERIFIED,if(rows.isEmpty()) "Eşleşen liste yok." else rows.joinToString("\n\n",transform=::render),
                    JSONObject().put("lists",JSONArray(rows.map { JSONObject(it.value.toString()).put("id",it.id) })),true)
            }
            "delete_list" -> {
                val row=store.list("checklist",1000).firstOrNull { it.id==a.getLong("id") }
                if(row==null) ToolResult(ResultState.FAILED,"Bu liste bulunamadı.")
                else {
                    val approved=ui.request(UiRequest.Launch(DeviceAction("approve",JSONObject()),null,
                        Approval("Listeyi sil",render(row).take(1800),"Sil"))).allowed
                    if(!approved) ToolResult(ResultState.CANCELLED,"Liste korundu.")
                    else {
                        val removed=store.delete("checklist",row.id)
                        val verified=removed && store.list("checklist",1000).none { it.id==row.id }
                        ToolResult(if(verified) ResultState.VERIFIED else ResultState.FAILED,
                            if(verified) "Liste silindi." else "Liste silinemedi.")
                    }
                }
            }
            "remove_list_item" -> {
                val matches=store.list("checklist",1000).filter { same(it.value.optString("title"),a.getString("title")) }
                if(matches.size!=1) ToolResult(ResultState.FAILED,"Bu listeyi bulamadım.")
                else {
                    val row=matches.single(); val data=JSONObject(row.value.toString()); val items=data.getJSONArray("items")
                    val targets=(0 until items.length()).filter { same(items.getJSONObject(it).getString("text"),a.getString("text")) }
                    if(targets.size!=1) ToolResult(ResultState.FAILED,"Kaldırılacak maddeyi tam adıyla belirt.")
                    else {
                        val approved=ui.request(UiRequest.Launch(DeviceAction("approve",JSONObject()),null,
                            Approval("Maddeyi kaldır",row.value.optString("title")+"\n"+items.getJSONObject(targets.single()).getString("text"),"Kaldır"))).allowed
                        if(!approved) ToolResult(ResultState.CANCELLED,"Liste maddesi korundu.")
                        else {
                            items.remove(targets.single()); store.put("checklist",data,row.id)
                            val saved=store.list("checklist",1000).firstOrNull { it.id==row.id }
                            val verified=saved?.value?.optJSONArray("items")?.toString()==items.toString()
                            ToolResult(if(verified) ResultState.VERIFIED else ResultState.FAILED,
                                if(verified) "Madde listeden kaldırıldı." else "Liste güncellenemedi.")
                        }
                    }
                }
            }
            "add_list_item","check_list_item" -> {
                val matches=store.list("checklist",1000).filter { same(it.value.optString("title"),a.getString("title")) }
                if(matches.size!=1) ToolResult(ResultState.FAILED,"Bu listeyi bulamadım. Önce liste oluştur.")
                else {
                    val row=matches.single(); val data=JSONObject(row.value.toString()); val items=data.getJSONArray("items")
                    val text=a.getString("text").trim()
                    if(call.name=="add_list_item") {
                        if(items.length()>=60) return ToolResult(ResultState.FAILED,"Liste 60 maddeye ulaştı.")
                        if((0 until items.length()).any { same(items.getJSONObject(it).getString("text"),text) })
                            return ToolResult(ResultState.VERIFIED,"Bu madde zaten listede.")
                        items.put(JSONObject().put("text",text).put("done",false))
                    } else {
                        val exact=(0 until items.length()).filter { same(items.getJSONObject(it).getString("text"),text) }
                        val candidates=if(exact.isNotEmpty()) exact else (0 until items.length()).filter { index ->
                            val term=LocalParser.normalize(text).substringBeforeLast("'",LocalParser.normalize(text))
                            listOf(term,term.dropLast(1),term.dropLast(2)).any { it.length>=2 && LocalParser.normalize(items.getJSONObject(index).getString("text"))==it }
                        }
                        if(candidates.size!=1) return ToolResult(ResultState.FAILED,"Maddeyi tam adıyla söyle.")
                        items.getJSONObject(candidates.single()).put("done",a.getBoolean("done"))
                    }
                    store.put("checklist",data,row.id)
                    val saved=store.list("checklist",1000).first { it.id==row.id }
                    val verified=saved.value.getJSONArray("items").toString()==items.toString()
                    ToolResult(if(verified) ResultState.VERIFIED else ResultState.FAILED,
                        if(verified) (if(call.name=="add_list_item") "Listeye ekledim." else "Listeyi güncelledim.")+"\n\n"+render(saved) else "Liste güncellenemedi.",privateData=true)
                }
            }
            "set_volume" -> {
                val manager=context.getSystemService(AudioManager::class.java)
                if(manager.isVolumeFixed) ToolResult(ResultState.FAILED,"Bu cihazda medya sesi sabit.")
                else try {
                    val max=manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val desired=(max*a.getInt("percent")/100.0).roundToInt().coerceIn(0,max)
                    manager.setStreamVolume(AudioManager.STREAM_MUSIC,desired,0)
                    val actual=manager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if(actual!=desired) ToolResult(ResultState.FAILED,"Android istenen ses düzeyini uygulamadı.")
                    else ToolResult(ResultState.VERIFIED,"Medya sesi %"+(if(max==0) 0 else (actual*100.0/max).roundToInt())+".",
                        JSONObject().put("volume_index",actual).put("max_index",max))
                } catch(_:SecurityException) { ToolResult(ResultState.BLOCKED,"Android ses değişikliğine izin vermedi.") }
            }
            "flashlight" -> torch(a.getBoolean("enabled"))
            else -> null
        }
    }
    private fun same(a:String,b:String)=LocalParser.normalize(a)==LocalParser.normalize(b)
    private fun time(at:Long)=Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM · HH:mm",LocalParser.tr))
    private fun render(row:SavedItem):String {
        val items=row.value.optJSONArray("items") ?: JSONArray()
        return row.value.optString("title")+"\n"+(0 until items.length()).joinToString("\n") {
            val item=items.getJSONObject(it); (if(item.optBoolean("done")) "✓ " else "○ ")+item.optString("text")
        }
    }
    private suspend fun torch(enabled:Boolean):ToolResult {
        val manager=context.getSystemService(CameraManager::class.java)
        var callback:CameraManager.TorchCallback?=null
        return try {
            val id=manager.cameraIdList.firstOrNull { manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)==true }
                ?: return ToolResult(ResultState.FAILED,"Bu cihazda el feneri yok.")
            val done=CompletableDeferred<Boolean>()
            callback=object:CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId:String,on:Boolean) { if(cameraId==id && on==enabled) done.complete(true) }
            }
            manager.registerTorchCallback(callback,Handler(Looper.getMainLooper()))
            manager.setTorchMode(id,enabled)
            val verified=withTimeoutOrNull(2500) { done.await() }==true
            if(verified) ToolResult(ResultState.VERIFIED,if(enabled) "Fener açık." else "Fener kapalı.")
            else ToolResult(ResultState.FAILED,"Fenerin durumu doğrulanamadı.")
        } catch(_:SecurityException) { ToolResult(ResultState.BLOCKED,"Android fener erişimine izin vermedi.") }
        catch(_:android.hardware.camera2.CameraAccessException) { ToolResult(ResultState.FAILED,"Kamera kullanımda veya fener kullanılamıyor.") }
        finally { callback?.let { manager.unregisterTorchCallback(it) } }
    }
}
