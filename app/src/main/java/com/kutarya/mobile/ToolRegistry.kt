package com.kutarya.mobile

import org.json.JSONArray
import org.json.JSONObject

data class Field(val type: String, val required: Boolean = true, val max: Int = 8000)
data class ToolSpec(val name: String, val title: String, val description: String, val fields: Map<String, Field>, val writes: Boolean = false, val privateRead: Boolean = false)

object ToolRegistry {
    val tools = listOf(
        ToolSpec("device_status", "Telefon durumu", "Cihaz saati, tarih ve pil durumu.", emptyMap()),
        ToolSpec("daily_summary","Günlük özet","Bugünkü yerel hatırlatmalar ve tamamlanmamış liste maddeleri.",emptyMap(),privateRead=true),
        ToolSpec("calculate", "Hesaplama", "Sayılarla matematik işlemi hesapla.", mapOf("expression" to Field("STRING", max=300))),
        ToolSpec("create_note", "Not oluştur", "Kullanıcının istediği notu cihazda kaydet.", mapOf("text" to Field("STRING")) , true),
        ToolSpec("list_notes", "Notları göster", "Yereldeki notları göster; kişisel içerik aktarımı ayrıca izinlidir.", emptyMap(), privateRead=true),
        ToolSpec("delete_note", "Not sil", "Kimliği belirtilen notu onayla sil.", mapOf("id" to Field("INTEGER")), true),
        ToolSpec("create_reminder", "Hatırlatma", "Gelecekteki Unix milisaniye zamanı için yerel hatırlatma kaydet.", mapOf("text" to Field("STRING", max=500), "at" to Field("INTEGER")), true),
        ToolSpec("set_alarm", "Alarm", "Saat uygulamasında alarm isteği aç; alarm sonucu ayrıca kontrol edilir.", mapOf("hour" to Field("INTEGER"), "minute" to Field("INTEGER"), "label" to Field("STRING", false,100), "tomorrow" to Field("BOOLEAN",false)), true),
        ToolSpec("set_timer", "Sayaç", "Sistem sayacı için saniye süresi.", mapOf("seconds" to Field("INTEGER")),true),
        ToolSpec("open_app", "Uygulama aç", "Cihazda yüklü uygulamayı adıyla aç.", mapOf("app" to Field("STRING", max=120)),true),
        ToolSpec("call_contact", "Telefon araması", "Yerelde kişi/numara çözümle ve arama işlemi aç. Rehberi modelden sorgulama.", mapOf("contact" to Field("STRING",max=120)),true),
        ToolSpec("compose_message", "Mesaj taslağı", "SMS veya WhatsApp taslağı hazırla; gönderildi sayma.", mapOf("contact" to Field("STRING",max=120), "text" to Field("STRING",max=2000), "app" to Field("STRING",false,40)),true),
        ToolSpec("compose_email", "E-posta taslağı", "E-posta uygulamasında alıcı, başlık ve metinle taslak aç.", mapOf("to" to Field("STRING",max=200), "subject" to Field("STRING",max=200), "body" to Field("STRING",max=8000)),true),
        ToolSpec("calendar_event", "Takvim taslağı", "Takvimde kullanıcının kaydedeceği etkinlik taslağı aç.", mapOf("title" to Field("STRING",max=200), "start" to Field("INTEGER"), "duration_minutes" to Field("INTEGER")),true),
        ToolSpec("open_map", "Harita", "Adres veya mekân aramasını haritada aç.", mapOf("query" to Field("STRING",max=400)),true),
        ToolSpec("research", "Web araştırması", "Güncel bilgi için kaynaklı web araştırması; anahtar yoksa tarayıcıya aktar.", mapOf("query" to Field("STRING",max=2000))),
        ToolSpec("read_notifications", "Bildirimler", "Kullanıcının izin verdiği yerel bildirimlerin özetini göster.",emptyMap(),privateRead=true),
        ToolSpec("offline_utility","Yerel hesap","Birim, yüzde, tarih veya metin işlemini cihazda yap.",mapOf("request" to Field("STRING"))),
        ToolSpec("search_notes","Not ara","Kullanıcının notlarını cihazda anahtar kelimelerle ara.",mapOf("query" to Field("STRING",max=200)),privateRead=true),
        ToolSpec("edit_note","Not düzenle","Kimliği belirtilen notu yeni metinle güncelle.",mapOf("id" to Field("INTEGER"),"text" to Field("STRING")),true),
        ToolSpec("list_reminders","Hatırlatmalar","Cihazdaki bekleyen hatırlatmaları göster.",emptyMap(),privateRead=true),
        ToolSpec("cancel_reminder","Hatırlatma iptali","Hatırlatmayı hedefi gösterip onayla kaldır.",mapOf("id" to Field("INTEGER")),true),
        ToolSpec("create_list","Liste oluştur","İşaretlenebilir bir liste oluştur.",mapOf("title" to Field("STRING",max=100),"entries" to Field("STRING",false,4000)),true),
        ToolSpec("read_lists","Listeleri göster","Yereldeki işaretlenebilir listeleri oku.",mapOf("title" to Field("STRING",false,100)),privateRead=true),
        ToolSpec("delete_list","Liste sil","Kimliği belirtilen listeyi içeriğini gösterip onayla sil.",mapOf("id" to Field("INTEGER")),true),
        ToolSpec("remove_list_item","Liste maddesini kaldır","Liste ve tam madde adıyla, onayla kaldır.",mapOf("title" to Field("STRING",max=100),"text" to Field("STRING",max=200)),true),
        ToolSpec("add_list_item","Listeye ekle","Adı verilen listeye bir madde ekle.",mapOf("title" to Field("STRING",max=100),"text" to Field("STRING",max=200)),true),
        ToolSpec("check_list_item","Liste maddesi","Adı verilen listedeki maddeyi tamamla veya geri al.",mapOf("title" to Field("STRING",max=100),"text" to Field("STRING",max=200),"done" to Field("BOOLEAN")),true),
        ToolSpec("flashlight","El feneri","Cihazın fenerini aç veya kapat.",mapOf("enabled" to Field("BOOLEAN")),true),
        ToolSpec("set_volume","Medya sesi","Medya sesini yüzde olarak ayarla; zil ve alarm sesine dokunma.",mapOf("percent" to Field("INTEGER")),true),
        ToolSpec("open_settings","Telefon ayarları","İlgili Android ayar ekranını aç.",mapOf("panel" to Field("STRING",max=30)),true)
    ).associateBy { it.name }

