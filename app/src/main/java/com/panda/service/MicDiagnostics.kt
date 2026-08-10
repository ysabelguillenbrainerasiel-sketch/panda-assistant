package com.panda.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

/**
 * Utilidad de solo-debug: abre el micrófono brevemente ANTES de que Vosk
 * tome el AudioRecord, graba ~300ms y calcula el nivel RMS de la señal.
 *
 * Sirve para descartar de un vistazo dos causas comunes de "no pasa nada
 * al decir Oye Panda":
 *   1. Permiso RECORD_AUDIO no concedido de verdad.
 *   2. Micrófono devolviendo puro silencio (RMS ~0): hardware silenciado,
 *      la capa del fabricante bloqueando el mic en background, o
 *      AudioRecord fallando en el estado STATE_UNINITIALIZED.
 *
 * No sustituye nada de la arquitectura: es un chequeo previo, independiente,
 * que solo escribe en Logcat.
 */
object MicDiagnostics {

    // Filtra en Logcat con: adb logcat -s PandaDebug/Mic
    private const val TAG = "PandaDebug/Mic"
    private const val SAMPLE_RATE = 16000

    fun runQuickCheck(context: Context) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "Permiso RECORD_AUDIO concedido = $hasPermission")
        if (!hasPermission) {
            Log.e(TAG, "❌ Sin permiso de micrófono: Vosk jamás va a recibir audio. Revisa Ajustes > Apps > Panda > Permisos.")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) {
            Log.e(TAG, "❌ AudioRecord.getMinBufferSize devolvió $minBufferSize (config de audio no soportada en este dispositivo)")
            return
        }

        var recorder: AudioRecord? = null
        try {
            @Suppress("MissingPermission") // ya verificado arriba
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "❌ AudioRecord no se inicializó (state=${recorder.state}). El micrófono puede estar en uso por otra app.")
                return
            }

            recorder.startRecording()
            val buffer = ShortArray(minBufferSize)
            val read = recorder.read(buffer, 0, buffer.size)
            recorder.stop()

            if (read <= 0) {
                Log.e(TAG, "❌ AudioRecord.read() devolvió $read (sin datos). El micrófono no está entregando audio.")
                return
            }

            var sumSquares = 0.0
            for (i in 0 until read) {
                sumSquares += (buffer[i].toDouble() * buffer[i].toDouble())
            }
            val rms = sqrt(sumSquares / read)

            if (rms < 20.0) {
                Log.w(TAG, "⚠️ Nivel de audio muy bajo (RMS=$rms). Puede ser silencio real o mic bloqueado/tapado. Si sale así siempre incluso hablando fuerte, es un problema de hardware/permiso a nivel de sistema.")
            } else {
                Log.i(TAG, "✅ Micrófono entregando audio correctamente (RMS=$rms).")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción probando el micrófono", e)
        } finally {
            recorder?.release()
        }
    }
}
