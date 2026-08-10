package com.panda.core

import com.panda.actions.ActionRegistry

/**
 * Coordina el ciclo completo:
 *
 *   1. Escucha en background esperando "Oye Panda" (WakeWordEngine)
 *   2. Al detectarla, pausa el wake word y activa el STT (SpeechRecognizerEngine)
 *   3. Con el texto reconocido, busca una acción que sepa responder (ActionRegistry)
 *   4. Dice la respuesta en voz alta (SpeechSynthesizer)
 *   5. Vuelve a escuchar la wake word
 *
 * Esta clase no sabe nada de Vosk, Android TTS, etc. Solo conoce las
 * interfaces, así que cualquiera de esos motores se puede reemplazar
 * sin tocar esta lógica.
 */
class PandaOrchestrator(
    private val wakeWordEngine: WakeWordEngine,
    private val speechRecognizer: SpeechRecognizerEngine,
    private val speechSynthesizer: SpeechSynthesizer,
    private val actionRegistry: ActionRegistry
) {

    fun start() {
        wakeWordEngine.setOnWakeWordDetected { onWakeWordDetected() }
        speechRecognizer.setOnResult { commandText -> onCommandRecognized(commandText) }
        speechRecognizer.setOnError { onCommandFailed() }

        wakeWordEngine.start()
    }

    private fun onWakeWordDetected() {
        wakeWordEngine.stop()
        speechSynthesizer.speak("Dime") {
            speechRecognizer.startListening()
        }
    }

    private fun onCommandRecognized(commandText: String) {
        val response = actionRegistry.resolve(commandText)
        speechSynthesizer.speak(response) {
            wakeWordEngine.start()
        }
    }

    private fun onCommandFailed() {
        speechSynthesizer.speak("No te escuché bien, intenta de nuevo") {
            wakeWordEngine.start()
        }
    }

    fun stop() {
        wakeWordEngine.stop()
        speechRecognizer.stopListening()
        speechSynthesizer.stop()
    }

    fun release() {
        wakeWordEngine.release()
        speechRecognizer.release()
        speechSynthesizer.release()
    }
}
