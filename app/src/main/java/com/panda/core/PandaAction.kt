package com.panda.core

/**
 * Contrato que debe cumplir cualquier "acción" que Panda pueda ejecutar
 * (hoy: decir la hora; mañana: abrir apps, controlar hardware, etc.).
 *
 * Cada acción decide, a partir del texto reconocido, si le corresponde
 * responder (matches) y qué hacer si le toca (execute).
 */
interface PandaAction {

    /** Nombre corto para logs/depuración. */
    val name: String

    /** Devuelve true si esta acción sabe responder a ese comando. */
    fun matches(commandText: String): Boolean

    /**
     * Ejecuta la acción y devuelve el texto que Panda debe decir en voz alta
     * como respuesta.
     */
    fun execute(commandText: String): String
}
