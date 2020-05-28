package com.artamonov.look4

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.service.ForegroundService
import com.artamonov.look4.settings.SettingsActivity
import com.artamonov.look4.utils.UserGender.Companion.ALL
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE
import com.artamonov.look4.utils.setSafeOnClickListener
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionsClient
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {

companion object {
    private lateinit var deviceId: String
}
    private var connectionClient: ConnectionsClient? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.supportActionBar?.hide()

        connectionClient = Nearby.getConnectionsClient(this)

        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        setAdView()

        if (BuildConfig.DEBUG) { current_version_text.text = getString(R.string.main_version, BuildConfig.VERSION_NAME) }

        look_text.setOnClickListener {
            stopService()
            startActivity(Intent(this, LookActivity::class.java))
        }

        offline_text.setSafeOnClickListener {
            if (offline_text.text == getString(R.string.main_online_mode)) {
                stopService()
                showSnackbarError(R.string.main_advertising_has_stopped)
                connectionClient?.stopAllEndpoints()
                offline_text.text = getString(R.string.main_offline_mode)
                letter_0_1.clearAnimation()
            } else {
                startService()
                showSnackbarError(R.string.main_advertising_has_started)
                animateDot()
                look_text.isEnabled = true
                look_text.isClickable = true
                offline_text.text = getString(R.string.main_online_mode)
            }
        }

        contacts_text.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }

        settings_text.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        main_look_gender_text.setOnClickListener {
            changeLookGenderText()
        }

        setLookGenderText()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun animateDot() {
        Glide.with(this).load(R.drawable.ic_black_o).into(letter_0_1)

        val rotate = RotateAnimation(
            0F, 360F,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )

        rotate.duration = 3000
        rotate.repeatCount = Animation.INFINITE
        rotate.fillAfter = true
        rotate.setInterpolator(this, android.R.anim.linear_interpolator)
        letter_0_1.startAnimation(rotate)
    }

    private fun setLookGenderText() {
        when (PreferenceHelper.getUserProfile()?.lookGender) {
            MALE -> main_look_gender_text.text = getString(R.string.main_man)
            FEMALE -> main_look_gender_text.text = getString(R.string.main_woman)
            ALL -> main_look_gender_text.text = getString(R.string.main_all)
        }
    }

    private fun setProgressBar(status: Boolean) {
        when (status) {
            true -> main_progress_bar.visibility = View.VISIBLE
            false -> main_progress_bar.visibility = View.GONE
        }
    }

    private fun changeLookGenderText() {
        setProgressBar(true)

        when (main_look_gender_text.text) {
            getString(R.string.main_man) -> {
                main_look_gender_text.text = getString(R.string.main_woman)
                setProgressBar(!PreferenceHelper.updateLookGender(FEMALE))
            }
            getString(R.string.main_woman) -> {
                main_look_gender_text.text = getString(R.string.main_all)
                setProgressBar(!PreferenceHelper.updateLookGender(ALL))
            }
            getString(R.string.main_all) -> {
                main_look_gender_text.text = getString(R.string.main_man)
                setProgressBar(!PreferenceHelper.updateLookGender(MALE))
            }
        }
    }

    private fun setAdView() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onResume() {
        super.onResume()
        if (ForegroundService.isAppInForeground) {
            offline_text.text = resources.getString(R.string.main_online_mode)
            animateDot()
        } else {
            offline_text.text = resources.getString(R.string.main_offline_mode)
            letter_0_1.clearAnimation()
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        serviceIntent.putExtra("inputExtra", "Advertising ...")
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopService() {
        if (ForegroundService.isAppInForeground) {
            stopService(Intent(this, ForegroundService::class.java))
        }
    }
}
