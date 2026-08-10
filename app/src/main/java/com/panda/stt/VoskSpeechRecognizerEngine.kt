package com.panda.stt

import android.content.Context
import android.util.Log
import com.panda.core.SpeechRecognizerEngine
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

/**
 * Captura una frase de comando completa después de que se detectó "oye panda"
 * y la devuelve como texto. Usa un modelo de Vosk (puede ser el mismo modelo
 * pequeño del wake word, o uno más completo si se necesita mejor precisión).
 */
class VoskSpeechRecognizerEngine(
    private val context: Context,
    private val modelAssetPath: String = "model-wakeword"
) : SpeechRecognizerEngine {

    companion object {
        private const val TAG = "VoskSpeechRecognizer"
    }

    private var speechService: SpeechService? = null
    private var model: Model? = null
    private var onResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    override fun setOnResult(listener: (String) -> Unit) {
        onResult = listener
    }

    override fun setOnError(listener: (String) -> Unit) {
        onError = listener
    }

    override fun startListening() {
        StorageService.unpack(
            context,
            modelAssetPath,
            "model",
            { unpackedModel: Model ->
                model = unpackedModel
                listenOnce(unpackedModel)
            },
            { exception: Exception ->
                Log.e(TAG, "No se pudo cargar el modelo de comandos", exception)
                onError?.invoke("no_model")
            }
        )
    }

    private fun listenOnce(model: Model) {
        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f).also {
                it.startListening(object : RecognitionListener {
                    override fun onPartialResult(hypothesis: String?) { /* no-op */ }

                    override fun onResult(hypothesis: String?) {
                        emitFinal(hypothesis)
                    }

                    override fun onFinalResult(hypothesis: String?) {
                        emitFinal(hypothesis)
                        stopListening()
                    }

                    override fun onError(exception: Exception?) {
                        Log.e(TAG, "Error reconociendo comando", exception)
                        onError?.invoke(exception?.message ?: "unknown_error")
                    }

                    override fun onTimeout() {
                        onError?.invoke("timeout")
                        stopListening()
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando reconocedor de comandos", e)
            onError?.invoke(e.message ?: "init_error")
        }
    }

    private fun emitFinal(resultJson: String?) {
        if (resultJson.isNullOrBlank()) return
        val text = runCatching { JSONObject(resultJson).optString("text") }.getOrDefault("")
        if (text.isNotBlank()) {
            onResult?.invoke(text)
        }
    }

    override fun stopListening() {
        speechService?.stop()
    }

    override fun release() {
        speechService?.shutdown()
        speechService = null
        model?.close()
        model = null
    }
}
