package com.panda.wakeword

import android.content.Context
import android.util.Log
import com.panda.core.WakeWordEngine
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

/**
 * Detecta la frase "oye panda" escuchando en background con un modelo
 * pequeño de Vosk cargado desde assets. Todo el procesamiento ocurre
 * en el dispositivo, sin red.
 *
 * Requiere colocar un modelo de Vosk en:
 *   app/src/main/assets/model-wakeword/
 * (ver README del proyecto para el enlace de descarga del modelo español pequeño).
 */
class VoskWakeWordEngine(
    private val context: Context,
    private val wakePhrase: String = "oye panda"
) : WakeWordEngine {

    companion object {
        private const val TAG = "VoskWakeWordEngine"
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var onDetected: (() -> Unit)? = null

    override fun setOnWakeWordDetected(listener: () -> Unit) {
        onDetected = listener
    }

    override fun start() {
        StorageService.unpack(
            context,
            "model-wakeword",
            "model",
            { unpackedModel: Model ->
                model = unpackedModel
                startListeningInternal(unpackedModel)
            },
            { exception: Exception ->
                Log.e(TAG, "No se pudo cargar el modelo de wake word", exception)
            }
        )
    }

    private fun startListeningInternal(model: Model) {
        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f).also {
                it.startListening(object : RecognitionListener {
                    override fun onPartialResult(hypothesis: String?) {
                        checkForWakePhrase(hypothesis)
                    }

                    override fun onResult(hypothesis: String?) {
                        checkForWakePhrase(hypothesis)
                    }

                    override fun onFinalResult(hypothesis: String?) {
                        checkForWakePhrase(hypothesis)
                    }

                    override fun onError(exception: Exception?) {
                        Log.e(TAG, "Error de reconocimiento en wake word", exception)
                    }

                    override fun onTimeout() {
                        // Reinicia la escucha continua tras timeout de Vosk.
                        speechService?.startListening(this)
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando el reconocedor de wake word", e)
        }
    }

    private fun checkForWakePhrase(resultJson: String?) {
        if (resultJson.isNullOrBlank()) return
        val text = runCatching {
            JSONObject(resultJson).optString("text").ifBlank {
                JSONObject(resultJson).optString("partial")
            }
        }.getOrDefault("")

        if (text.contains(wakePhrase, ignoreCase = true)) {
            onDetected?.invoke()
        }
    }

    override fun stop() {
        speechService?.stop()
    }

    override fun release() {
        speechService?.shutdown()
        speechService = null
        model?.close()
        model = null
    }
}
