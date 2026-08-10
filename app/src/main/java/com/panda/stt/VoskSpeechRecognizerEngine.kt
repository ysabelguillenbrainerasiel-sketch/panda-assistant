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
        // Filtra en Logcat con: adb logcat -s PandaDebug/STT
        private const val TAG = "PandaDebug/STT"
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
        Log.d(TAG, "startListening() llamado. Desempaquetando modelo ($modelAssetPath) ...")
        StorageService.unpack(
            context,
            modelAssetPath,
            "model",
            { unpackedModel: Model ->
                Log.i(TAG, "✅ Modelo de comandos cargado correctamente.")
                model = unpackedModel
                listenOnce(unpackedModel)
            },
            { exception: Exception ->
                Log.e(TAG, "❌ No se pudo cargar el modelo de comandos", exception)
                onError?.invoke("no_model")
            }
        )
    }

    private fun listenOnce(model: Model) {
        try {
            val recognizer = Recognizer(model, 16000.0f)
            Log.d(TAG, "Recognizer de comandos creado, arrancando SpeechService...")
            speechService = SpeechService(recognizer, 16000.0f).also { service ->
                val startedOk = try {
                    service.startListening(buildListener())
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "❌ startListening() lanzó excepción (posible fallo de AudioRecord/permiso)", e)
                    onError?.invoke("audio_start_failed")
                    false
                }
                Log.d(TAG, "SpeechService.startListening() invocado. OK=$startedOk")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando reconocedor de comandos", e)
            onError?.invoke(e.message ?: "init_error")
        }
    }

    private fun buildListener() = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {
            Log.d(TAG, "PARTIAL -> $hypothesis")
        }

        override fun onResult(hypothesis: String?) {
            Log.i(TAG, "RESULT -> $hypothesis")
            emitFinal(hypothesis)
        }

        override fun onFinalResult(hypothesis: String?) {
            Log.i(TAG, "FINAL -> $hypothesis")
            emitFinal(hypothesis)
            stopListening()
        }

        override fun onError(exception: Exception?) {
            Log.e(TAG, "Error reconociendo comando", exception)
            onError?.invoke(exception?.message ?: "unknown_error")
        }

        override fun onTimeout() {
            Log.w(TAG, "onTimeout() capturando comando -> se cierra la escucha")
            onError?.invoke("timeout")
            stopListening()
        }
    }

    private fun emitFinal(resultJson: String?) {
        if (resultJson.isNullOrBlank()) {
            Log.v(TAG, "emitFinal: JSON vacío/nulo")
            return
        }
        val json = runCatching { JSONObject(resultJson) }.getOrNull()
        if (json == null) {
            Log.w(TAG, "No se pudo parsear el JSON de comando: $resultJson")
            return
        }

        val wordsArray = json.optJSONArray("result")
        if (wordsArray != null) {
            for (i in 0 until wordsArray.length()) {
                val w = wordsArray.optJSONObject(i) ?: continue
                Log.d(
                    TAG,
                    "  palabra='${w.optString("word")}' conf=${w.optDouble("conf", -1.0)}"
                )
            }
        }

        val text = json.optString("text")
        Log.d(TAG, "Comando reconocido: \"$text\"")
        if (text.isNotBlank()) {
            onResult?.invoke(text)
        } else {
            Log.w(TAG, "Texto vacío: Vosk no entendió nada (silencio o ruido)")
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
