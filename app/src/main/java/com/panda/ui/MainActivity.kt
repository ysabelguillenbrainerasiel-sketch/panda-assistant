package com.panda.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.panda.R
import com.panda.service.PandaListeningService

class MainActivity : AppCompatActivity() {

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListeningService()
        } else {
            showStatus("Panda necesita el micrófono para funcionar")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (hasMicPermission()) {
            startListeningService()
        } else {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun startListeningService() {
        val intent = Intent(this, PandaListeningService::class.java)
        ContextCompat.startForegroundService(this, intent)
        showStatus("Panda está escuchando. Di \"Oye Panda\"")
    }

    private fun showStatus(text: String) {
        findViewById<TextView>(R.id.statusText).text = text
    }
}