    fun validate(call: ToolCall, now: Long = System.currentTimeMillis()): String? {
        val spec = tools[call.name] ?: return "Bu araç kayıtlı değil."
        if (call.args.keys().asSequence().any { it !in spec.fields }) return "Araçta bilinmeyen parametre var."
        for ((name, field) in spec.fields) {
            if (!call.args.has(name)) { if (field.required) return "$name bilgisi eksik."; else continue }
            val value = call.args.opt(name)
            when (field.type) {
                "STRING" -> if (value !is String || value.isBlank() || value.length > field.max || value.any { it.code < 32 && it !in "\n\t" })
                    return "$name metni geçersiz."
                "INTEGER" -> if (value !is Number || !value.toDouble().isFinite() || value.toDouble() != value.toLong().toDouble()) return "$name tam sayı olmalı."
                "BOOLEAN" -> if (value !is Boolean) return "$name seçimi geçersiz."
            }
        }
        when (call.name) {
            "set_alarm" -> if (call.args.getLong("hour") !in 0..23 || call.args.getLong("minute") !in 0..59) return "Alarm saati geçersiz."
            "set_timer" -> if (call.args.getLong("seconds") !in 1..86400) return "Sayaç 1 saniye ile 24 saat arasında olmalı."
            "create_reminder" -> {
                if (call.args.getLong("at") !in (now+1000)..(now+366L*86400000L)) return "Hatırlatma gelecekte, en fazla bir yıl sonra olmalı."
                if(Privacy.sensitive(call.args.getString("text"))) return "Anahtar ve parolaları hatırlatmalara kaydetmiyorum."
            }
            "calendar_event" -> if (call.args.getLong("start") !in (now-60000)..(now+366L*86400000L) || call.args.getLong("duration_minutes") !in 5..1440) return "Takvim zamanı veya süresi geçersiz."
            "delete_note","cancel_reminder" -> if (call.args.getLong("id") < 0) return "Kayıt kimliği geçersiz."
            "delete_list" -> if(call.args.getLong("id")<=0) return "Liste kimliği geçersiz."
            "edit_note" -> if(call.args.getLong("id")<=0 || Privacy.sensitive(call.args.getString("text"))) return "Not kimliği veya içerik geçersiz."
            "create_note" -> if (Privacy.sensitive(call.args.getString("text"))) return "Anahtar, parola ve doğrulama kodlarını notlara kaydetmiyorum."
            "set_volume" -> if(call.args.getLong("percent") !in 0..100) return "Ses düzeyi 0 ile 100 arasında olmalı."
            "open_settings" -> if(call.args.getString("panel") !in setOf("wifi","bluetooth","display","location","sound")) return "Bu ayar desteklenmiyor."
            "create_list","add_list_item","check_list_item" -> if(Privacy.sensitive(call.args.toString())) return "Bu içeriği listeye kaydetmiyorum."
            "compose_email" -> if (!Regex("^[A-Za-z0-9.!#$%&'*+/=?^_{}|~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(call.args.getString("to"))) return "E-posta adresi geçersiz."
        }
        return null
    }

    /** İzin, dış kaynak içeriğinden değil yalnız kullanıcının görevinden türetilir. */
    fun intentAllows(name: String, request: String): Boolean {
        val n = LocalParser.normalize(CommandLanguage.canonical(request))
        if (tools[name]?.writes==true && LocalParser.informational(n) &&
            (LocalParser.parse(request) as? LocalPlan.Actions)?.calls?.none { it.name==name }!=false) return false
        fun verb(vararg words:String)=Regex("(?:^|[^a-z0-9])(?:${words.joinToString("|")})(?:$|[^a-z0-9])").containsMatchIn(n)
        return when (name) {
            "device_status", "calculate", "research", "offline_utility" -> true
            "create_note" -> n.contains("not al") || n.contains("not et") || (n.contains("not") && verb("kaydet","kayda")) || verb("hatirla")
            "delete_note" -> n.contains("not") && verb("sil","kaldir")
            "list_notes" -> n.contains("not")
            "daily_summary" -> n in setOf("gunumu ozetle","bugunku planimi ozetle","bugun neler var","gunluk ozet")
            "create_reminder" -> n.contains("hatirlat")
            "set_alarm" -> n.contains("alarm") && verb("kur","kurar","ayarla","ayarlayabilir")
            "set_timer" -> listOf("sayac", "zamanlayici").any(n::contains) && verb("baslat","kur","ac","ayarla")
            "open_app" -> verb("ac","acar","baslat","baslatir")
            "call_contact" -> (verb("ara","arar","arayabilir") || n.contains("telefon et")) && !n.contains("internette")
            "compose_message" -> (n.contains("mesaj") && verb("yaz","gonder","at","hazirla")) || n.contains("haber ver") ||
                (LocalParser.parse(request) as? LocalPlan.Actions)?.calls?.any { it.name=="compose_message" }==true
            "compose_email" -> listOf("e-posta", "eposta", "mail").any(n::contains) && verb("yaz","gonder","hazirla","taslak")
            "calendar_event" -> listOf("takvim", "toplanti", "etkinlik").any(n::contains) && verb("ekle","olustur","kaydet")
            "open_map" -> listOf("harita", "yol tarifi", "navigasyon", "en yakin", "adres").any(n::contains)
            "read_notifications" -> n.contains("bildirim")
            "search_notes" -> n.contains("not") && verb("ara","bul")
            "edit_note" -> n.contains("not") && verb("guncelle","duzenle")
            "list_reminders" -> n.contains("hatirlatma") || n.contains("planim")
            "cancel_reminder" -> n.contains("hatirlatma") && verb("iptal","sil")
            "create_list" -> n.contains("listesi") && verb("olustur","hazirla")
            "read_lists" -> n.contains("liste")
            "delete_list" -> n.contains("liste") && verb("sil")
            "remove_list_item" -> n.contains("listesinden") && verb("kaldir","sil")
            "add_list_item" -> n.contains("listesine") && verb("ekle")
            "check_list_item" -> n.contains("listesinde") && verb("tamamla","geri")
            "flashlight" -> (n.contains("fener") || n.contains("flas")) && verb("ac","kapat")
            "set_volume" -> n.contains("ses") && verb("ayarla","yap","getir")
            "open_settings" -> (LocalParser.parse(request) as? LocalPlan.Actions)?.calls?.any { it.name=="open_settings" }==true
            else -> false
        }
    }

    fun declarations(request: String): JSONArray = JSONArray().also { array ->
        tools.values.filter { intentAllows(it.name, request) }.forEach { spec ->
            val props = JSONObject()
            spec.fields.forEach { (name, field) -> props.put(name, JSONObject().put("type", field.type)) }
            val parameters = JSONObject().put("type","OBJECT").put("properties",props)
            val required = spec.fields.filterValues { it.required }.keys
            if (required.isNotEmpty()) parameters.put("required", JSONArray(required.toList()))
            array.put(JSONObject().put("name",spec.name).put("description",spec.description).put("parameters",parameters))
        }
    }
}
object Privacy {
    fun sensitive(value: String): Boolean = Regex("(?:AIza[0-9A-Za-z_-]{20,}|sk-[0-9A-Za-z_-]{12,})").containsMatchIn(value) ||
        Regex("(?:sifre|parola|password|api anahtar|api key|otp|dogrulama kod).{0,25}(?:[:=]|[0-9]{4,})").containsMatchIn(LocalParser.normalize(value))
    fun notification(value: String): String? = if (sensitive(value) || Regex("(?:otp|verification|dogrulama|guvenlik kod)").containsMatchIn(LocalParser.normalize(value))) null else value.take(1500)
}
