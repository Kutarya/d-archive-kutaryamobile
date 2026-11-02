package com.kutarya.mobile

import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class LocalParserTest {
    private val clock=Clock.fixed(Instant.parse("2026-10-09T10:00:00Z"),ZoneId.of("Europe/Istanbul"))
    private fun call(text:String):ToolCall {
        val plan=LocalParser.parse(text,clock)
        assertTrue("Yerel komut çözülemedi: $text → $plan",plan is LocalPlan.Actions)
        return (plan as LocalPlan.Actions).calls.single()
    }
    @Test fun TurkishLettersAndApostrophes() {
        assertEquals("whatsapp'i ac",LocalParser.normalize("WhatsApp’ı AÇ"))
        assertEquals("open_app",call("WhatsApp'ı aç.").name)
        assertEquals("whatsapp",call("WhatsApp'ı aç").args.getString("app"))
        assertEquals("call_contact",call("Annemi ara").name)
        assertEquals("Annemi",call("Annemi ara").args.getString("contact"))
    }
    @Test fun corpusOfPhoneCommands() {
        // 216 bağımsız alarm örneği: tüm saatler, üç dakika, üç ifade biçimi.
        for(hour in 0..23) for(minute in listOf(0,15,30)) for(prefix in listOf("","Lütfen ","Yarın ")) {
            val item=call("${prefix}${"%02d:%02d".format(hour,minute)} alarm kur")
            assertEquals("set_alarm",item.name); assertEquals(hour,item.args.getInt("hour")); assertEquals(minute,item.args.getInt("minute"))
        }
    }
    @Test fun spokenTimeAndDurations() {
        assertEquals(7 to 0,LocalParser.time("yarın sabah yediye alarm kur"))
        assertEquals(19 to 30,LocalParser.time("akşam yedi buçuk alarm kur"))
        assertEquals(6 to 45,LocalParser.time("sabah yediye çeyrek kala alarm kur"))
        assertEquals(300,call("Beş dakika sayaç başlat").args.getInt("seconds"))
        assertEquals(720,call("On iki dakika zamanlayıcı başlat").args.getInt("seconds"))
        assertNull(LocalParser.duration("0 saniye")); assertNull(LocalParser.duration("25 saat"))
    }
    @Test fun invalidTimeNeverBecomesPhoneAction() {
        assertTrue(LocalParser.parse("25:00 alarm kur") is LocalPlan.Clarify)
        assertTrue(LocalParser.parse("07:99 alarm kur") is LocalPlan.Clarify)
        assertTrue(LocalParser.parse("alarm kur") is LocalPlan.Clarify)
        assertTrue(LocalParser.parse("not al") is LocalPlan.Clarify)
    }
    @Test fun informationalRequestNeverStartsAnAlarmOrCall() {
        listOf("Alarm nasıl kurulur?","Annemi nasıl ararım?","WhatsApp nedir?","Not almak nasıl yapılır?").forEach {
            assertEquals(LocalPlan.Cloud,LocalParser.parse(it))
        }
    }
    @Test fun noteRetainsTurkishContent() {
        assertEquals("Yarın İŞ toplantısı",call("Not al: Yarın İŞ toplantısı").args.getString("text"))
        assertEquals("Alışverişte süt",call("Alışverişte süt not al").args.getString("text"))
    }
    @Test fun politeAndPunctuatedCommandsRetainTheirArguments() {
        assertEquals("Yarın süt al.",call("Lütfen not al: Yarın süt al.").args.getString("text"))
        assertEquals("Not içindeki  boşluk",call("not   al: Not içindeki  boşluk").args.getString("text"))
        assertEquals("annemi",call("Lütfen annemi ara.").args.getString("contact"))
        assertEquals("Annemi",call("Annemi arar mısın?").args.getString("contact"))
        assertEquals("İŞE  geç geleceğim",call("Lütfen Ahmet'e İŞE  geç geleceğim yaz.").args.getString("text"))
    }
    @Test fun questionInsideExplicitNoteIsContent() {
        assertEquals("create_note",call("Not al: Alarm nasıl kurulur?").name)
        assertTrue(ToolRegistry.intentAllows("create_note","Not al: Alarm nasıl kurulur?"))
    }
    @Test fun calculatorAndMapAndResearch() {
        assertEquals("17+28",call("hesapla:17+28").args.getString("expression"))
        assertEquals("calculate",call("17 + 28 kaç eder").name)
        assertEquals("open_map",call("En yakın eczaneyi bul").name)
        assertEquals("research",call("İnternette İstanbul hava durumunu ara").name)
    }
    @Test fun messageDraftDoesNotHaveSendTool() {
        val draft=call("Ahmet'e on dakika gecikeceğimi yaz")
        assertEquals("compose_message",draft.name); assertEquals("Ahmet",draft.args.getString("contact"))
        assertEquals("on dakika gecikeceğimi",draft.args.getString("text"))
    }
    @Test fun remindersUseInjectedClock() {
        assertEquals(clock.millis()+1200000,call("20 dakika sonra su içmeyi hatırlat").args.getLong("at"))
        val tomorrow=call("Yarın 15:00 toplantı ekle")
        assertEquals(Instant.parse("2026-10-10T12:00:00Z").toEpochMilli(),tomorrow.args.getLong("start"))
    }
    @Test fun boundedRoutineAndUnknownRequests() {
        val plan=LocalParser.parse("Not al: toplantı\nSaat kaç?\nHesapla 17+28") as LocalPlan.Actions
        assertEquals(listOf("create_note","device_status","calculate"),plan.calls.map { it.name })
        assertTrue(LocalParser.parse(List(9){"Saat kaç?"}.joinToString("\n")) is LocalPlan.Clarify)
        assertEquals(LocalPlan.Cloud,LocalParser.parse("Bana bir hikaye anlat"))
        assertTrue(LocalParser.parse("x".repeat(8001)) is LocalPlan.Clarify)
    }
}
