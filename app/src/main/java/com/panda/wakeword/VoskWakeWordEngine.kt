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
        // Filtra en Logcat con: adb logcat -s PandaDebug/WakeWord
        private const val TAG = "PandaDebug/WakeWord"
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var onDetected: (() -> Unit)? = null
    private var partialCount = 0

    override fun setOnWakeWordDetected(listener: () -> Unit) {
        onDetected = listener
    }

    override fun start() {
        Log.d(TAG, "start() llamado. Desempaquetando modelo desde assets/model-wakeword ...")
        StorageService.unpack(
            context,
            "model-wakeword",
            "model",
            { unpackedModel: Model ->
                Log.i(TAG, "✅ Modelo de wake word cargado correctamente (Model no nulo).")
                model = unpackedModel
                startListeningInternal(unpackedModel)
            },
            { exception: Exception ->
                Log.e(
                    TAG,
                    "❌ FALLÓ la carga del modelo. Revisa que exista " +
                        "app/src/main/assets/model-wakeword/ con archivos dentro " +
                        "(am/, conf/, graph/, etc.), no una subcarpeta anidada de más.",
                    exception
                )
            }
        )
    }

    private fun startListeningInternal(model: Model) {
        try {
            val recognizer = Recognizer(model, 16000.0f)
            Log.d(TAG, "Recognizer creado con sample rate 16000.0f")
            speechService = SpeechService(recognizer, 16000.0f).also { service ->
                // SpeechService internamente abre un AudioRecord. Si esto lanza
                // excepción, es casi siempre permiso RECORD_AUDIO no concedido
                // o el micrófono ya está tomado por otra app/proceso.
                val startedOk = try {
                    service.startListening(buildListener())
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "❌ startListening() lanzó excepción (posible fallo de AudioRecord/permiso)", e)
                    false
                }
                Log.d(TAG, "SpeechService.startListening() invocado. OK=$startedOk")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando el reconocedor de wake word", e)
        }
    }

    private fun buildListener() = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {
            partialCount++
            // Log de CADA resultado parcial, con el JSON crudo completo.
            Log.d(TAG, "PARTIAL #$partialCount -> $hypothesis")
            checkForWakePhrase(hypothesis, isPartial = true)
        }

        override fun onResult(hypothesis: String?) {
            Log.i(TAG, "RESULT -> $hypothesis")
            checkForWakePhrase(hypothesis, isPartial = false)
        }

        override fun onFinalResult(hypothesis: String?) {
            Log.i(TAG, "FINAL -> $hypothesis")
            checkForWakePhrase(hypothesis, isPartial = false)
        }

        override fun onError(exception: Exception?) {
            Log.e(TAG, "Error de reconocimiento en wake word", exception)
        }

        override fun onTimeout() {
            Log.w(TAG, "onTimeout() -> reiniciando escucha continua")
            speechService?.startListening(this)
        }
    }

    /**
     * Extrae el texto del JSON de Vosk y también loguea la confianza por
     * palabra si está presente (campo "result": [{"word":..,"conf":..}]).
     * El small model sin gramática no siempre trae "conf"; si no aparece,
     * se loguea igual el texto para que puedas ver qué está escuchando.
     */
    private fun checkForWakePhrase(resultJson: String?, isPartial: Boolean) {
        if (resultJson.isNullOrBlank()) {
            Log.v(TAG, "checkForWakePhrase: JSON vacío/nulo, se ignora")
            return
        }

        val json = runCatching { JSONObject(resultJson) }.getOrNull()
        if (json == null) {
            Log.w(TAG, "No se pudo parsear el JSON de Vosk: $resultJson")
            return
        }

        val text = if (isPartial) json.optString("partial") else json.optString("text")

        // Log de confianza por palabra, si el modelo la reporta.
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

        Log.d(TAG, "Texto reconocido (${if (isPartial) "partial" else "final"}): \"$text\"")

        if (text.contains(wakePhrase, ignoreCase = true)) {
            Log.i(TAG, "🐼 WAKE WORD DETECTADA: \"$wakePhrase\" encontrada en \"$text\"")
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
