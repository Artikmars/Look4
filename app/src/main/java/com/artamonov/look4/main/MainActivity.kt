package com.artamonov.look4.main

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.artamonov.look4.BuildConfig
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.contacts.ContactsActivity
import com.artamonov.look4.look.LookActivity
import com.artamonov.look4.main.models.FetchMainStatus
import com.artamonov.look4.main.models.MainAction
import com.artamonov.look4.main.models.MainEvent
import com.artamonov.look4.main.models.MainViewState
import com.artamonov.look4.service.ForegroundService
import com.artamonov.look4.settings.SettingsActivity
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactAdvertiserUnseenState
import com.artamonov.look4.utils.LogHandler
import com.artamonov.look4.utils.UserGender.Companion.ALL
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE
import com.artamonov.look4.utils.default
import com.artamonov.look4.utils.setSafeOnClickListener
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.nearby.Nearby
import java.io.File
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {

companion object {
    private lateinit var deviceId: String
    var isDestroyed = false
}
    private lateinit var viewModel: MainViewModel

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.supportActionBar?.hide()
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        setAdView()

        if (BuildConfig.DEBUG) { current_version_text.text = getString(
            R.string.main_version,
            BuildConfig.VERSION_NAME
        ) }

        current_version_text.setOnClickListener { viewModel.obtainEvent(MainEvent.SendEmail) }

        viewModel.viewStates().observe(this, Observer { bindViewState(it) })
        viewModel.viewEffects().observe(this, Observer { bindViewAction(it) })

        look_text.setOnClickListener { viewModel.obtainEvent(MainEvent.DiscoveringIsStarted) }
        offline_text.setSafeOnClickListener { viewModel.obtainEvent(MainEvent.ChangeStatus) }
        contacts_text.setOnClickListener { viewModel.obtainEvent(MainEvent.OpenContacts) }
        settings_text.setOnClickListener { viewModel.obtainEvent(MainEvent.OpenSettings) }
        main_look_gender_text.setOnClickListener { viewModel.obtainEvent(MainEvent.ChangeLookGender) }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun bindViewState(viewState: MainViewState) {
        when (viewState.fetchStatus) {
            is FetchMainStatus.DefaultState -> {
                setLookGenderText(viewState.data?.lookGender)
            }
            is FetchMainStatus.OnLookClickedState -> {
                if (serviceWasStopped()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivity(Intent(this, LookActivity::class.java),
                            ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                    } else {
                        startActivity(Intent(this, LookActivity::class.java))
                    }
                }
            }
            is FetchMainStatus.LoadingState -> {
            }
            is FetchMainStatus.OnContactsClickedState -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivity(Intent(this, ContactsActivity::class.java),
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                } else {
                    startActivity(Intent(this, ContactsActivity::class.java))
                }
            }
            is FetchMainStatus.OnSettingsClickedState -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivity(Intent(this, SettingsActivity::class.java),
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                } else {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
            is FetchMainStatus.OfflineState -> {
                if (serviceWasStopped()) {
                    showSnackbarError(R.string.main_advertising_has_stopped)
                    Nearby.getConnectionsClient(this).stopAllEndpoints()
                    offline_text.text = getString(R.string.main_offline_mode)
                    letter_0_1.clearAnimation()
                }
            }
            is FetchMainStatus.OnlineState -> {
                startService()
                showSnackbarError(R.string.main_advertising_has_started)
                animateDot()
                look_text.isEnabled = true
                look_text.isClickable = true
                offline_text.text = getString(R.string.main_online_mode)
            }
            is FetchMainStatus.LookGenderManState -> {
                main_look_gender_text.text = getString(R.string.main_man)
            }
            is FetchMainStatus.LookGenderWomenState -> {
                main_look_gender_text.text = getString(R.string.main_woman)
            }
            is FetchMainStatus.LookGenderAllState -> {
                main_look_gender_text.text = getString(R.string.main_all)
            }
        }
    }

    private fun bindViewAction(viewAction: MainAction) {
        when (viewAction) {
            is MainAction.SendEmail -> { sendEmail(LogHandler.saveLogsToFile(this)) }
        }
    }

    override fun onStart() {
        super.onStart()
        MainActivity.isDestroyed = false
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

    private fun setLookGenderText(lookGender: String?) {
        when (lookGender) {
            MALE -> main_look_gender_text.text = getString(R.string.main_man)
            FEMALE -> main_look_gender_text.text = getString(R.string.main_woman)
            ALL -> main_look_gender_text.text = getString(R.string.main_all)
        }
    }

    private fun setAdView() {
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onResume() {
        super.onResume()
        contactAdvertiserUnseenState.observe(this, Observer { state ->
            when (state) {
                ContactUnseenState.EnabledState -> {
                    showSnackbarWithAction()
                    contactAdvertiserUnseenState.default(ContactUnseenState.DisabledState)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        MainActivity.isDestroyed = true
    }

    private fun startService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        serviceIntent.putExtra("inputExtra", getString(R.string.main_advertising_title))
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun serviceWasStopped(): Boolean {
        return if (ForegroundService.isForegroundServiceRunning) {
            stopService(Intent(this, ForegroundService::class.java))
        } else {
            true
        }
    }

    private fun sendEmail(log: File) {
        val emailIntent = Intent(
            Intent.ACTION_SENDTO,
            Uri.fromParts("mailto", "artamonov06@gmail.com", null)
        )
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Look4 - logs")

        emailIntent.putExtra(
            Intent.EXTRA_TEXT,
            "Device model: " +
                    Build.MODEL +
                    " (" +
                    Build.PRODUCT +
                    ")\n" +
                    "Android version: " +
                    Build.VERSION.RELEASE +
                    "\nApp Version: " +
                    BuildConfig.VERSION_NAME
        )
        val path = Uri.fromFile(log)
        emailIntent.putExtra(Intent.EXTRA_STREAM, path)
        startActivity(Intent.createChooser(emailIntent, "Send email..."))
    }
}
