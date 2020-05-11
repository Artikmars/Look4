package com.artamonov.look4

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import kotlinx.android.synthetic.main.activity_main.look_text
import kotlinx.android.synthetic.main.activity_main.offline_text
import kotlinx.android.synthetic.main.activity_main.settings_text

class MainActivity : AppCompatActivity() {

companion object {

    private const val LOG_TAG = "Look4"
    private lateinit var deviceId: String
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        look_text.setOnClickListener {
            startActivity(Intent(this, LookActivity::class.java))
        }

        if (ForegroundService.isAppInForeground) {
            offline_text.text = resources.getString(R.string.online_mode)
        }

        offline_text.setOnClickListener {
            if (offline_text.text == resources.getString(R.string.online_mode)) {
                stopService()
                Nearby.getConnectionsClient(applicationContext).stopAllEndpoints()
                offline_text.text = resources.getString(R.string.offline_mode)
            } else {
                startService()
                look_text.isEnabled = true
                look_text.isClickable = true
                offline_text.text = resources.getString(R.string.online_mode)
                look_text.setTextColor(ContextCompat.getColor(this, R.color.green))
            }
        }

        settings_text.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        serviceIntent.putExtra("inputExtra", "Is enabled ...")
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        stopService(serviceIntent)
    }
}
