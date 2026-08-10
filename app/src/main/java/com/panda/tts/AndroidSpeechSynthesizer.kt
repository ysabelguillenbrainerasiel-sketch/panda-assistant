package com.panda.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.panda.core.SpeechSynthesizer
import java.util.Locale
import java.util.UUID

/**
 * Envuelve el TextToSpeech nativo de Android. Es 100% local: el propio
 * sistema operativo sintetiza la voz, sin llamadas de red.
 */
class AndroidSpeechSynthesizer(context: Context) : SpeechSynthesizer {

    companion object {
        private const val TAG = "AndroidTts"
    }

    private var ready = false
    private var pendingUtterance: Pair<String, (() -> Unit)?>? = null

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            setSpanishIfAvailable()
            pendingUtterance?.let { (text, callback) -> speak(text, callback) }
            pendingUtterance = null
        } else {
            Log.e(TAG, "No se pudo inicializar TextToSpeech (status=$status)")
        }
    }

    private fun setSpanishIfAvailable() {
        val result = tts.setLanguage(Locale("es", "ES"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Español no disponible, usando el idioma por defecto del dispositivo")
        }
    }

    override fun speak(text: String, onDone: (() -> Unit)?) {
        if (!ready) {
            pendingUtterance = text to onDone
            return
        }
        val utteranceId = UUID.randomUUID().toString()
        if (onDone != null) {
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { onDone() }
                @Deprecated("Deprecated in API")
                override fun onError(utteranceId: String?) { onDone() }
            })
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun stop() {
        tts.stop()
    }

    override fun release() {
        tts.stop()
        tts.shutdown()
    }
}
