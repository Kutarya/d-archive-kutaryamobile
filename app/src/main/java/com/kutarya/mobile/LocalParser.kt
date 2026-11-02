package com.kutarya.mobile

import org.json.JSONObject
import java.time.Clock
import java.time.LocalDateTime
import java.util.Locale

/** Türkçe komutlar yalnız kayıtlı araçlara dönüşür; bilinmeyen istek model yoluna gider. */
object LocalParser {
    val tr = Locale.forLanguageTag("tr-TR")
    fun normalize(text: String): String = text.lowercase(tr)
        .replace('’', '\'').replace('ı', 'i').replace('İ', 'i')
        .replace('ş', 's').replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
        .replace(Regex("\\s+"), " ").trim()

    fun parse(text: String, clock: Clock = Clock.systemDefaultZone()): LocalPlan {
        val source = text.trim()
        if (source.isEmpty()) return LocalPlan.Clarify("Ne yapmamı istediğini yaz veya söyle.")
        if (source.length > 8000) return LocalPlan.Clarify("İsteği 8.000 karakterin altında tut.")
        if(Regex("^(?:metni özetle|metni ozetle|özetle|ozetle|kelimeleri say|kelime say|metni say):",RegexOption.IGNORE_CASE).containsMatchIn(source))
            return one(source,clock)
        val chunks = source.split(Regex("\\s+(?:ve sonra|ardından|ardindan)\\s+|\\n|;",RegexOption.IGNORE_CASE))
            .map { it.trim() }.filter { it.isNotEmpty() }
            .flatMap { chunk ->
                // "Süt ve ekmek" içerik olarak kalır; ancak iki tam komut ayrılır.
                val halves=chunk.split(Regex("\\s+(?:ve|sonra)\\s+",RegexOption.IGNORE_CASE))
                val whole=(one(chunk,clock) as? LocalPlan.Actions)?.calls?.singleOrNull()
                if(whole?.name in setOf("create_note","compose_message","edit_note") ||
                    whole?.name=="offline_utility" && Regex("^(?:metni |kelimeleri |özetle:|ozetle:)",RegexOption.IGNORE_CASE).containsMatchIn(chunk)) listOf(chunk)
                else if(halves.size>1 && halves.all { one(it,clock) is LocalPlan.Actions }) halves else listOf(chunk)
            }
        if (chunks.size > 8) return LocalPlan.Clarify("Bir görevde en fazla sekiz adım kullan.")
        val calls = mutableListOf<ToolCall>()
        for (chunk in chunks) {
            when (val plan = one(chunk, clock)) {
                is LocalPlan.Actions -> calls.addAll(plan.calls)
                is LocalPlan.Clarify -> return plan
                LocalPlan.Cloud -> return if(chunks.size>1) LocalPlan.Clarify("Bu adımı anlayamadım: "+chunk.take(160)+". Komutları ayrı ayrı belirt.") else LocalPlan.Cloud
            }
        }
        return LocalPlan.Actions(calls)
    }

