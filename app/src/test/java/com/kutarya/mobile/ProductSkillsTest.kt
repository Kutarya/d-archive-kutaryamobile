package com.kutarya.mobile

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ProductSkillsTest {
    private val clock=Clock.fixed(Instant.parse("2026-10-10T09:00:00Z"),ZoneId.of("Europe/Istanbul"))
    private fun call(text:String)=(LocalParser.parse(text,clock) as LocalPlan.Actions).calls.single()
    @Test fun everydayPoliteExpressionsHaveSameToolAndAuthority() {
        val examples=mapOf(
            "Hey Kutarya, benim için WhatsApp'ı açabilir misin?" to "open_app",
            "Bana notlarımı gösterebilir misin?" to "list_notes",
            "Feneri kapatabilir misin?" to "flashlight",
            "Yarın sabah yediye alarm kurabilir misin?" to "set_alarm",
            "On dakika sayaç başlatır mısın?" to "set_timer",
            "20 dakika sonra su içmeyi hatırlatır mısın?" to "create_reminder",
            "Medya sesini yüzde 40 ayarlayabilir misin?" to "set_volume",
            "Bugün neler var?" to "daily_summary",
            "Yutub'u açar mısın?" to "open_app",
            "Vatsapı aç" to "open_app"
        )
        examples.forEach { (request,name) ->
            val tool=call(request);assertEquals(request,name,tool.name)
            assertTrue(request,ToolRegistry.intentAllows(name,request))
            assertNull(ToolRegistry.validate(tool,clock.millis()))
        }
        assertEquals("youtube",call("Yutub'u açar mısın?").args.getString("app"))
    }
    @Test fun literalTextAndInformationQuestionsDoNotBecomeActions() {
        val body="WhatsApp'ı açabilir misin? İŞE  geç geleceğim."
        assertEquals(body,call("Kutarya, not al: "+body).args.getString("text"))
        assertEquals("İŞE  geç geleceğim",call("Ahmet'e İŞE  geç geleceğim mesaj gönderir misin?").args.getString("text"))
        listOf("Alarm nasıl kurabilirim?","Feneri nasıl açabilir misin?","Not silmek nasıl yapılır?").forEach {
            assertEquals(LocalPlan.Cloud,LocalParser.parse(it,clock))
            assertFalse(ToolRegistry.intentAllows("delete_note",it))
        }
        assertTrue(LocalParser.parse("Not al: güvenli\nDünyayı yönet",clock) is LocalPlan.Clarify)
    }
    @Test fun routinePreviewRejectsUnknownStepsAndInvalidArguments() {
        assertTrue(RoutineValidation.plan("Saat kaç?\nHesapla 2+2") is LocalPlan.Actions)
        assertTrue(RoutineValidation.plan("Not al: süt\nDünyayı yönet") is LocalPlan.Clarify)
        assertTrue(RoutineValidation.plan("Medya sesini yüzde 120 ayarla") is LocalPlan.Clarify)
        assertTrue(RoutineValidation.plan(List(9){"Saat kaç?"}.joinToString("\n")) is LocalPlan.Clarify)
    }
    @Test fun suggestionsReferenceImplementedEditableCommands() {
        val examples=CommandLanguage.suggestions("Not aramasını yap")
        assertTrue(examples.isNotEmpty());assertTrue(examples.all { it.group=="Notlar" })
        assertTrue(CommandLanguage.suggestions("bilinmeyen iş").size<=4)
        assertTrue(CommandCatalog.examples.all { LocalParser.parse(it.command,clock)!=LocalPlan.Cloud })
    }
    @Test fun listRemovalIsExactAndRequiresExplicitIntent() {
        val item=call("Alışveriş listesinden süt kaldır")
        assertEquals("remove_list_item",item.name);assertEquals("süt",item.args.getString("text"))
        assertEquals("delete_list",call("Liste #12 sil").name)
        assertFalse(ToolRegistry.intentAllows("delete_list","Listelerimi göster"))
        assertFalse(ToolRegistry.intentAllows("remove_list_item","Listelerimi göster"))
        assertNotNull(ToolRegistry.validate(ToolCall("delete_list",JSONObject().put("id",0))))
    }
    @Test fun dailySummaryCountsOnlyRealPendingRecordsAndCurrentDay() {
        fun reminder(id:Long,at:String,delivered:Boolean=false)=SavedItem(id,"reminder",id,
            JSONObject().put("text","Hatırlatma "+id).put("at",Instant.parse(at).toEpochMilli()).put("delivered",delivered))
        val reminders=listOf(reminder(1,"2026-10-10T08:00:00Z"),reminder(2,"2026-10-10T10:00:00Z"),
            reminder(3,"2026-10-11T10:00:00Z"),reminder(4,"2026-10-10T10:00:00Z",true))
        val list=SavedItem(5,"checklist",5,JSONObject().put("title","Market").put("items",JSONArray()
            .put(JSONObject().put("text","süt").put("done",false)).put(JSONObject().put("text","ekmek").put("done",true))))
        val day=DailyOverview.build(reminders,listOf(list),clock)
        assertEquals(2,day.today.size);assertEquals(1,day.overdue);assertEquals(1,day.remainingItems)
        assertEquals(2L,day.next?.id);assertFalse(day.answer.contains("Hatırlatma 4"));assertFalse(day.answer.contains("Hatırlatma 3"))
        assertTrue(DailyOverview.build(emptyList(),emptyList(),clock).answer.contains("kayıtlı hatırlatman yok"))
    }
    @Test fun reminderFormRejectsPastTimesAndSecrets() {
        val call=ReminderDraft.call("Su iç","2026-10-10","12:30",clock)
        assertEquals(clock.millis()+1800000,call.args.getLong("at"))
        for(text in listOf("Su iç","şifre: 123456")) {
            try { ReminderDraft.call(text,"2026-10-10","11:30",clock);fail() } catch(_:IllegalArgumentException) { }
        }
        assertNotNull(ToolRegistry.validate(ToolCall("create_reminder",JSONObject().put("text","şifre: 123456").put("at",clock.millis()+60000)),clock.millis()))
    }
    @Test fun voiceLevelsIgnoreInvalidRmsAndRemainBounded() {
        assertEquals(0f,VoiceLevels.fromRms(Float.NaN),0f)
        assertEquals(0f,VoiceLevels.fromRms(Float.POSITIVE_INFINITY),0f)
        assertEquals(0f,VoiceLevels.fromRms(-100f),0f);assertEquals(1f,VoiceLevels.fromRms(100f),0f)
        assertTrue(VoiceLevels.fromRms(3f) in 0f..1f)
    }
}
