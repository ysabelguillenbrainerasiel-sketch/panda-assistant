package com.panda.core

/**
 * Contrato para cualquier motor de detección de palabra de activación ("Oye Panda").
 * Cualquier implementación (Vosk, Porcupine, un modelo propio, etc.) debe cumplir esto,
 * de forma que se pueda cambiar el motor sin tocar el resto del sistema.
 */
interface WakeWordEngine {

    /** Empieza a escuchar en background esperando la palabra de activación. */
    fun start()

    /** Detiene la escucha de wake word (por ejemplo, mientras se procesa un comando). */
    fun stop()

    /** Libera recursos nativos (modelo, micrófono, etc). Llamar al destruir el servicio. */
    fun release()

    /** Registra el callback que se dispara cuando se detecta la palabra de activación. */
    fun setOnWakeWordDetected(listener: () -> Unit)
}
