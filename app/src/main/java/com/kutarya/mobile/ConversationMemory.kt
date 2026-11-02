package com.kutarya.mobile

import org.json.JSONArray
import org.json.JSONObject

data class ConversationSummary(val id:String,val title:String,val updated:Long,val count:Int)

/** Eski kayıtlar okunur; yeni alanları olmayan kayıtlar AI geçmişine aktarılmaz. */
object ConversationMemory {
    fun summaries(runs:List<TaskRun>):List<ConversationSummary> = runs.groupBy { it.conversationId }
        .map { (id,rows) ->
            ConversationSummary(id,if(id.isBlank()) "Eski kayıtlar" else
                rows.minBy { it.created }.request.replace(Regex("\\s+")," ").take(70),
                rows.maxOf { it.created },rows.size)
        }.sortedByDescending { it.updated }

    fun messages(runs:List<TaskRun>,id:String):List<TaskRun> =
        runs.filter { it.conversationId==id }.sortedBy { it.created }

    fun context(runs:List<TaskRun>,id:String,sharePrivate:Boolean):JSONArray {
        if(id.isBlank()) return JSONArray()
        var remaining=12000
        val chosen=mutableListOf<Pair<String,String>>()
        for(run in messages(runs,id).asReversed()) {
            if(!run.contextEligible || run.answer.isBlank() || run.status !in setOf("AI yanıtı","Doğrulandı") ||
                run.privateContext && !sharePrivate) continue
            // Gizli bilgiler ve eski işlem yetkileri bağlam yolundan da sızamaz.
            if(Privacy.sensitive(run.request) || Privacy.sensitive(run.answer)) continue
            val question=run.request.take(3000)
            val answer=run.answer.take(5000)
            if(question.length+answer.length>remaining) break
            chosen+=question to answer
            remaining-=question.length+answer.length
            if(chosen.size==6) break
        }
        return JSONArray().also { contents ->
            chosen.asReversed().forEach { (question,answer) ->
                contents.put(GeminiClient.userContent(question))
                contents.put(JSONObject().put("role","model")
                    .put("parts",JSONArray().put(JSONObject().put("text",answer))))
            }
        }
    }
}
