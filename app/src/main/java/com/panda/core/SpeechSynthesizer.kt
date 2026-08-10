package com.panda.core

/**
 * Contrato para la respuesta hablada de Panda.
 */
interface SpeechSynthesizer {

    fun speak(text: String, onDone: (() -> Unit)? = null)

    fun stop()

    fun release()
}
