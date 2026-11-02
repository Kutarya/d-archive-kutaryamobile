package com.kutarya.mobile

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ResultState { VERIFIED, HANDOFF, FAILED, BLOCKED, CANCELLED }
data class ToolCall(val name: String, val args: JSONObject = JSONObject(), val id: String = UUID.randomUUID().toString())
data class ToolResult(val state: ResultState, val message: String, val data: JSONObject = JSONObject(), val privateData: Boolean = false) {
    fun forModel(sharePrivate: Boolean): JSONObject = JSONObject()
        .put("state", state.name).put("message", if (privateData) "Kişisel veri cihazda gösterildi." else message)
        .put("data", if (privateData && !sharePrivate) JSONObject().put("withheld", true) else data)
}
data class Step(val name: String, val state: ResultState, val message: String)
data class TaskRun(
    val id: String = UUID.randomUUID().toString(), val request: String,
    val created: Long = System.currentTimeMillis(), val status: String = "Çalışıyor",
    val answer: String = "", val steps: List<Step> = emptyList(),
    val conversationId: String = "", val privateContext: Boolean = false,
    val contextEligible: Boolean = false
) {
    fun json(): JSONObject = JSONObject().put("id", id).put("request", request).put("created", created)
        .put("status", status).put("answer", answer).put("steps", JSONArray().also { a ->
            steps.forEach { a.put(JSONObject().put("name", it.name).put("state", it.state.name).put("message", it.message)) }
        }).put("conversationId", conversationId).put("privateContext", privateContext)
        .put("contextEligible", contextEligible)
    companion object {
        fun from(j: JSONObject): TaskRun = TaskRun(j.getString("id"), j.getString("request"),
            j.getLong("created"), j.optString("status"), j.optString("answer"),
            j.optJSONArray("steps")?.let { a -> (0 until a.length()).map { i ->
                val s = a.getJSONObject(i); Step(s.getString("name"), ResultState.valueOf(s.getString("state")), s.getString("message"))
            } } ?: emptyList(), j.optString("conversationId"), j.optBoolean("privateContext"),
            j.optBoolean("contextEligible"))
    }
}
data class SavedItem(val id: Long, val kind: String, val created: Long, val value: JSONObject)
data class Attachment(val name: String, val mime: String, val bytes: ByteArray)
data class AppSettings(
    val model: String = "gemini-3.1-flash-lite", val cloudConsent: Boolean = false,
    val sharePrivate: Boolean = false, val onlineSpeech: Boolean = false,
    val speak: Boolean = true, val directCall: Boolean = false,
    val theme: String = "system", val reduceMotion: Boolean = false,
    val notifications: Boolean = false, val voiceSubmit: Boolean = false
) {
    fun json(): JSONObject = JSONObject().put("model", model).put("cloudConsent", cloudConsent)
        .put("sharePrivate", sharePrivate).put("onlineSpeech", onlineSpeech).put("speak", speak)
        .put("directCall", directCall).put("theme", theme).put("reduceMotion", reduceMotion).put("notifications", notifications)
        .put("voiceSubmit", voiceSubmit)
    companion object {
        fun from(j: JSONObject) = AppSettings(j.optString("model", "gemini-3.1-flash-lite"),
            j.optBoolean("cloudConsent"), j.optBoolean("sharePrivate"), j.optBoolean("onlineSpeech"),
            j.optBoolean("speak", true), j.optBoolean("directCall"), j.optString("theme", "system"),
            j.optBoolean("reduceMotion"), j.optBoolean("notifications"), j.optBoolean("voiceSubmit"))
    }
}
data class Approval(val title: String, val detail: String, val accept: String = "Onayla")
data class Choice(val label: String, val value: String)
sealed class UiRequest {
    data class Launch(val action: DeviceAction, val permission: String?, val approval: Approval?) : UiRequest()
    data class Select(val title: String, val choices: List<Choice>) : UiRequest()
}
data class DeviceAction(val type: String, val args: JSONObject)
data class UiReply(val allowed: Boolean, val value: String = "")
sealed class LocalPlan {
    data class Actions(val calls: List<ToolCall>) : LocalPlan()
    data class Clarify(val message: String) : LocalPlan()
    data object Cloud : LocalPlan()
}
