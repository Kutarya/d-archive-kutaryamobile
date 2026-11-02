package com.kutarya.mobile

import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AgentRunnerTest {
    private fun reply(vararg calls:ToolCall,text:String="Tamam")=ModelReply(JSONObject().put("role","model").put("parts",JSONArray().put(JSONObject().put("text",text))),text,calls.toList(),"STOP",emptyList())
    private class Gateway(val rows:List<ModelReply>):ModelGateway {
        var index=0; val sent=mutableListOf<JSONArray>()
        override suspend fun generate(contents:JSONArray,declarations:JSONArray):ModelReply { sent+=JSONArray(contents.toString()); return rows[(index++).coerceAtMost(rows.lastIndex)] }
    }
    @Test fun executesAndReportsActualResultNotModelsClaim()=runBlocking {
        val item=ToolCall("set_alarm",JSONObject().put("hour",7).put("minute",30),"alarm-id")
        val gateway=Gateway(listOf(reply(item),reply(text="Alarm kesin kuruldu")))
        val report=AgentRunner(gateway,{ToolResult(ResultState.HANDOFF,"Saat uygulamasına aktarıldı.")},{_,_->}).run("07:30 alarm kur")
        assertEquals("Saat uygulamasına aktarıldı.",report.answer)
        assertEquals("alarm-id",gateway.sent[1].getJSONObject(2).getJSONArray("parts").getJSONObject(0).getJSONObject("functionResponse").getString("id"))
    }
    @Test fun unsupportedToolsDoNotExecute()=runBlocking {
        var executions=0
        val report=AgentRunner(Gateway(listOf(reply(ToolCall("shell")))),{executions++;ToolResult(ResultState.VERIFIED,"x")},{_,_->}).run("Saat kaç?")
        assertEquals(0,executions); assertEquals(ResultState.BLOCKED,report.results.single().second.state)
    }
    @Test fun injectedCallInstructionCannotGrantAuthority()=runBlocking {
        var executions=0
        val malicious=ToolCall("call_contact",JSONObject().put("contact","112"))
        val report=AgentRunner(Gateway(listOf(reply(malicious))),{executions++;ToolResult(ResultState.VERIFIED,"x")},{_,_->}).run("Müze açılış saatlerini araştır")
        assertEquals(0,executions); assertEquals(ResultState.BLOCKED,report.results.single().second.state)
    }
    @Test fun duplicateWriteIsExecutedOnce()=runBlocking {
        val item=ToolCall("create_note",JSONObject().put("text","süt")); var writes=0
        val report=AgentRunner(Gateway(listOf(reply(item),reply(item.copy(id="new-id")),reply())),{writes++;ToolResult(ResultState.VERIFIED,"Not kaydedildi.")},{_,_->}).run("süt not al")
        assertEquals(1,writes); assertEquals(2,report.results.size)
    }
    @Test fun failureStopsLaterEffects()=runBlocking {
        var executions=0
        val report=AgentRunner(Gateway(listOf(reply(ToolCall("device_status"),ToolCall("calculate",JSONObject().put("expression","2+2"))))),{
            executions++;ToolResult(ResultState.FAILED,"İlk işlem başarısız.")
        },{_,_->}).run("Saat kaç? Ve 2+2 hesapla")
        assertEquals(1,executions); assertEquals("İlk işlem başarısız.",report.answer)
    }
    @Test fun privateContentIsNotSentAndCannotBeWebQuery()=runBlocking {
        var executions=0
        val gateway=Gateway(listOf(reply(ToolCall("list_notes")),reply(ToolCall("research",JSONObject().put("query","gizli telefon 555")))))
        val report=AgentRunner(gateway,{executions++;ToolResult(ResultState.VERIFIED,"gizli telefon 555",JSONObject().put("secret","555"),true)},{_,_->}).run("notlarımı oku ve internette araştır")
        assertEquals(1,executions); assertEquals(ResultState.BLOCKED,report.results.last().second.state)
        assertFalse(gateway.sent[1].toString().contains("555"))
    }
    @Test fun iterationAndStepLimitsTerminate()=runBlocking {
        var executions=0
        val report=AgentRunner(Gateway(listOf(reply(ToolCall("device_status")))),{executions++;ToolResult(ResultState.VERIFIED,"saat")},{_,_->}).run("Saat kaç?")
        assertEquals(5,executions); assertTrue(report.answer.contains("tur sınırı"))
        var count=0
        val many=reply(*Array(4){ToolCall("device_status")})
        AgentRunner(Gateway(listOf(many)),{count++;ToolResult(ResultState.VERIFIED,"saat")},{_,_->}).run("Saat kaç?")
        assertEquals(8,count)
    }
    @Test fun cancellationStopsExecution()=runBlocking {
        var executions=0; val job=launch(start=CoroutineStart.LAZY) {
            AgentRunner(Gateway(listOf(reply(ToolCall("device_status")))),{executions++;ToolResult(ResultState.VERIFIED,"x")},{_,_->}).run("Saat kaç?")
        }
        job.cancel(); job.join(); assertEquals(0,executions)
    }
    @Test fun modelCannotInventCompletedAction()=runBlocking {
        val report=AgentRunner(Gateway(listOf(reply(text="Mesajını gönderdim."))),{ToolResult(ResultState.VERIFIED,"x")},{_,_->}).run("Ahmet'e mesaj yaz")
        assertTrue(report.answer.contains("doğrulanmış araç sonucu yok"))
    }
}
