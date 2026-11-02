package com.kutarya.mobile

import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.*
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechEngine(private val context: Context, private val text: (String)->Unit,
    private val status: (Boolean,String)->Unit,private val partial:(String)->Unit={},
    private val level:(Float)->Unit={}) {
    private var recognizer: SpeechRecognizer?=null
    private var token=0
    private var tts: TextToSpeech?=null
    private var ttsReady=false
    private var ttsNetwork=true
    init {
        tts=TextToSpeech(context) { result ->
            if(result==TextToSpeech.SUCCESS) {
                val engine=tts
                val voices=engine?.voices?.filter { it.locale.language=="tr" } ?: emptyList()
                val local=voices.firstOrNull { !it.isNetworkConnectionRequired }
                val voice=local ?: voices.firstOrNull()
                if(voice!=null) { engine?.voice=voice; ttsReady=true; ttsNetwork=voice.isNetworkConnectionRequired }
                else {
                    val supported=engine?.setLanguage(Locale.forLanguageTag("tr-TR"))?.let { it>=0 }==true
                    val selected=engine?.voice
                    ttsReady=supported && selected?.locale?.language=="tr"
                    ttsNetwork=selected?.isNetworkConnectionRequired ?: true
                }
            }
        }
    }
    fun start(onlineAllowed: Boolean) {
        stop(); silence()
        val local=onDevice(context)
        if(!local && !onlineAllowed) { status(false,"Bu cihazda yerel ses tanıma yok. Ayarlar'dan çevrimiçi sesi seçebilir veya yazabilirsin."); return }
        if(!local && !SpeechRecognizer.isRecognitionAvailable(context)) { status(false,"Telefonda ses tanıma hizmeti bulunamadı."); return }
        val current=++token
        val r=try {
            if(local && Build.VERSION.SDK_INT>=31) SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else SpeechRecognizer.createSpeechRecognizer(context)
        } catch(_:Exception) { status(false,"Ses hizmeti açılamadı. Yazıyla devam edebilirsin."); return }
        recognizer=r
        r.setRecognitionListener(object:RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) { if(current==token) status(true,if(local) "Cihazda dinliyorum…" else "Ses hizmeti dinliyor…") }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB:Float) { if(current==token) level(VoiceLevels.fromRms(rmsdB)) }
            override fun onBufferReceived(buffer:ByteArray?)=Unit
            override fun onEndOfSpeech() { if(current==token) status(true,"Konuşmanı çözüyorum…") }
            override fun onError(error:Int) { if(current==token) { status(false,when(error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni gerekli."
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Türkçe ses paketi cihazda kullanılabilir değil."
                SpeechRecognizer.ERROR_NETWORK,SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Ses hizmetinin internet bağlantısı kurulamadı."
                SpeechRecognizer.ERROR_NO_MATCH,SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Konuşmayı duyamadım. Tekrar deneyebilirsin."
                else -> "Ses tanıma tamamlanamadı ($error)."
            }); release() } }
            override fun onResults(results:android.os.Bundle?) {
                if(current!=token) return
                val result=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                status(false,if(result.isBlank()) "Konuşmayı duyamadım." else "Metni düzenleyip gönderebilirsin.")
                release(); if(result.isNotBlank()) text(result.take(8000))
            }
            override fun onPartialResults(partialResults:android.os.Bundle?) {
                if(current==token) partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.takeIf(String::isNotBlank)?.let { partial(it.take(8000)) }
            }
            override fun onEvent(eventType:Int,params:android.os.Bundle?)=Unit
        })
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR").putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,local)
        status(true,"Mikrofon açılıyor…")
        try { r.startListening(intent) } catch(_:Exception) { status(false,"Ses tanıma başlatılamadı."); release() }
    }
    fun finish() { try { recognizer?.stopListening() } catch(_:Exception) { stop(); status(false,"Ses tanıma durdu. Metni yazabilirsin.") } }
    fun stop() { token++; try { recognizer?.cancel() } catch(_:Exception) { }; release(); status(false,"") }
    private fun release() { try { recognizer?.destroy() } catch(_:Exception) { }; recognizer=null; level(0f) }
    fun speak(value:String,onlineAllowed:Boolean) {
        if(ttsReady && (!ttsNetwork || onlineAllowed)) tts?.speak(value.take(1800),TextToSpeech.QUEUE_FLUSH,null,"kutarya-answer")
    }
    fun silence() { tts?.stop() }
    fun close() { stop(); tts?.stop(); tts?.shutdown(); tts=null }
    companion object {
        fun onDevice(context:Context):Boolean = try {
            Build.VERSION.SDK_INT>=31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } catch(_:Exception) { false }
    }
}
