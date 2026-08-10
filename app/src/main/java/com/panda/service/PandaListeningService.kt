package com.panda.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.panda.R
import com.panda.actions.ActionRegistry
import com.panda.core.PandaOrchestrator
import com.panda.stt.VoskSpeechRecognizerEngine
import com.panda.tts.AndroidSpeechSynthesizer
import com.panda.wakeword.VoskWakeWordEngine

/**
 * Mantiene el pipeline de Panda vivo mientras la app está en background,
 * escuchando permanentemente la wake word "Oye Panda".
 */
class PandaListeningService : Service() {

    companion object {
        private const val CHANNEL_ID = "panda_listening_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var orchestrator: PandaOrchestrator

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())

        val wakeWordEngine = VoskWakeWordEngine(applicationContext)
        val speechRecognizer = VoskSpeechRecognizerEngine(applicationContext)
        val speechSynthesizer = AndroidSpeechSynthesizer(applicationContext)
        val actionRegistry = ActionRegistry()

        orchestrator = PandaOrchestrator(
            wakeWordEngine = wakeWordEngine,
            speechRecognizer = speechRecognizer,
            speechSynthesizer = speechSynthesizer,
            actionRegistry = actionRegistry
        )
        orchestrator.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        orchestrator.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Panda escuchando",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Panda")
            .setContentText("Esperando \"Oye Panda\"...")
            .setSmallIcon(R.drawable.ic_panda_notification)
            .setOngoing(true)
            .build()
    }
}
