package com.kutarya.mobile

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ToolSafetyTest {
    @Test fun schemaRejectsUnknownToolsFieldsAndTypes() {
        assertNotNull(ToolRegistry.validate(ToolCall("shell",JSONObject().put("cmd","rm"))))
        assertNotNull(ToolRegistry.validate(ToolCall("device_status",JSONObject().put("extra",true))))
        assertNotNull(ToolRegistry.validate(ToolCall("set_timer",JSONObject().put("seconds","60"))))
        assertNotNull(ToolRegistry.validate(ToolCall("set_timer",JSONObject().put("seconds",1.5))))
        assertNotNull(ToolRegistry.validate(ToolCall("open_app",JSONObject().put("app",""))))
    }
    @Test fun numericLimitsAreEnforced() {
        for(seconds in listOf(-1L,0L,86401L,Long.MAX_VALUE)) assertNotNull(ToolRegistry.validate(ToolCall("set_timer",JSONObject().put("seconds",seconds))))
        assertNull(ToolRegistry.validate(ToolCall("set_timer",JSONObject().put("seconds",60))))
        assertNotNull(ToolRegistry.validate(ToolCall("set_alarm",JSONObject().put("hour",24).put("minute",0))))
        assertNotNull(ToolRegistry.validate(ToolCall("calendar_event",JSONObject().put("title","iş").put("start",0).put("duration_minutes",60))))
    }
    @Test fun userRequestNotExternalContentGrantsEffects() {
        assertFalse(ToolRegistry.intentAllows("call_contact","Türkiye'deki müzeleri araştır"))
        assertFalse(ToolRegistry.intentAllows("delete_note","notlarımı oku"))
        assertFalse(ToolRegistry.intentAllows("set_alarm","Alarm nasıl kurulur?"))
        assertTrue(ToolRegistry.intentAllows("call_contact","Annemi ara"))
        assertTrue(ToolRegistry.intentAllows("create_note","Bunu not al"))
        assertTrue(ToolRegistry.intentAllows("calendar_event","Yarın takvime toplantı ekle"))
    }
    @Test fun secretsAndVerificationNotificationsAreFiltered() {
        assertTrue(Privacy.sensitive("API anahtarı: AIza"+"a".repeat(30)))
        assertTrue(Privacy.sensitive("parolam: 123456"))
        assertNull(Privacy.notification("Doğrulama kodunuz 382104"))
        assertNull(Privacy.notification("Your OTP is 123123"))
        assertEquals("Yeni bir mesajın var",Privacy.notification("Yeni bir mesajın var"))
        assertNotNull(ToolRegistry.validate(ToolCall("create_note",JSONObject().put("text","şifre: 123456"))))
    }
    @Test fun privateToolResultIsWithheldUnlessChosen() {
        val result=ToolResult(ResultState.VERIFIED,"gizli metin",JSONObject().put("phone","123456789"),true)
        assertTrue(result.forModel(false).getJSONObject("data").getBoolean("withheld"))
        assertFalse(result.forModel(false).toString().contains("123456789"))
        assertFalse(result.forModel(false).toString().contains("gizli metin"))
        assertEquals("123456789",result.forModel(true).getJSONObject("data").getString("phone"))
    }
    @Test fun arithmeticHasNoEvaluationOrCodeExecution() {
        assertEquals("45",SafeMath.calculate("17+28")); assertEquals("14",SafeMath.calculate("2+3*4"))
        assertEquals("20",SafeMath.calculate("(2+3)*4")); assertEquals("3.5",SafeMath.calculate("1,5 + 2"))
        assertEquals("256",SafeMath.calculate("2^8")); assertEquals("2",SafeMath.calculate("10%4"))
        listOf("Runtime.exec('cmd')","1/0","2+(3","(".repeat(33)+"1"+")".repeat(33),"1".repeat(301),"9^99999").forEach {
            try { SafeMath.calculate(it); fail("Reddedilmeliydi: $it") } catch(_:IllegalArgumentException) { }
        }
    }
}
