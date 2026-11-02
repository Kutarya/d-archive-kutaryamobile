package com.kutarya.mobile

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ConversationTest {
    private fun run(index:Int,conversation:String="one",private:Boolean=false,eligible:Boolean=true) =
        TaskRun(id=index.toString(),request="Soru "+index,created=index.toLong(),status="AI yanıtı",answer="Yanıt "+index,
            conversationId=conversation,privateContext=private,contextEligible=eligible)
    @Test fun olderTaskAndSettingsRemainReadableWithoutOptingIntoHistoryUpload() {
        val old=JSONObject().put("id","old").put("request","eski özel not").put("created",10).put("answer","bir yanıt")
        val task=TaskRun.from(old)
        assertEquals("",task.conversationId);assertFalse(task.contextEligible)
        assertEquals(0,ConversationMemory.context(listOf(task),"",true).length())
        assertFalse(AppSettings.from(JSONObject()).voiceSubmit)
        assertEquals(task,TaskRun.from(task.json()))
        val next=run(1,private=true)
        assertEquals(next,TaskRun.from(next.json()))
    }
    @Test fun groupingUsesOriginalTaskTimeAndRetainsAllTurnsInOrder() {
        val runs=listOf(run(4,"two"),run(2),run(3),run(1),run(5,""))
        assertEquals(listOf("","two","one"),ConversationMemory.summaries(runs).map { it.id })
        val group=ConversationMemory.summaries(runs).first { it.id=="one" }
        assertEquals("Soru 1",group.title);assertEquals(3,group.count)
        assertEquals(listOf("1","2","3"),ConversationMemory.messages(runs,"one").map { it.id })
    }
    @Test fun contextUsesOnlySameConversationAndCompleteAllowedPairs() {
        val rows=listOf(run(1),run(2,"other"),run(3,private=true),run(4,eligible=false),run(5).copy(status="İptal"))
        val privateOff=ConversationMemory.context(rows,"one",false).toString()
        assertTrue(privateOff.contains("Yanıt 1"));assertFalse(privateOff.contains("Yanıt 2"))
        assertFalse(privateOff.contains("Yanıt 3"));assertFalse(privateOff.contains("Yanıt 4"));assertFalse(privateOff.contains("Yanıt 5"))
        assertTrue(ConversationMemory.context(rows,"one",true).toString().contains("Yanıt 3"))
        val content=ConversationMemory.context(rows,"one",false)
        assertEquals("user",content.getJSONObject(0).getString("role"))
        assertEquals("model",content.getJSONObject(1).getString("role"))
    }
    @Test fun contextBoundsPairsCharactersAndFiltersCredentials() {
        val rows=(1..20).map { run(it) }
        val contents=ConversationMemory.context(rows,"one",false)
        assertEquals(12,contents.length());assertTrue(contents.toString().contains("Yanıt 20"))
        assertFalse(contents.toString().contains("Yanıt 13"))
        val large=(1..8).map { run(it).copy(request="a".repeat(5000),answer="b".repeat(7000)) }
        val parts=ConversationMemory.context(large,"one",false)
        val chars=(0 until parts.length()).sumOf { parts.getJSONObject(it).getJSONArray("parts").getJSONObject(0).getString("text").length }
        assertTrue(chars<=12000);assertEquals(0,parts.length()%2)
        assertEquals(0,ConversationMemory.context(listOf(run(1).copy(answer="şifre: 123456")),"one",true).length())
    }
    @Test fun oldCallAuthorityNeverAppliesToNewQuestion()=runBlocking {
        val history=JSONArray().put(GeminiClient.userContent("Annemi ara")).put(
            JSONObject().put("role","model").put("parts",JSONArray().put(JSONObject().put("text","Arama ekranı açıldı."))))
        var executions=0
        val gateway=object:ModelGateway {
            override suspend fun generate(contents:JSONArray,declarations:JSONArray):ModelReply {
                assertEquals(3,contents.length())
                assertEquals("Dünya nedir?",contents.getJSONObject(2).getJSONArray("parts").getJSONObject(0).getString("text"))
                assertFalse(declarations.toString().contains("call_contact"))
                val content=JSONObject().put("role","model").put("parts",JSONArray())
                return ModelReply(content,"",listOf(ToolCall("call_contact",JSONObject().put("contact","112"))),"STOP",emptyList())
            }
        }
        val report=AgentRunner(gateway,{executions++;ToolResult(ResultState.HANDOFF,"açıldı")},{_,_->})
            .run("Dünya nedir?",history=history)
        assertEquals(0,executions);assertEquals(ResultState.BLOCKED,report.results.single().second.state)
        assertEquals(2,history.length())
    }
}
