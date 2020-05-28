package com.artamonov.look4.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.artamonov.look4.BuildConfig
import com.artamonov.look4.ContactsActivity
import com.artamonov.look4.LookActivity
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.database.User
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
    private var mainViewModel = MainViewModel()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.supportActionBar?.hide()

        connectionClient = Nearby.getConnectionsClient(this)
        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        setAdView()

        if (BuildConfig.DEBUG) { current_version_text.text = getString(
            R.string.main_version,
            BuildConfig.VERSION_NAME
        ) }

        mainViewModel.isInForeground.observe(this, Observer { isInForeground ->
            if (isInForeground) {
                offline_text.text = resources.getString(R.string.main_online_mode)
                animateDot()
            } else {
                offline_text.text = resources.getString(R.string.main_offline_mode)
                letter_0_1.clearAnimation()
            }
        })

        mainViewModel.state.observe(this, Observer { state ->
            when (state) {
                is MainState.SucceededState<*> -> {
                    setLookGenderText(state.user as User?)
                }
                is MainState.OnLookClickedState -> {
                    stopService()
                    startActivity(Intent(this, LookActivity::class.java))
                }
                is MainState.LoadingState -> {
                    main_progress_bar.visibility = VISIBLE
                }
                is MainState.OnContactsClickedState -> {
                    startActivity(Intent(this, ContactsActivity::class.java))
                }
                is MainState.OnSettingsClickedState -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                is MainState.DefaultState -> {
                    stopService()
                    showSnackbarError(R.string.main_advertising_has_stopped)
                    connectionClient?.stopAllEndpoints()
                    offline_text.text = getString(R.string.main_offline_mode)
                    letter_0_1.clearAnimation()
                }
                is MainState.OnlineState -> {
                    startService()
                    showSnackbarError(R.string.main_advertising_has_started)
                    animateDot()
                    look_text.isEnabled = true
                    look_text.isClickable = true
                    offline_text.text = getString(R.string.main_online_mode)
                }
                is MainState.LookGenderManState -> {
                    main_progress_bar.visibility = GONE
                    main_look_gender_text.text = getString(R.string.main_man)
                }
                is MainState.LookGenderWomenState -> {
                    main_progress_bar.visibility = GONE
                    main_look_gender_text.text = getString(R.string.main_woman)
                }
                is MainState.LookGenderAllState -> {
                    main_progress_bar.visibility = GONE
                    main_look_gender_text.text = getString(R.string.main_all)
                }
            }
        })

        mainViewModel.populateData()

        look_text.setOnClickListener { mainViewModel.startDiscovering() }
        offline_text.setSafeOnClickListener { mainViewModel.changeAdvertisingStatus() }
        contacts_text.setOnClickListener { mainViewModel.openContacts() }
        settings_text.setOnClickListener { mainViewModel.openSettings() }
        main_look_gender_text.setOnClickListener { mainViewModel.changeLookGenderText() }
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

    private fun setLookGenderText(user: User?) {
        when (user?.lookGender) {
            MALE -> main_look_gender_text.text = getString(R.string.main_man)
            FEMALE -> main_look_gender_text.text = getString(R.string.main_woman)
            ALL -> main_look_gender_text.text = getString(R.string.main_all)
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
        mainViewModel.isInForeground()
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
