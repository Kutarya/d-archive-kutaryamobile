package com.kutarya.mobile

import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ln

/** Model veya ağ kullanmayan, yalnız desteklediği işlemleri yanıtlayan yardımcılar. */
object OfflineUtilities {
    private fun n(text: String) = LocalParser.normalize(text).trimEnd('?', '.', '!')
    private fun number(value: Double): String {
        require(value.isFinite()) { "Sonuç hesaplanamadı." }
        return BigDecimal.valueOf(value).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }
    private val words = mapOf("sifir" to 0, "bir" to 1, "iki" to 2, "uc" to 3,
        "dort" to 4, "bes" to 5, "alti" to 6, "yedi" to 7, "sekiz" to 8, "dokuz" to 9,
        "on" to 10, "yirmi" to 20, "otuz" to 30, "kirk" to 40, "elli" to 50,
        "altmis" to 60, "yetmis" to 70, "seksen" to 80, "doksan" to 90)
    fun spokenNumber(text: String): Long? {
        val source = n(text)
        source.toLongOrNull()?.let { return it }
        val tokens = source.split(" ")
        if (tokens.isEmpty() || tokens.size > 12) return null
        var total = 0L; var group = 0L
        for (token in tokens) when {
            token in words -> group += words.getValue(token)
            token == "yuz" -> group = (if (group == 0L) 1L else group) * 100
            token == "bin" -> { total += (if (group == 0L) 1L else group) * 1000; group = 0 }
            else -> return null
        }
        return (total + group).takeIf { it in 0..999999 }
    }
    fun spokenExpression(source: String): String? {
        val text = n(source).removePrefix("hesapla ").removeSuffix(" kac eder").removeSuffix(" kac")
        val parts = text.split(Regex("""\s+(arti|eksi|carpi|kere|bolu)\s+"""))
        val ops = Regex("""\s+(arti|eksi|carpi|kere|bolu)\s+""").findAll(text).map { it.groupValues[1] }.toList()
        if (ops.isEmpty() || parts.size != ops.size + 1 || ops.size > 12) return null
        val values = parts.map { spokenNumber(it) ?: return null }
        return buildString {
            append(values.first())
            ops.forEachIndexed { i, op -> append(when(op) { "arti" -> "+"; "eksi" -> "-"; "bolu" -> "/"; else -> "*" }); append(values[i+1]) }
        }
    }
    private data class UnitDef(val family: String, val factor: Double, val label: String)
    private val units = buildMap {
        fun add(family: String, factor: Double, label: String, vararg aliases: String) {
            aliases.forEach { put(it, UnitDef(family, factor, label)) }
        }
        add("length", 1.0, "m", "m", "metre", "meter")
        add("length", 1000.0, "km", "km", "kilometre")
        add("length", .01, "cm", "cm", "santimetre")
        add("length", .001, "mm", "mm", "milimetre")
        add("length", 1609.344, "mil", "mil", "mile")
        add("length", .0254, "inç", "inc", "inch")
        add("mass", 1.0, "kg", "kg", "kilogram", "kilo")
        add("mass", .001, "g", "g", "gram")
        add("mass", .000001, "mg", "mg", "miligram")
        add("mass", 1000.0, "ton", "ton")
        add("volume", 1.0, "L", "l", "litre")
        add("volume", .001, "mL", "ml", "mililitre")
        add("time", 1.0, "saniye", "saniye", "sn")
        add("time", 60.0, "dakika", "dakika", "dk")
        add("time", 3600.0, "saat", "saat")
        add("time", 86400.0, "gün", "gun")
        add("time", 604800.0, "hafta", "hafta")
        add("data", 1.0, "B", "byte", "bayt")
        add("data", 1000.0, "KB", "kb", "kilobayt")
        add("data", 1000000.0, "MB", "mb", "megabayt")
        add("data", 1000000000.0, "GB", "gb", "gigabayt")
    }
    fun reply(source: String, clock: Clock = Clock.systemDefaultZone()): String? {
        val text = n(source)
        if (text in setOf("merhaba", "selam", "gunaydin", "iyi aksamlar"))
            return "Merhaba. Bir iş söyle, başlayalım."
        if (text in setOf("yardim", "komutlar", "neler yapabilirsin", "ne yapabilirsin", "cevrimdisi neler yapabilirsin"))
            return "Not kaydeder, arar ve düzenlerim. Hatırlatma ve listelerini yönetir, hesap yapar, birimleri çeviririm. Uygulamaları ve telefon ayarlarını açarım.\n\nDene:\n• Not al: yarın süt al\n• 5 km kaç metre?\n• Alışveriş listesi oluştur: süt, ekmek\n• 20 dakika sonra su içmeyi hatırlat\n• Notlarımda toplantı ara"
        val conversion = Regex("""^(-?\d+(?:[.,]\d+)?)\s+([a-z]+)\s+(?:kac|to)\s+([a-z]+)(?:\s+(?:eder|yapar))?$""").matchEntire(text)
            ?: Regex("""^(-?\d+(?:[.,]\d+)?)\s+([a-z]+)(?:[' ](?:yi|yu|i|u))?\s+([a-z]+)(?:ye|ya|e|a)?\s+cevir$""").matchEntire(text)
        if (conversion != null) {
            val amount = conversion.groupValues[1].replace(',', '.').toDouble()
            val from = conversion.groupValues[2]
            val rawTo = conversion.groupValues[3]
            val to=if(rawTo in units || rawTo in setOf("celsius","c","santigrat","fahrenheit","f","kelvin","k")) rawTo
                else listOf(rawTo.removeSuffix("ye"),rawTo.removeSuffix("ya"),rawTo.removeSuffix("e"),rawTo.removeSuffix("a")).firstOrNull { it in units } ?: rawTo
            val temperatures = mapOf("celsius" to "c", "santigrat" to "c", "c" to "c", "fahrenheit" to "f", "f" to "f", "kelvin" to "k", "k" to "k")
            if (from in temperatures && to in temperatures) {
                val c = when(temperatures[from]) { "f" -> (amount-32)*5/9; "k" -> amount-273.15; else -> amount }
                require(c >= -273.15) { "Mutlak sıfırın altında bir sıcaklık çevrilemez." }
                val result = when(temperatures[to]) { "f" -> c*9/5+32; "k" -> c+273.15; else -> c }
                return number(amount) + " " + from + " = " + number(result) + " " + to
            }
            val a = units[from]; val b = units[to]
            if (a != null && b != null) {
                if (a.family != b.family) return "Bu iki birim aynı türde değil."
                return number(amount) + " " + a.label + " = " + number(amount*a.factor/b.factor) + " " + b.label
            }
        }
        val percent = Regex("""^(\d+(?:[.,]\d+)?)(?:[' ](?:nin|nun|in|un)|\s+lira(?:nin)?|\s+tl(?:nin)?)?\s+(?:yuzde|%)\s*(\d+(?:[.,]\d+)?)(.*)$""").matchEntire(text)
        if (percent != null) {
            val amount = percent.groupValues[1].replace(',', '.').toDouble()
            val p = percent.groupValues[2].replace(',', '.').toDouble()
            val tail = percent.groupValues[3].trim()
            if(!Regex("""^(?:[' ]?(?:si|su|i|u))?(?:\s*(?:kac|kac eder|indirim|indirimi|indirimli|zam|artis|artisi))?$""").matches(tail)) return null
            val result = when {
                tail.contains("indirim") -> amount*(1-p/100)
                tail.contains("artis") || tail.contains("zam") -> amount*(1+p/100)
                else -> amount*p/100
            }
            return number(result) + if (tail.contains("indirim")) " · indirimli tutar" else if(tail.contains("zam") || tail.contains("artis")) " · yeni tutar" else ""
        }
        spokenExpression(source)?.let { return SafeMath.calculate(it) }
        val days = Regex("""^(\d{1,5})\s+(gun|hafta)\s+(sonra|once)(?:\s+hangi\s+gun|\s+tarih\s+ne)?$""").matchEntire(text)
        if (days != null) {
            val count = days.groupValues[1].toLong() * if(days.groupValues[2]=="hafta") 7 else 1
            val date = LocalDate.now(clock).plusDays(if(days.groupValues[3]=="once") -count else count)
            return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy EEEE", LocalParser.tr))
        }
        val until = Regex("""^(\d{1,2})[./-](\d{1,2})[./-](\d{4})(?:[' ](?:e|a|ye|ya))?\s+kac\s+gun(?:\s+kaldi)?$""").matchEntire(text)
        if (until != null) {
            val target = try { LocalDate.of(until.groupValues[3].toInt(),until.groupValues[2].toInt(),until.groupValues[1].toInt()) }
                catch(_:Exception) { return "Bu tarih geçerli değil." }
            val count = ChronoUnit.DAYS.between(LocalDate.now(clock),target)
            return when { count>0 -> count.toString()+" gün kaldı."; count<0 -> (-count).toString()+" gün geçti."; else -> "Bugün." }
        }
        val colon = source.indexOf(':')
        if (colon > 0) {
            val command = n(source.substring(0,colon)); val body=source.substring(colon+1).trim()
            if (command in setOf("metni ozetle","ozetle","kisa ozet")) return TextTools.summarize(body)
            if (command in setOf("kelimeleri say","kelime say","metni say")) return TextTools.statistics(body)
        }
        return null
    }
    fun attachment(request: String, file: Attachment): String? {
        if (file.mime != "text/plain" && file.mime != "text/markdown") return null
        require(file.bytes.size <= 200000) { "Yerel metin işlemi için dosya en fazla 200 KB olmalı." }
        val body = try { Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(file.bytes)).toString().removePrefix("\uFEFF") }
            catch(_:Exception) { throw IllegalArgumentException("Metin dosyasını UTF-8 biçiminde kaydet.") }
        require(body.none { it=='\u0000' }) { "Bu dosya düz metin değil." }
        val command=n(request)
        return when {
            command.contains("ozetle") || command.contains("ozet cikar") -> TextTools.summarize(body)
            command.contains("kelime") && command.contains("say") -> TextTools.statistics(body)
            command.startsWith("dosyada ") && command.endsWith(" ara") -> TextTools.search(body,request.trim()
                .replaceFirst(Regex("^dosyada\\s+",RegexOption.IGNORE_CASE),"")
                .replace(Regex("\\s+ara[.!?]*$",RegexOption.IGNORE_CASE),""))
            else -> null
        }
    }
}

