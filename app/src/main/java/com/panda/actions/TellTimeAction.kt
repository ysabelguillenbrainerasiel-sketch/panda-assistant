package com.panda.actions

import com.panda.core.PandaAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Primera acción "de prueba" del sistema: responde con la hora actual.
 * Sirve como plantilla para agregar acciones nuevas (abrir apps, controlar
 * hardware, etc.) más adelante.
 */
class TellTimeAction : PandaAction {

    override val name = "decir_hora"

    override fun matches(commandText: String): Boolean {
        val t = commandText.lowercase(Locale.getDefault())
        return t.contains("hora") || t.contains("qué hora es") || t.contains("que hora es")
    }

    override fun execute(commandText: String): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val horaActual = formatter.format(Date())
        return "Son las $horaActual"
    }
}
