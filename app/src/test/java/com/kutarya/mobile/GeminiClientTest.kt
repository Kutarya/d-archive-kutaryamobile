package com.kutarya.mobile

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiClientTest {
    private fun success(parts:JSONArray)=JSONObject().put("candidates",JSONArray().put(JSONObject().put("content",JSONObject().put("role","model").put("parts",parts)).put("finishReason","STOP")))
    @Test fun requestUsesHeaderNotUrlAndPreservesSignature() {
        MockWebServer().use { server ->
            val parts=JSONArray().put(JSONObject().put("functionCall",JSONObject().put("name","device_status").put("args",JSONObject()).put("id","call-123")).put("thoughtSignature","signature-fixture"))
            server.enqueue(MockResponse().setBody(success(parts).toString())); server.start()
            val result=GeminiClient(server.url("/").toString()).generate("fixture-key","gemini-3.1-flash-lite",JSONArray().put(GeminiClient.userContent("Saat kaç?")),ToolRegistry.declarations("Saat kaç?"))
            val sent=server.takeRequest(2,TimeUnit.SECONDS)!!
            assertEquals("fixture-key",sent.getHeader("x-goog-api-key")); assertFalse(sent.path!!.contains("fixture-key"))
            assertEquals("call-123",result.calls.single().id)
            assertEquals("signature-fixture",result.content.getJSONArray("parts").getJSONObject(0).getString("thoughtSignature"))
            assertTrue(JSONObject(sent.body.readUtf8()).has("tools"))
        }
    }
    @Test fun httpFailureIsMappedWithoutEchoingBody() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).setBody("private-provider-body")); server.start()
            try { GeminiClient(server.url("/").toString()).generate("key","model",JSONArray()); fail() }
            catch(e:IOException) { assertTrue(e.message!!.contains("429")); assertFalse(e.message!!.contains("private-provider-body")) }
        }
    }
    @Test fun redirectsNeverForwardKey() {
        MockWebServer().use { first -> MockWebServer().use { other ->
            other.start(); first.enqueue(MockResponse().setResponseCode(302).setHeader("Location",other.url("/leak"))); first.start()
            try { GeminiClient(first.url("/").toString()).generate("secret-fixture","model",JSONArray()); fail() } catch(_:IOException) { }
            assertEquals(0,other.requestCount)
        } }
    }
    @Test fun cancellationBeforeRequestDoesNotHitServer() {
        MockWebServer().use { server ->
            server.start(); val client=GeminiClient(server.url("/").toString()); client.cancel()
            try { client.generate("key","model",JSONArray()); fail() } catch(_:IOException) { }
            assertEquals(0,server.requestCount)
        }
    }
    @Test fun invalidModelAndRemoteTestUrlAreRejected() {
        try { GeminiClient("https://example.com/"); fail() } catch(_:IllegalArgumentException) { }
        MockWebServer().use { server -> server.start()
            try { GeminiClient(server.url("/").toString()).generate("key","../../wrong",JSONArray()); fail() } catch(_:IllegalArgumentException) { }
            assertEquals(0,server.requestCount)
        }
    }
    @Test fun thoughtTextAndUnsafeSourcesAreExcluded() {
        val response=success(JSONArray().put(JSONObject().put("text","private-thought").put("thought",true)).put(JSONObject().put("text","Yanıt")))
        response.getJSONArray("candidates").getJSONObject(0).put("groundingMetadata",JSONObject().put("groundingChunks",JSONArray()
            .put(JSONObject().put("web",JSONObject().put("title","Resmî kaynak").put("uri","https://developer.android.com")))
            .put(JSONObject().put("web",JSONObject().put("uri","javascript:evil")))))
        val result=GeminiClient.parse(response); assertEquals("Yanıt",result.text); assertEquals(1,result.sources.size)
    }
    @Test fun blockedAndEmptyRepliesAreErrors() {
        for(response in listOf(JSONObject(),JSONObject().put("candidates",JSONArray()),JSONObject().put("promptFeedback",JSONObject().put("blockReason","SAFETY")))) {
            try { GeminiClient.parse(response); fail() } catch(_:IOException) { }
        }
    }
    @Test fun modelsAreFilteredBySupportedMethod() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(JSONObject().put("models",JSONArray()
                .put(JSONObject().put("name","models/chat-fixture").put("supportedGenerationMethods",JSONArray().put("generateContent")))
                .put(JSONObject().put("name","models/embed-fixture").put("supportedGenerationMethods",JSONArray().put("embedContent")))).toString()));server.start()
            assertEquals(listOf("chat-fixture"),GeminiClient(server.url("/").toString()).models("key"))
        }
    }
}