object TextTools {
    private val word = Regex("""[\p{L}\p{N}]+""")
    private val stopWords = setOf("ve","ile","bir","bu","da","de","icin","ama","gibi","daha","olan","olarak","o","en","her","ise","mi","ki","ne","birlikte")
    fun statistics(body: String): String {
        val count=word.findAll(body).count()
        val sentences=sentences(body).size
        return count.toString()+" kelime · "+body.codePointCount(0,body.length)+" karakter · "+sentences+" cümle"
    }
    private fun sentences(body: String)=body.trim().split(Regex("""(?<=[.!?])\s+|\r?\n+""")).map(String::trim).filter(String::isNotBlank)
    fun summarize(body: String): String {
        require(body.isNotBlank()) { "Özetlenecek metni ekle." }
        require(body.length<=200000) { "Metin çok uzun." }
        val rows=sentences(body)
        if(rows.size<=3) return body.trim().take(4000)
        val frequency=mutableMapOf<String,Int>()
        val tokens=rows.map { line -> word.findAll(LocalParser.normalize(line)).map { it.value }.filter { it.length>2 && it !in stopWords }.toList() }
        tokens.forEach { list -> list.distinct().forEach { frequency[it]=(frequency[it] ?: 0)+1 } }
        val ranked=rows.indices.map { index ->
            index to (tokens[index].distinct().sumOf { ln(1.0+(frequency[it] ?: 0)) } / (1+tokens[index].size).toDouble().let { kotlin.math.sqrt(it) })
        }.sortedByDescending { it.second }.take(3).map { it.first }.sorted()
        return ranked.joinToString("\n\n") { rows[it].take(1300) }
    }
    fun search(body: String, query: String): String {
        val terms=word.findAll(LocalParser.normalize(query)).map { it.value }.filter { it.length>1 }.toList()
        require(terms.isNotEmpty()) { "Aranacak kelimeyi söyle." }
        val matches=body.lineSequence().mapIndexedNotNull { index,line ->
            if(terms.all { LocalParser.normalize(line).contains(it) }) (index+1).toString()+": "+line.take(500) else null
        }.take(10).toList()
        return if(matches.isEmpty()) "Dosyada eşleşme yok." else matches.joinToString("\n\n")
    }
}
