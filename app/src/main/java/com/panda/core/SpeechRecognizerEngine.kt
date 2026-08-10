package com.panda.core

/**
 * Contrato para el reconocimiento de voz a texto (STT), usado después de detectar
 * la wake word para capturar el comando que dice el usuario.
 */
interface SpeechRecognizerEngine {

    /** Inicia la captura y transcripción de una sola frase de comando. */
    fun startListening()

    /** Detiene la captura manualmente (ej. timeout desde el orquestador). */
    fun stopListening()

    fun release()

    /** Se dispara con el texto final reconocido. */
    fun setOnResult(listener: (String) -> Unit)

    /** Se dispara si no se entendió nada o hubo un error de audio. */
    fun setOnError(listener: (String) -> Unit)
}
