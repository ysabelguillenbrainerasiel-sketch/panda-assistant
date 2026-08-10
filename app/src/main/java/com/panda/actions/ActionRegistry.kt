package com.panda.actions

import com.panda.core.PandaAction

/**
 * Punto único donde se registran todas las acciones disponibles.
 * Para agregar una acción nueva en el futuro (abrir cámara, jugar,
 * automatizar algo) solo hay que:
 *   1. Crear una clase que implemente PandaAction.
 *   2. Agregarla a la lista de abajo.
 * El resto del sistema (orquestador, wake word, STT, TTS) no cambia.
 */
class ActionRegistry {

    private val actions: List<PandaAction> = listOf(
        TellTimeAction()
        // Próximas acciones se agregan aquí, por ejemplo:
        // OpenAppAction(), FlashlightAction(), etc.
    )

    private val fallbackResponse =
        "No entendí ese comando todavía, pero lo puedo aprender más adelante."

    fun resolve(commandText: String): String {
        val action = actions.firstOrNull { it.matches(commandText) }
        return action?.execute(commandText) ?: fallbackResponse
    }
}
