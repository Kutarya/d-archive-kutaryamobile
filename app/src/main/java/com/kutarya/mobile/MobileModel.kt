package com.kutarya.mobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class RequestTicket(val request:UiRequest,val reply:CompletableDeferred<UiReply> = CompletableDeferred())
data class MobileState(
    val input:String="",val answer:String="",val busy:Boolean=false,val progress:String="",
    val settings:AppSettings=AppSettings(),val hasKey:Boolean=false,val models:List<String> = emptyList(),
    val history:List<TaskRun> = emptyList(),val notes:List<SavedItem> = emptyList(),val routines:List<SavedItem> = emptyList(),
    val reminders:List<SavedItem> = emptyList(),val checklists:List<SavedItem> = emptyList(),val lastRequest:String="",
    val attachment:Attachment?=null,val error:String="",val revision:Int=0,val ready:Boolean=false,
    val conversationId:String="",val messages:List<TaskRun> = emptyList(),
    val conversations:List<ConversationSummary> = emptyList(),val planned:List<String> = emptyList(),
    val currentSteps:List<Step> = emptyList(),val suggestions:List<CommandExample> = emptyList()
)
class MobileModel(application:Application):AndroidViewModel(application),Interaction {
    val store=LocalStore(application)
    private val mutable=MutableStateFlow(MobileState())
    val state=mutable.asStateFlow()
    val requests=Channel<RequestTicket>(Channel.BUFFERED)
    private var work:Job?=null
    private var client:GeminiClient?=null
    private var pending:RequestTicket?=null
    private var pendingCommand:String?=null
    private val settingsWrite=Mutex()
    private val conversationWrite=Mutex()
    private val executor=ToolExecutor(application,store,{ mutable.value.settings },this)
    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settings=store.settings()
                store.list("task",500).filter { it.value.optString("status")=="Çalışıyor" }.forEach {
                    store.put("task",JSONObject(it.value.toString()).put("status","Kesildi").put("answer","Uygulama kapanınca görev durdu; otomatik devam edilmedi."),it.id)
                }
                val saved=store.vault.get("active-conversation")
                val conversation=when {
                    saved=="legacy" -> ""
                    saved.matches(Regex("[a-fA-F0-9-]{36}")) -> saved
                    else -> UUID.randomUUID().toString()
                }
                if(saved.isBlank()) store.vault.set("active-conversation",conversation)
                mutable.update { it.copy(settings=settings,hasKey=store.vault.has("gemini-key"),conversationId=conversation) }
                refresh()
                mutable.update { current ->
                    val last=current.messages.lastOrNull()
                    current.copy(ready=true,answer=last?.answer.orEmpty(),lastRequest=last?.request.orEmpty())
                }
            } catch(_:Exception) { mutable.update { it.copy(error="Yerel şifreli veriler okunamadı. Uygulamayı kaldırmadan önce destek al.") } }
        }
    }
    fun input(value:String) { mutable.update { it.copy(input=value.take(8000)) } }
    fun error(message:String) { mutable.update { it.copy(error=message) } }
    fun clearError() { mutable.update { it.copy(error="") } }
    fun attachment(value:Attachment?) { mutable.update { it.copy(attachment=value) } }
    fun settings(value:AppSettings,key:String?=null) {
        try { require(Regex("[A-Za-z0-9._-]{1,120}").matches(value.model)) { "Model kimliği geçersiz." } }
        catch(e:IllegalArgumentException) { error(e.message ?: "Ayar geçersiz."); return }
        mutable.update { it.copy(settings=value) }
        viewModelScope.launch { settingsWrite.withLock { withContext(Dispatchers.IO) {
            try {
                require(Regex("[A-Za-z0-9._-]{1,120}").matches(value.model)) { "Model kimliği geçersiz." }
                if(key!=null && key.isNotBlank()) {
                    require(key.length<=4096 && !key.any { it.code<32 }) { "API anahtarı geçersiz." }
                    store.vault.set("gemini-key",key.trim())
                }
                store.settings(value)
                if(!value.notifications) store.clear("notification")
                mutable.update { it.copy(hasKey=store.vault.has("gemini-key")) }
            } catch(e:IllegalArgumentException) { error(e.message ?: "Ayar geçersiz.") }
            catch(_:Exception) { error("Ayarlar güvenli biçimde kaydedilemedi.") }
        } } }
    }
    fun removeKey() {
        viewModelScope.launch(Dispatchers.IO) {
            try { store.vault.set("gemini-key",""); mutable.update { it.copy(hasKey=false,models=emptyList()) } }
            catch(_:Exception) { error("Anahtar kaldırılamadı.") }
        }
    }
    fun loadModels() {
        if(mutable.value.busy) return
        if(!mutable.value.settings.cloudConsent) { error("AI bağlantısında metin aktarım tercihini aç."); return }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val key=store.vault.get("gemini-key")
                if(key.isBlank()) { error("Önce AI bağlantısına API anahtarı ekle."); return@launch }
                val rows=GeminiClient().models(key); mutable.update { it.copy(models=rows) }
                if(rows.isEmpty()) error("Bu hesap için kullanılabilir model bulunamadı.")
            } catch(e:Exception) { error(e.message?.takeIf { e is java.io.IOException || e is IllegalArgumentException } ?: "Model listesi alınamadı.") }
        }
    }
    fun runRoutine(id:Long) {
        val row=mutable.value.routines.firstOrNull { it.id==id }
        if(row==null) { error("Bu rutin bulunamadı.");return }
        submit(row.value.optString("title")+" çalıştır",routineId=id)
    }
    fun submit(value:String = mutable.value.input,localCall:ToolCall?=null,routineId:Long?=null) {
        val request=value.trim()
        if(LocalParser.normalize(request) in setOf("dur","iptal","vazgec")) { stop(); return }
        if(mutable.value.busy || request.isBlank()) return
        if(!mutable.value.ready) { error("Kayıtlar hazırlanıyor. Birazdan tekrar dene."); return }
        if(Privacy.sensitive(request)) { error("Anahtar ve parolaları sohbete yazma. API anahtarını AI bağlantısından ekle."); return }
        val attachment=if(localCall==null) mutable.value.attachment else null
        val contextNote=LocalParser.normalize(request).trimEnd('.', '?','!') in setOf("bunu not al","bunu kaydet","bu yaniti not al") && mutable.value.answer.isNotBlank()
        val effectiveCall=localCall ?: if(contextNote) ToolCall("create_note",JSONObject().put("text",mutable.value.answer.take(7500))) else null
        val command=LocalDialog.resolve(request,pendingCommand,mutable.value.answer)
        val conversation=mutable.value.conversationId
        val priorRuns=mutable.value.history
        val textFile=attachment!=null && attachment.mime in setOf("text/plain","text/markdown") &&
            (LocalParser.normalize(request).contains("ozetle") || LocalParser.normalize(request).contains("ozet cikar") ||
                LocalParser.normalize(request).contains("kelime") || LocalParser.normalize(request).startsWith("dosyada "))
        if(attachment!=null && !textFile && (!mutable.value.settings.cloudConsent || !mutable.value.hasKey)) { error("Bu dosya için AI bağlantısı gerekiyor. Metin dosyalarını yerelde özetleyebilir ve arayabilirsin."); return }
        mutable.update { it.copy(input="",answer="",lastRequest=request,busy=true,progress="İşleniyor…",
            attachment=null,error="",planned=emptyList(),currentSteps=emptyList(),suggestions=emptyList()) }
        work=viewModelScope.launch(Dispatchers.IO) {
            var row:Long?=null
            var run=TaskRun(request=request,conversationId=conversation,privateContext=attachment!=null || effectiveCall!=null)
            val outcomes=mutableListOf<Pair<ToolCall,ToolResult>>()
            try {
                row=store.put("task",run.json())
                refresh()
                suspend fun record(call:ToolCall,result:ToolResult) {
                    outcomes+=call to result
                    val spec=ToolRegistry.tools[call.name]
                    run=run.copy(steps=run.steps+Step(spec?.title ?: call.name,result.state,result.message),
                        privateContext=run.privateContext || result.privateData || spec?.privateRead==true || spec?.writes==true)
                    store.put("task",run.json(),row)
                    mutable.update { it.copy(currentSteps=run.steps) }
                    refresh()
                }
                val routineName=LocalParser.normalize(command).removeSuffix(" calistir").removeSuffix(" rutinini").trim()
                val routines=store.list("routine").filter {
                    val title=LocalParser.normalize(it.value.optString("title"))
                    if(routineId!=null) it.id==routineId else routineName==title || routineName==title.removeSuffix(" rutini")
                }
                val source=if(routines.size==1) routines.single().value.optString("commands") else command
                val plan=when {
                    effectiveCall!=null -> LocalPlan.Actions(listOf(effectiveCall))
                    attachment!=null -> LocalPlan.Cloud
                    routineId!=null && routines.isEmpty() -> LocalPlan.Clarify("Bu rutin artık kayıtlı değil.")
                    routines.size>1 -> LocalPlan.Clarify("Bu adla birden fazla rutin var. Rutinler'den seç.")
                    routines.size==1 -> RoutineValidation.plan(source)
                    else -> LocalParser.parse(source)
                }
                if(plan is LocalPlan.Actions) mutable.update { it.copy(planned=plan.calls.map { call ->
                    ToolRegistry.tools[call.name]?.title ?: call.name }) }
                pendingCommand=if(plan is LocalPlan.Clarify && routines.isEmpty()) command else null
                val localFileAnswer=if(textFile) OfflineUtilities.attachment(request,attachment!!) else null
                if(textFile && localFileAnswer==null) throw IllegalArgumentException("Dosya için özetle, kelimeleri say veya dosyada ... ara komutunu kullan.")
                val answer=if(localFileAnswer!=null) {
                    val result=ToolResult(ResultState.VERIFIED,localFileAnswer,privateData=true)
                    record(ToolCall("offline_utility"),result)
                    localFileAnswer
                } else when(plan) {
                    is LocalPlan.Clarify -> plan.message
                    is LocalPlan.Actions -> {
                        var privateSeen=false
                        for(call in plan.calls) {
                            ensureActive()
                            mutable.update { it.copy(progress=ToolRegistry.tools[call.name]?.title ?: "İşlem") }
                            val result=if(call.name=="research" && privateSeen) ToolResult(ResultState.BLOCKED,"Bu görevde kişisel veriden sonra web araması çalıştırılmadı.")
                                else executor.execute(call)
                            record(call,result); if(result.privateData) privateSeen=true
                            if(result.state in setOf(ResultState.FAILED,ResultState.BLOCKED,ResultState.CANCELLED)) break
                        }
                        outcomes.joinToString("\n\n") { it.second.message }
                    }
                    LocalPlan.Cloud -> {
                        val cfg=mutable.value.settings; val key=store.vault.get("gemini-key")
                        if(!cfg.cloudConsent || key.isBlank()) {
                            mutable.update { it.copy(suggestions=CommandLanguage.suggestions(request)) }
                            "Bu isteği çözemedim. Aşağıdan bir iş seçebilirsin. Genel sorular için AI bağlantısı gerekir."
                        }
                        else {
                            val api=GeminiClient(); client=api
                            val gateway=object:ModelGateway { override suspend fun generate(contents:JSONArray,declarations:JSONArray):ModelReply {
                                client=api; mutable.update { it.copy(progress="Kutarya Us yanıt hazırlıyor…") }; return api.generate(key,cfg.model,contents,declarations)
                            } }
                            val report=AgentRunner(gateway,{ call ->
                                mutable.update { it.copy(progress=ToolRegistry.tools[call.name]?.title ?: "Araç kontrolü") }
                                executor.execute(call)
                            },::record).run(command,attachment,cfg.sharePrivate,
                                ConversationMemory.context(priorRuns,conversation,cfg.sharePrivate))
                            run=run.copy(contextEligible=outcomes.isEmpty())
                            report.answer
                        }
                    }
                }
                ensureActive()
                val status=when {
                    outcomes.any { it.second.state==ResultState.CANCELLED } -> "İptal"
                    outcomes.any { it.second.state in setOf(ResultState.FAILED,ResultState.BLOCKED) } -> "Tamamlanamadı"
                    outcomes.any { it.second.state==ResultState.HANDOFF } -> "Uygulamaya aktarıldı"
                    outcomes.isNotEmpty() -> "Doğrulandı"
                    plan is LocalPlan.Clarify -> "Bilgi bekliyor"
                    !mutable.value.settings.cloudConsent || !mutable.value.hasKey -> "Desteklenmeyen istek"
                    else -> "AI yanıtı"
                }
                val contextSafe=outcomes.isNotEmpty() && outcomes.all { (call,result) ->
                    call.name in setOf("calculate","device_status","offline_utility") &&
                        result.state==ResultState.VERIFIED && !result.privateData
                } && attachment==null
                run=run.copy(status=status,answer=answer,contextEligible=run.contextEligible || contextSafe)
                store.put("task",run.json(),row)
                mutable.update { it.copy(answer=answer,revision=it.revision+1) }
            } catch(_:CancellationException) {
                run=run.copy(status="İptal",answer="Durdurdum.")
                if(row!=null) try { store.put("task",run.json(),row) } catch(_:Exception) { }
                mutable.update { it.copy(answer=run.answer) }
            } catch(e:Exception) {
                if(textFile) mutable.update { it.copy(attachment=attachment) }
                val cancelled=!currentCoroutineContext().isActive
                val message=if(cancelled) "Durdurdum."
                    else if(e is java.io.IOException || e is IllegalArgumentException) e.message.orEmpty().take(500) else "İşlem tamamlanamadı. İzinleri ve bağlantıyı kontrol et."
                run=run.copy(status=if(cancelled) "İptal" else "Tamamlanamadı",answer=message)
                if(row!=null) try { store.put("task",run.json(),row) } catch(_:Exception) { }
                mutable.update { it.copy(answer=message,revision=it.revision+1) }
            } finally {
                pending?.reply?.cancel(); pending=null; client=null
                try { store.prune("task",500); refresh() } catch(_:Exception) { }
                mutable.update { it.copy(busy=false,progress="") }
            }
        }
    }
    fun stop() { pendingCommand=null; client?.cancel(); pending?.reply?.cancel(); work?.cancel(); mutable.update { it.copy(progress=if(it.busy) "Durduruluyor…" else "") } }
    fun newConversation() { if(!mutable.value.busy && mutable.value.ready) switchConversation(UUID.randomUUID().toString()) }
    fun openConversation(id:String) {
        if(!mutable.value.busy && mutable.value.ready && mutable.value.conversations.any { it.id==id }) switchConversation(id)
    }
    private fun switchConversation(id:String) {
        pendingCommand=null
        mutable.update { state ->
            val messages=ConversationMemory.messages(state.history,id)
            val last=messages.lastOrNull()
            state.copy(conversationId=id,messages=messages,input="",answer=last?.answer.orEmpty(),
                lastRequest=last?.request.orEmpty(),attachment=null,planned=emptyList(),currentSteps=emptyList(),suggestions=emptyList())
        }
        viewModelScope.launch { conversationWrite.withLock { withContext(Dispatchers.IO) {
            try { store.vault.set("active-conversation",id.ifBlank { "legacy" }) }
            catch(_:Exception) { error("Konuşma seçimi kaydedilemedi.") }
        } } }
    }
    fun deleteConversation(id:String) {
        if(mutable.value.busy) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                store.list("task",500).filter { it.value.optString("conversationId")==id }.forEach { store.delete("task",it.id) }
                if(mutable.value.conversationId==id) newConversation()
                refresh()
            } catch(_:Exception) { error("Konuşma silinemedi.") }
        }
    }
    override suspend fun request(request:UiRequest):UiReply {
        currentCoroutineContext().ensureActive()
        val ticket=RequestTicket(request); pending=ticket
        requests.send(ticket)
        return try { ticket.reply.await() } finally { if(pending===ticket) pending=null }
    }
    override suspend fun research(query:String):ToolResult {
        val cfg=mutable.value.settings; val key=store.vault.get("gemini-key")
        if(key.isBlank() || !cfg.cloudConsent) {
            val reply=request(UiRequest.Launch(DeviceAction("browser",JSONObject().put("query",query)),null,null))
            return ToolResult(if(reply.allowed) ResultState.HANDOFF else ResultState.FAILED,
                if(reply.allowed) "Arama sayfasını tarayıcıda açtım. Sonuçları henüz okuyup doğrulamadım." else reply.value)
        }
        val api=GeminiClient(); client=api
        val result=api.generate(key,cfg.model,JSONArray().put(GeminiClient.userContent("Türkçe ve kaynaklı araştır: $query")),grounded=true)
        val sources=if(result.sources.isEmpty()) "\n\nSağlayıcı kaynak bağlantısı döndürmedi; güncel doğrulama tamamlanmadı."
            else "\n\n"+result.sources.joinToString("\n") { "${it.first}: ${it.second}" }
        return ToolResult(if(result.sources.isEmpty()) ResultState.FAILED else ResultState.VERIFIED,result.text+sources,
            JSONObject().put("answer",result.text).put("sources",JSONArray(result.sources.map { JSONObject().put("title",it.first).put("url",it.second) })))
    }
    fun saveRoutine(title:String,commands:String,id:Long?=null):Boolean {
        if(title.isBlank() || commands.isBlank()) { error("Rutin adı ve komutları gerekli."); return false }
        if(title.length>100 || commands.length>8000 || Privacy.sensitive(commands)) { error("Rutin içeriği geçersiz."); return false }
        val plan=RoutineValidation.plan(commands)
        if(plan !is LocalPlan.Actions) { error((plan as LocalPlan.Clarify).message); return false }
        viewModelScope.launch(Dispatchers.IO) { try { store.put("routine",JSONObject().put("title",title.trim()).put("commands",commands.trim()),id); refresh() } catch(_:Exception) { error("Rutin kaydedilemedi.") } }
        return true
    }
    fun delete(kind:String,id:Long) { viewModelScope.launch(Dispatchers.IO) { try { require(kind in setOf("note","routine")); store.delete(kind,id); refresh() } catch(_:Exception) { error("Kayıt silinemedi.") } } }
    fun clearHistory() {
        if(mutable.value.busy) return
        viewModelScope.launch(Dispatchers.IO) {
            try { store.clear("task"); newConversation(); refresh() } catch(_:Exception) { error("Geçmiş temizlenemedi.") }
        }
    }
    fun alias(name:String,person:String,phone:String) {
        val digits=phone.filter { it.isDigit() || it=='+' }
        if(name.isBlank() || !Regex("^\\+?[0-9]{3,18}$").matches(digits)) { error("Kişi adı veya numarası geçersiz."); return }
        viewModelScope.launch(Dispatchers.IO) { try { store.vault.set("contact:"+LocalParser.normalize(name),JSONObject().put("name",person).put("phone",digits).toString()); error("Kişi kısayolu kaydedildi.") } catch(_:Exception) { error("Kişi kısayolu kaydedilemedi.") } }
    }
    private fun refresh() {
        val history=store.list("task",500).map { TaskRun.from(it.value) }.sortedByDescending { it.created }
        mutable.update { it.copy(history=history,messages=ConversationMemory.messages(history,it.conversationId),
            conversations=ConversationMemory.summaries(history),notes=store.list("note"),routines=store.list("routine"),
            reminders=store.list("reminder").filter { row -> !row.value.optBoolean("delivered") }.sortedBy { row -> row.value.optLong("at") },
            checklists=store.list("checklist")) }
    }
    override fun onCleared() { stop(); requests.close(); super.onCleared() }
}