    private fun one(rawSource: String, clock: Clock): LocalPlan {
        val raw=CommandLanguage.canonical(rawSource)
        val n = normalize(raw).removePrefix("lutfen ").trimEnd('.', '?', '!')
            .replace(Regex("acar misin$"), "ac").replace(Regex("arar misin$"), "ara")
            .replace(Regex("bulur musun$"), "bul").replace(Regex("kurar misin$"), "kur")
        fun action(name: String, args: JSONObject = JSONObject()) = LocalPlan.Actions(listOf(ToolCall(name, args)))
        if(n=="ara") return LocalPlan.Clarify("Kimi arayayım?")
        if(n=="uygulama ac") return LocalPlan.Clarify("Hangi uygulamayı açayım?")
        if(n in setOf("gunumu ozetle","bugunku planimi ozetle","bugun neler var","gunluk ozet"))
            return action("daily_summary")
        if (n in setOf("saat kac", "bugun hangi gun", "tarih ne", "pil durumu", "sarjim ne kadar", "batarya durumu", "telefon durumu"))
            return action("device_status")
        if (n in setOf("notlarimi goster", "notlarimi oku", "notlarim", "kayitli notlarimi goster")) return action("list_notes")
        if (n in setOf("bildirimlerimi goster", "son bildirimlerimi oku", "bildirimlerimi ozetle", "bildirimlerim")) return action("read_notifications")
        if (n.startsWith("hesapla ") || n.startsWith("hesapla:")) {
            OfflineUtilities.spokenExpression(raw)?.let { return action("calculate",JSONObject().put("expression",it)) }
            return action("calculate", JSONObject().put("expression", raw.replaceFirst(Regex("(?i)^hesapla[: ]*"), "").trim()))
        }
        if (Regex("^[0-9()., +*/%^-]+(?:kac eder)?$").matches(n) && n.any { it in "+-*/" })
            return action("calculate", JSONObject().put("expression", n.removeSuffix("kac eder").trim()))
        val notePrefix = Regex("^(?:not\\s+al|not\\s+et|not\\s+kaydet|bunu\\s+not\\s+al|hat[ıi]rla)[:\\s]+",RegexOption.IGNORE_CASE)
        val prefix = notePrefix.find(raw)
        if (prefix != null) {
            val body = raw.substring(prefix.range.last + 1).trim()
            return if (body.isBlank()) LocalPlan.Clarify("Notun içeriğini de söyle.")
            else action("create_note", JSONObject().put("text", body))
        }
        if (n in setOf("not al", "bunu not al", "hatirla")) return LocalPlan.Clarify("Notun içeriğini de söyle.")
        if (n.endsWith(" not al")) return action("create_note", JSONObject().put("text", raw.replace(Regex("\\s+not\\s+al[.!?]*$",RegexOption.IGNORE_CASE),"").trim()))
        val offlineRaw=if(raw.contains(':')) raw else raw
            .replace(Regex("(?:açar mısın|acar misin)[.!?]*$",RegexOption.IGNORE_CASE),"aç")
            .replace(Regex("(?:kapatır mısın|kapatir misin)[.!?]*$",RegexOption.IGNORE_CASE),"kapat")
            .replace(Regex("(?:gösterir misin|gosterir misin)[.!?]*$",RegexOption.IGNORE_CASE),"göster")
        OfflineParser.parse(offlineRaw,clock)?.let { return it }
        if (informational(n)) return LocalPlan.Cloud
        if (n.contains("alarm") && (n.contains("kur") || n.contains("ayarla"))) {
            val time = time(n) ?: return LocalPlan.Clarify("Alarmı kaçta kurmamı istiyorsun? Örneğin sabah 07:30.")
            return action("set_alarm", JSONObject().put("hour", time.first).put("minute", time.second)
                .put("label", "Kutarya Mobile").put("tomorrow", n.contains("yarin")))
        }
        if (n.contains("sayac") || n.contains("zamanlayici")) {
            val duration = duration(n) ?: return LocalPlan.Clarify("Sayacın süresini söyle. Örneğin 10 dakika sayaç başlat.")
            return action("set_timer", JSONObject().put("seconds", duration))
        }
        if (n.contains("hatirlat")) {
            val d = duration(n)
            val target = if (d != null && (n.contains("sonra") || n.contains("icinde"))) clock.millis() + d * 1000L
            else time(n)?.let {
                var date = LocalDateTime.now(clock).withHour(it.first).withMinute(it.second).withSecond(0).withNano(0)
                if (n.contains("yarin") || !date.isAfter(LocalDateTime.now(clock))) date = date.plusDays(1)
                date.atZone(clock.zone).toInstant().toEpochMilli()
            }
            if (target == null) return LocalPlan.Clarify("Hatırlatma zamanını söyle. Örneğin 20 dakika sonra su içmeyi hatırlat.")
            val body = raw.replace(Regex("(?i)hatırlat(?:ır mısın)?[.!]?$"), "").trim()
            return action("create_reminder", JSONObject().put("text", body).put("at", target))
        }
        if (n.contains("toplanti") && (n.contains("ekle") || n.contains("olustur"))) {
            val t = time(n) ?: return LocalPlan.Clarify("Toplantının saatini söyle. Örneğin yarın 15:00'e toplantı ekle.")
            var date = LocalDateTime.now(clock).withHour(t.first).withMinute(t.second).withSecond(0).withNano(0)
            if (n.contains("yarin") || !date.isAfter(LocalDateTime.now(clock))) date = date.plusDays(1)
            val start = date.atZone(clock.zone).toInstant().toEpochMilli()
            return action("calendar_event", JSONObject().put("title", "Toplantı").put("start", start).put("duration_minutes", 60))
        }
        if (n.contains("yol tarifi") || n.contains("navigasyon") || (n.startsWith("en yakin ") && n.contains("bul"))) {
            val q = n.removePrefix("en yakin ").replace("yol tarifini ac", "").replace("yol tarifi ac", "")
                .replace("navigasyon ac", "").removeSuffix(" bul").trim()
            return if (q.isEmpty()) LocalPlan.Clarify("Hangi adresin yol tarifini açayım?")
            else action("open_map", JSONObject().put("query", q))
        }
        if (n.contains("internette") || n.startsWith("arastir ") || n.endsWith(" arastir")) {
            val q = raw.replace(Regex("(?i)internette|araştır|ara[.!]?$"), "").trim()
            return if (q.isEmpty()) LocalPlan.Clarify("Ne aramamı istiyorsun?")
            else action("research", JSONObject().put("query", q))
        }
        val msg = Regex("^(.+?)(?:'?(?:ye|ya|e|a))\\s+(.+?)\\s+(?:mesaj\\s+g[öo]nder|mesaj\\s+yaz|mesaj\\s+at|yaz)[.!?]*$",RegexOption.IGNORE_CASE).find(raw.replace('’','\''))
        if (msg != null) {
            return action("compose_message", JSONObject().put("contact", msg.groupValues[1].trim())
                .put("text", msg.groupValues[2]))
        }
        if (n.endsWith(" ara") && !n.contains("internette")) {
            val who = raw.replace(Regex("\\s+(?:ara|arar\\s+m[ıi]s[ıi]n)[.!?]*$",RegexOption.IGNORE_CASE),"").trim()
            return if (who.isEmpty()) LocalPlan.Clarify("Kimi arayayım?")
            else action("call_contact", JSONObject().put("contact", who))
        }
        if (n.endsWith(" ac") || n.endsWith(" baslat")) {
            val end = if (n.endsWith(" baslat")) 7 else 3
            var app = n.dropLast(end).trim()
            app = app.substringBefore("'")
            return if (app.isEmpty()) LocalPlan.Clarify("Hangi uygulamayı açayım?")
            else action("open_app", JSONObject().put("app", app))
        }
        if (n.endsWith(" bul") || n.endsWith(" ara")) return action("research", JSONObject().put("query", raw.dropLast(4).trim()))
        return LocalPlan.Cloud
    }

