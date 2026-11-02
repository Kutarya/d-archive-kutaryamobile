package com.kutarya.mobile

import org.json.JSONObject
import java.time.Clock

/** Ek araçlar tam komutlardan seçilir; dosya ve not içeriği komut olarak yorumlanmaz. */
object OfflineParser {
    fun parse(source: String, clock: Clock): LocalPlan? {
        val raw=source.trim(); val n=LocalParser.normalize(raw).trimEnd('.', '?', '!')
        fun action(name:String,args:JSONObject=JSONObject())=LocalPlan.Actions(listOf(ToolCall(name,args)))
        fun match(pattern:String)=Regex(pattern,RegexOption.IGNORE_CASE).matchEntire(raw.trimEnd('.', '?', '!'))
        if(n in setOf("hatirlatmalarim","hatirlatmalarimi goster","hatirlatmalarimi listele","planim","bugunku planim"))
            return action("list_reminders")
        match("""(?:#?(\d+)\s+(?:numaral[ıi]\s+)?hat[ıi]rlatma(?:s[ıi]n[ıi]|y[ıi])|hat[ıi]rlatma\s+#?(\d+))\s+(?:iptal\s+et|sil)""")?.let {
            return action("cancel_reminder",JSONObject().put("id",(it.groupValues[1].ifBlank { it.groupValues[2] }).toLongOrNull() ?: -1))
        }
        if(n in setOf("son hatirlatmayi iptal et","son hatirlatmami iptal et")) return action("cancel_reminder",JSONObject().put("id",0))
        match("""notlar[ıi]mda\s+(.+?)\s+(?:ara|bul)""")?.let { return action("search_notes",JSONObject().put("query",it.groupValues[1])) }
        match("""(?:not|notu)\s+#?(\d+)\s+(?:g[üu]ncelle|d[üu]zenle)[:\s]+(.+)""")?.let {
            return action("edit_note",JSONObject().put("id",it.groupValues[1].toLongOrNull() ?: -1).put("text",it.groupValues[2]))
        }
        match("""(?:#?(\d+)\s+(?:numaral[ıi]\s+)?not(?:u|umu)|not\s+#?(\d+))\s+sil""")?.let {
            return action("delete_note",JSONObject().put("id",(it.groupValues[1].ifBlank { it.groupValues[2] }).toLongOrNull() ?: -1))
        }
        if(n in setOf("son notu sil","son notumu sil")) return action("delete_note",JSONObject().put("id",0))
        match("""(.+?)\s+listesi\s+(?:olu[şs]tur|haz[ıi]rla)[:\s]*(.*)""")?.let {
            val args=JSONObject().put("title",it.groupValues[1].trim())
            if(it.groupValues[2].isNotBlank()) args.put("entries",it.groupValues[2].trim())
            return action("create_list",args)
        }
        if(n in setOf("listelerim","listelerimi goster")) return action("read_lists")
        match("""liste\s+#?(\d+)\s+sil""")?.let {
            return action("delete_list",JSONObject().put("id",it.groupValues[1].toLongOrNull() ?: -1))
        }
        match("""(.+?)\s+listesinden\s+(.+?)\s+(?:kald[ıi]r|sil)""")?.let {
            return action("remove_list_item",JSONObject().put("title",it.groupValues[1]).put("text",it.groupValues[2]))
        }
        match("""(.+?)\s+listesini\s+(?:g[öo]ster|oku)""")?.let { return action("read_lists",JSONObject().put("title",it.groupValues[1])) }
        match("""(.+?)\s+listesine\s+(.+?)\s+ekle""")?.let {
            return action("add_list_item",JSONObject().put("title",it.groupValues[1]).put("text",it.groupValues[2]))
        }
        match("""(.+?)\s+listesinde\s+(.+?)\s+(tamamla|geri\s+al)""")?.let {
            return action("check_list_item",JSONObject().put("title",it.groupValues[1]).put("text",it.groupValues[2]).put("done",LocalParser.normalize(it.groupValues[3])=="tamamla"))
        }
        if(n in setOf("feneri ac","fener ac","el fenerini ac","flasi ac")) return action("flashlight",JSONObject().put("enabled",true))
        if(n in setOf("feneri kapat","fener kapat","el fenerini kapat","flasi kapat")) return action("flashlight",JSONObject().put("enabled",false))
        Regex("""^(?:medya\s+)?(?:sesi|sesini|muzik\s+sesini)\s+(?:yuzde\s+|%)?(\d{1,3})(?:[' ]?(?:e|a|ye|ya))?\s+(?:ayarla|yap|getir)$""").matchEntire(n)?.let {
            return action("set_volume",JSONObject().put("percent",it.groupValues[1].toInt()))
        }
        val settings=mapOf("wifi" to "wifi","wi-fi" to "wifi","bluetooth" to "bluetooth","ekran" to "display",
            "parlaklik" to "display","konum" to "location","internet" to "wifi","ses" to "sound")
        for((word,panel) in settings) if(n in setOf(word+" ayarlarini ac",word+" ayarlari",word+"'u ac",word+"u ac",word+" ac"))
            return action("open_settings",JSONObject().put("panel",panel))
        // Hesap ve metin yardımcıları yan etkisizdir; desteklenmeyen metin ağ yoluna kalır.
        try { if(OfflineUtilities.reply(raw,clock)!=null) return action("offline_utility",JSONObject().put("request",raw)) }
        catch(_:IllegalArgumentException) { return action("offline_utility",JSONObject().put("request",raw)) }
        return null
    }
}

object LocalDialog {
    fun resolve(request:String,pending:String?,previousAnswer:String):String {
        val n=LocalParser.normalize(request).trimEnd('.', '?', '!')
        if(n in setOf("bunu not al","bunu kaydet","bu yaniti not al") && previousAnswer.isNotBlank())
            return "Not al: "+previousAnswer.take(7500)
        if(pending==null) return request
        val before=LocalParser.normalize(pending)
        if(LocalParser.parse(request) !is LocalPlan.Cloud) return request
        if(before in setOf("not al","bunu not al","not kaydet")) return "Not al: "+request
        if(before.contains("alarm") && LocalParser.time(request)!=null && !n.contains("alarm"))
            return pending+" "+request
        if((before.contains("sayac") || before.contains("zamanlayici")) && LocalParser.duration(request)!=null && !n.contains("sayac"))
            return pending+" "+request
        if(before.contains("hatirlat") && (LocalParser.duration(request)!=null || LocalParser.time(request)!=null) && !n.contains("hatirlat"))
            return request+" "+pending
        if(before=="ara") return request+" ara"
        if(before=="uygulama ac") return request+" aç"
        return request
    }
}
