package com.kutarya.mobile

/** Söyleyişleri eşler; not ve mesaj içindeki metni yeniden yazmaz. */
object CommandLanguage {
    fun canonical(source:String):String {
        var raw=source.trim().replaceFirst(Regex(
            "^(?:(?:hey\\s+)?kutarya(?:\\s+us)?[,:]?\\s+)?(?:l[üu]tfen\\s+)?(?:benim\\s+i[çc]in\\s+|bana\\s+)?",
            RegexOption.IGNORE_CASE),"")
        if(Regex("^(?:not\\s+(?:al|et|kaydet)|bunu\\s+not\\s+al|hat[ıi]rla|not\\s+#?\\d+\\s+(?:g[üu]ncelle|d[üu]zenle))[:\\s]+",
            RegexOption.IGNORE_CASE).containsMatchIn(raw) ||
            Regex("^(?:metni\\s+|kelimeleri\\s+|[öo]zetle:)").containsMatchIn(raw.lowercase(LocalParser.tr))) return raw
        val endings=linkedMapOf(
            "(?:a[çc]ar m[ıi]s[ıi]n|a[çc]abilir misin|a[çc]abilir miyiz)" to "aç",
            "(?:arar m[ıi]s[ıi]n|arayabilir misin)" to "ara",
            "(?:bulur musun|bulabilir misin)" to "bul",
            "(?:kurar m[ıi]s[ıi]n|kurabilir misin)" to "kur",
            "(?:ayarlayabilir misin|ayarlar m[ıi]s[ıi]n)" to "ayarla",
            "(?:ba[şs]lat[ıi]r m[ıi]s[ıi]n|ba[şs]latabilir misin)" to "başlat",
            "(?:g[öo]sterir misin|g[öo]sterebilir misin|okur musun)" to "göster",
            "(?:kapat[ıi]r m[ıi]s[ıi]n|kapatabilir misin)" to "kapat",
            "(?:hat[ıi]rlat[ıi]r m[ıi]s[ıi]n|hat[ıi]rlatabilir misin)" to "hatırlat",
            "(?:ekler misin|ekleyebilir misin)" to "ekle",
            "(?:olu[şs]turur musun|olu[şs]turabilir misin|haz[ıi]rlar m[ıi]s[ıi]n)" to "oluştur",
            "(?:g[öo]nderir misin|g[öo]nderebilir misin)" to "gönder",
            "(?:siler misin|silebilir misin)" to "sil"
        )
        for((pattern,verb) in endings) {
            val regex=Regex("\\s+(?:"+pattern+")[.!?]*$",RegexOption.IGNORE_CASE)
            if(regex.containsMatchIn(raw)) { raw=raw.replace(regex," "+verb); break }
        }
        val n=LocalParser.normalize(raw).trimEnd('.', '?','!')
        val simple=mapOf(
            "bugun neler var" to "Günümü özetle", "bugun ne yapacagim" to "Günümü özetle",
            "gunluk ozet" to "Günümü özetle", "gunumu goster" to "Günümü özetle",
            "yapilacaklarim" to "Listelerimi göster", "notlarimi ac" to "Notlarımı göster",
            "hatirlatmalarimi ac" to "Hatırlatmalarımı göster",
            "pilim kac" to "Pil durumu", "sarjim kac" to "Pil durumu", "sarjim kac kaldi" to "Pil durumu",
            "saat kacti" to "Saat kaç"
        )
        if(n in simple) return simple.getValue(n)
        // Yalnız bilinen uygulama adları; yıkıcı hedeflerde yaklaşık eşleşme yok.
        val app=Regex("^(vatsap|whatsapp|yutub|youtube|kamera|spotify|instagram|gmail|chrome)(?:['’]?[ıiuüyi]{0,2})?\\s+(?:a[çc]|ba[şs]lat)[.!?]*$",
            RegexOption.IGNORE_CASE).matchEntire(raw)
        if(app!=null) {
            val name=LocalParser.normalize(app.groupValues[1])
            return (when(name) { "vatsap"->"WhatsApp";"yutub"->"YouTube";else->app.groupValues[1] })+" aç"
        }
        return raw
    }

    fun suggestions(request:String):List<CommandExample> {
        val n=LocalParser.normalize(request)
        val group=when {
            n.contains("not") -> "Notlar"
            n.contains("liste") || n.contains("alisveris") -> "Listeler"
            listOf("alarm","hatirlat","sayac","zaman","plan","bugun").any(n::contains) -> "Planlama"
            listOf("hesap","yuzde","kilo","metre","indirim").any(n::contains) -> "Hesap"
            listOf("fener","telefon","wifi","ses","ara","uygulama").any(n::contains) -> "Telefon"
            n.contains("metin") || n.contains("ozet") || n.contains("dosya") -> "Metin"
            else -> null
        }
        return (if(group==null) CommandCatalog.examples.filter { it.title in setOf("Not kaydet","Hatırlatma ekle","Liste oluştur","Hesapla") }
            else CommandCatalog.examples.filter { it.group==group }).take(4)
    }
}

object RoutineValidation {
    fun plan(commands:String):LocalPlan = when(val plan=LocalParser.parse(commands)) {
        LocalPlan.Cloud -> LocalPlan.Clarify("Rutin için desteklenen telefon veya kayıt komutlarını kullan.")
        is LocalPlan.Clarify -> plan
        is LocalPlan.Actions -> {
            val invalid=plan.calls.firstNotNullOfOrNull { ToolRegistry.validate(it) }
            if(invalid!=null) LocalPlan.Clarify(invalid) else plan
        }
    }
}
