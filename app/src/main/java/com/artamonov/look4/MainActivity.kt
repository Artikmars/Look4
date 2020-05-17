package com.artamonov.look4

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.artamonov.look4.service.ForegroundService
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.nearby.Nearby
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

companion object {

    private const val LOG_TAG = "Look4"
    private lateinit var deviceId: String
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.supportActionBar?.hide()

        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        setAdView()

        look_text.setOnClickListener {
            stopService()
            startActivity(Intent(this, LookActivity::class.java))
        }

        offline_text.setOnClickListener {
            if (offline_text.text == resources.getString(R.string.main_online_mode)) {
                stopService()
                Nearby.getConnectionsClient(applicationContext).stopAllEndpoints()
                offline_text.text = resources.getString(R.string.main_offline_mode)
            } else {
                startService()
                look_text.isEnabled = true
                look_text.isClickable = true
                offline_text.text = resources.getString(R.string.main_online_mode)
                look_text.setTextColor(ContextCompat.getColor(this, R.color.green))
            }
        }

        contacts_text.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        settings_text.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setAdView() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()
        if (ForegroundService.isAppInForeground) {
            offline_text.text = resources.getString(R.string.main_online_mode)
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