    private val numbers = mapOf("sifir" to 0, "bir" to 1, "iki" to 2, "uc" to 3, "dort" to 4,
        "bes" to 5, "alti" to 6, "yedi" to 7, "sekiz" to 8, "dokuz" to 9, "on" to 10,
        "on bir" to 11, "on iki" to 12, "on uc" to 13, "on dort" to 14, "on bes" to 15,
        "on alti" to 16, "on yedi" to 17, "on sekiz" to 18, "on dokuz" to 19,
        "yirmi" to 20, "otuz" to 30, "kirk" to 40, "elli" to 50, "altmis" to 60)

    fun informational(text:String):Boolean = listOf("nasil ","nasil?","nasil yap","nedir","ne demek","hakkinda bilgi","yontemini anlat").any(normalize(text)::contains)

    fun duration(text: String): Int? {
        val n = normalize(text)
        if(Regex("(?:^|\\s)-\\d|(?:^|\\s)eksi\\s").containsMatchIn(n)) return null
        if(n.contains("yarim saat")) return 1800
        val numeric=n.replace("bir bucuk saat","1,5 saat").replace("yarim dakika","0,5 dakika")
        val m = Regex("((?:\\d+(?:[.,]\\d+)?|(?:on|yirmi|otuz|kirk|elli|altmis|yetmis|seksen|doksan)(?: (?:bir|iki|uc|dort|bes|alti|yedi|sekiz|dokuz))?|bir|iki|uc|dort|bes|alti|yedi|sekiz|dokuz))\\s+(saniye|dakika|saat)").findAll(numeric).toList()
        if(m.isEmpty()) return null
        var seconds=0.0
        for(part in m) {
            val amount=part.groupValues[1].replace(',','.').toDoubleOrNull() ?: OfflineUtilities.spokenNumber(part.groupValues[1])?.toDouble() ?: return null
            if(amount<=0) return null
            seconds+=amount*when(part.groupValues[2]) { "saat" -> 3600; "dakika" -> 60; else -> 1 }
        }
        return seconds.takeIf { it in 1.0..86400.0 }?.toInt()
    }

    fun time(text: String): Pair<Int, Int>? {
        val n = normalize(text)
        val digits = Regex("(?:^|[^0-9])(\\d{1,2})[:.](\\d{2})(?:[^0-9]|$)").find(n)
        var hour: Int; var minute = 0
        if (digits != null) { hour = digits.groupValues[1].toInt(); minute = digits.groupValues[2].toInt() }
        else {
            val explicit = Regex("(?:saat |sabah |aksam |ogle |gece |yarin )([0-9]{1,2})(?:[' ]?(?:e|a|de|da|te|ta))?(?: |$)").find(n)
            val spoken = numbers.entries.sortedByDescending { it.key.length }.firstOrNull { (word, value) ->
                value <= 23 && Regex("(?:^| )(?:$word)(?:[' ]?(?:ye|ya|e|a|de|da|te|ta))?(?: |$)").containsMatchIn(n)
            }
            hour = explicit?.groupValues?.get(1)?.toIntOrNull() ?: spoken?.value ?: return null
            if (n.contains("bucuk")) minute = 30
            if (n.contains("ceyrek")) { minute = if (n.contains("kala")) 45 else 15; if (n.contains("kala")) hour = (hour + 23) % 24 }
        }
        if ((n.contains("aksam") || n.contains("ogle")) && hour in 1..11) hour += 12
        if (n.contains("gece") && hour == 12) hour = 0
        return if (hour in 0..23 && minute in 0..59) hour to minute else null
    }
}
