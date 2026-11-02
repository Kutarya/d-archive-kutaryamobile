package com.kutarya.mobile

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    private val model:MobileModel by viewModels()
    private lateinit var speech:SpeechEngine
    private var ticket by mutableStateOf<RequestTicket?>(null)
    private var listening by mutableStateOf(false)
    private var voiceStatus by mutableStateOf("")
    private var voiceLevel by mutableFloatStateOf(0f)
    private var microphonePermission=false
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(microphonePermission) {
            microphonePermission=false
            if(granted && !model.state.value.busy) speech.start(model.state.value.settings.onlineSpeech)
            else if(!granted) model.error("Sesli kullanım için mikrofon izni gerekli. Yazıyla kullanabilirsin.")
        } else {
            val current=ticket
            if(current!=null && current.reply.isActive) {
                if(granted) continueTicket(current) else reply(UiReply(false,"İzin verilmedi."))
            } else ticket=null
        }
    }
    private val role=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private val document=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) readDocument(uri) }
    private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if(uri!=null) lifecycleScope.launch(Dispatchers.IO) {
            try {
                val data=JSONObject().put("app","Kutarya Mobile").put("version",2)
                    .put("notes",JSONArray(model.store.list("note").map { it.value }))
                    .put("routines",JSONArray(model.store.list("routine").map { it.value }))
                    .put("lists",JSONArray(model.store.list("checklist").map { it.value }))
                    .put("reminders",JSONArray(model.store.list("reminder").map { it.value }))
                val stream=contentResolver.openOutputStream(uri) ?: error("Dosya açılamadı.")
                stream.use { it.write(data.toString(2).toByteArray(Charsets.UTF_8)) }
                model.error("Yedek kaydedildi.")
            } catch(_:Exception) { model.error("Yedek dosyası yazılamadı.") }
        }
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        speech=SpeechEngine(this,{ value ->
            model.input(value)
            if(model.state.value.settings.voiceSubmit) model.submit(value)
        },{ active,message -> listening=active; voiceStatus=message },model::input,{ voiceLevel=it })
        handleIntent(intent)
        lifecycleScope.launch {
            for(next in model.requests) lifecycle.withResumed {
                if(next.reply.isActive) { ticket=next; continueTicket(next) }
            }
        }
        setContent {
            val state by model.state.collectAsState()
            val dark=when(state.settings.theme) { "dark"->true; "light"->false; else->isSystemInDarkTheme() }
            SideEffect {
                WindowCompat.getInsetsController(window,window.decorView).apply {
                    isAppearanceLightStatusBars=!dark; isAppearanceLightNavigationBars=!dark
                }
            }
            LaunchedEffect(state.busy) { if(state.busy) { speech.stop(); speech.silence() } }
            LaunchedEffect(state.revision) {
                if(state.revision>0 && state.settings.speak && state.answer.isNotBlank()) speech.speak(state.answer,state.settings.onlineSpeech)
            }
            LaunchedEffect(state.busy,ticket) { if(!state.busy && ticket?.reply?.isActive==false) ticket=null }
            KutaryaUi(model,state,listening,voiceStatus,::voice,{
                speech.stop(); speech.silence(); model.stop()
            },{ document.launch(arrayOf("application/pdf","image/jpeg","image/png","image/webp","text/plain","text/markdown")) },
                { export.launch("Kutarya-Mobile-Yedek.json") },::assistantSettings,
                { role.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },ticket,::reply,voiceLevel=voiceLevel)
        }
    }
    override fun onNewIntent(intent:Intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(intent) }
    private fun handleIntent(value:Intent) {
        // Dış paylaşımdaki içerik yalnız yazı alanına gelir, kendiliğinden çalışmaz.
        if(value.action==Intent.ACTION_SEND && value.type=="text/plain") model.input(value.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
    }
    private fun continueTicket(current:RequestTicket) {
        if(!current.reply.isActive) { ticket=null; return }
        val request=current.request as? UiRequest.Launch ?: return
        val needed=request.permission
        if(needed!=null && ContextCompat.checkSelfPermission(this,needed)!=PackageManager.PERMISSION_GRANTED) {
            permission.launch(needed); return
        }
        if(request.approval==null) reply(PlatformActions.launch(this,request.action))
    }
    private fun reply(value:UiReply) {
        val current=ticket ?: return
        if(current.reply.isActive) {
            val final=if(value.allowed && value.value=="approve" && current.request is UiRequest.Launch)
                PlatformActions.launch(this,(current.request as UiRequest.Launch).action) else value
            current.reply.complete(final)
        }
        ticket=null
    }
    private fun voice() {
        if(model.state.value.busy) return
        if(listening) { speech.finish(); return }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) speech.start(model.state.value.settings.onlineSpeech)
        else { microphonePermission=true; permission.launch(Manifest.permission.RECORD_AUDIO) }
    }
    private fun assistantSettings() {
        try {
            if(Build.VERSION.SDK_INT>=29) {
                val manager=getSystemService(RoleManager::class.java)
                if(manager.isRoleAvailable(RoleManager.ROLE_ASSISTANT) && !manager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                    role.launch(manager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)); return
                }
            }
            role.launch(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
        } catch(_:Exception) { model.error("Bu cihazda asistan seçimi ekranı açılamadı.") }
    }
    private fun readDocument(uri:Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val name=contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {
                    if(it.moveToFirst()) it.getString(0) else "Dosya"
                } ?: "Dosya"
                var mime=contentResolver.getType(uri).orEmpty()
                if(mime=="application/octet-stream" && name.endsWith(".md",true)) mime="text/plain"
                require(mime in setOf("application/pdf","image/jpeg","image/png","image/webp","text/plain","text/markdown")) { "PDF, metin, JPEG, PNG veya WebP seç." }
                val bytes=contentResolver.openInputStream(uri)?.use { input ->
                    val output=ByteArrayOutputStream(); val buffer=ByteArray(8192)
                    while(true) { val count=input.read(buffer); if(count<0) break; require(output.size()+count<=6*1024*1024) { "Dosya en fazla 6 MB olabilir." }; output.write(buffer,0,count) }
                    output.toByteArray()
                } ?: error("Dosya okunamadı.")
                require(bytes.isNotEmpty()) { "Dosya boş." }
                val valid=when(mime) {
                    "application/pdf" -> bytes.take(5).toByteArray().toString(Charsets.US_ASCII)=="%PDF-"
                    "image/png" -> bytes.take(8)==listOf(137,80,78,71,13,10,26,10).map { it.toByte() }
                    "image/jpeg" -> bytes.size>2 && bytes[0]==0xff.toByte() && bytes[1]==0xd8.toByte()
                    "image/webp" -> bytes.size>12 && bytes.copyOfRange(0,4).toString(Charsets.US_ASCII)=="RIFF" && bytes.copyOfRange(8,12).toString(Charsets.US_ASCII)=="WEBP"
                    else -> true
                }
                require(valid) { "Dosyanın içeriği seçilen türle uyuşmuyor." }
                model.attachment(Attachment(name.take(180),if(mime=="text/markdown") "text/plain" else mime,bytes))
                if(model.state.value.input.isBlank()) model.input("Bu dosyayı Türkçe özetle.")
            } catch(e:IllegalArgumentException) { model.error(e.message ?: "Dosya geçersiz.") }
            catch(_:Exception) { model.error("Dosya okunamadı.") }
        }
    }
    override fun onStop() { speech.stop(); speech.silence(); super.onStop() }
    override fun onDestroy() { speech.close(); super.onDestroy() }
}
