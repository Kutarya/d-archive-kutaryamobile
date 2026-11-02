package com.kutarya.mobile

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONArray
import org.json.JSONObject

interface ModelGateway { suspend fun generate(contents:JSONArray,declarations:JSONArray):ModelReply }
data class AgentReport(val answer:String,val results:List<Pair<ToolCall,ToolResult>>)

/** Yürütme sınırları modelden bağımsızdır; testlerde gerçek aynı döngü kullanılır. */
class AgentRunner(private val gateway:ModelGateway,private val execute:suspend(ToolCall)->ToolResult,
    private val event:suspend(ToolCall,ToolResult)->Unit) {
    suspend fun run(request:String,attachment:Attachment?=null,sharePrivate:Boolean=false,
        history:JSONArray=JSONArray()):AgentReport {
        // Araç bildirimi ve yetkisi yalnız yeni istekten gelir; geçmiş yetki vermez.
        val contents=JSONArray(history.toString()).put(GeminiClient.userContent(request,attachment))
        val outcomes=mutableListOf<Pair<ToolCall,ToolResult>>()
        val writes=mutableMapOf<String,ToolResult>()
        val declarations=ToolRegistry.declarations(request)
        var count=0; var privateSeen=false
        repeat(5) {
            currentCoroutineContext().ensureActive()
            val reply=gateway.generate(contents,declarations)
            if(reply.calls.isEmpty()) {
                val answer=if(outcomes.isNotEmpty()) outcomes.joinToString("\n\n") { it.second.message }
                    else if(reply.text.isBlank()) "AI yanıt üretmedi."
                    else if(Regex("(?:kurdum|actim|gonderdim|kaydettim|sildim|aramayi baslattim)").containsMatchIn(LocalParser.normalize(reply.text)))
                        "Bu işlem için doğrulanmış araç sonucu yok. Görev tamamlanmış sayılmadı."
                    else reply.text + if(reply.finish=="MAX_TOKENS") "\n\nYanıt uzunluk sınırında kesildi." else ""
                return AgentReport(answer,outcomes)
            }
            // Model içeriği, function id ve thoughtSignature değişmeden geri gönderilir.
            contents.put(reply.content)
            val responses=JSONArray()
            for(call in reply.calls) {
                currentCoroutineContext().ensureActive()
                if(++count>8) return AgentReport("Sekiz araç adımı sınırına ulaştım.\n"+outcomes.joinToString("\n") { it.second.message },outcomes)
                val invalid=ToolRegistry.validate(call)
                val spec=ToolRegistry.tools[call.name]
                val fingerprint=call.name+canonical(call.args)
                val result=when {
                    invalid!=null -> ToolResult(ResultState.BLOCKED,invalid)
                    !ToolRegistry.intentAllows(call.name,request) -> ToolResult(ResultState.BLOCKED,"Bu işlem kullanıcının görevinde yetkilendirilmedi.")
                    call.name=="research" && privateSeen -> ToolResult(ResultState.BLOCKED,"Kişisel telefon verisinden sonra bu görevde web araştırması çalıştırmıyorum.")
                    spec?.writes==true && fingerprint in writes -> writes.getValue(fingerprint)
                    else -> execute(call).also { if(spec?.writes==true && it.state in setOf(ResultState.VERIFIED,ResultState.HANDOFF)) writes[fingerprint]=it }
                }
                if(result.privateData) privateSeen=true
                outcomes+=call to result; event(call,result)
                responses.put(GeminiClient.functionResponse(call,result,sharePrivate))
                if(result.state in setOf(ResultState.BLOCKED,ResultState.FAILED,ResultState.CANCELLED))
                    return AgentReport(outcomes.joinToString("\n\n") { it.second.message },outcomes)
            }
            contents.put(JSONObject().put("role","user").put("parts",responses))
        }
        return AgentReport("Görev tur sınırına ulaştım.\n"+outcomes.joinToString("\n") { it.second.message },outcomes)
    }
    private fun canonical(j:JSONObject):String = j.keys().asSequence().sorted().joinToString("|") { key -> key+":"+j.get(key).toString() }
}
