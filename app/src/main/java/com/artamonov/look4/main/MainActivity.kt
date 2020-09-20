package com.artamonov.look4.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import com.artamonov.look4.BuildConfig
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.main.models.FetchMainStatus
import com.artamonov.look4.main.models.MainAction
import com.artamonov.look4.main.models.MainEvent
import com.artamonov.look4.main.models.MainViewState
import com.artamonov.look4.service.ForegroundService.Companion.ADVERTISING_FAILED_EVENT
import com.artamonov.look4.service.ForegroundService.Companion.ADVERTISING_SUCCEEDED_EVENT
import com.artamonov.look4.service.ForegroundService.Companion.SERVICE_IS_DESTROYED
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactAdvertiserUnseenState
import com.artamonov.look4.utils.LogHandler
import com.artamonov.look4.utils.UserGender.Companion.ALL
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE
import com.artamonov.look4.utils.animateDot
import com.artamonov.look4.utils.default
import com.artamonov.look4.utils.registerBroadcastReceiver
import com.artamonov.look4.utils.setAdView
import com.artamonov.look4.utils.setSafeOnClickListener
import com.artamonov.look4.utils.showSnackbarError
import com.artamonov.look4.utils.showSnackbarWithAction
import com.artamonov.look4.utils.startContactsActivity
import com.artamonov.look4.utils.startLookActivity
import com.artamonov.look4.utils.startService
import com.artamonov.look4.utils.startSettingsActivity
import com.artamonov.look4.utils.stopService
import com.artamonov.look4.utils.unblockInput
import com.artamonov.look4.utils.unregisterBroadcastReceiver
import com.bumptech.glide.Glide
import java.io.File
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity(R.layout.activity_main) {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.supportActionBar?.hide()

        registerBroadcastReceiver(mMessageReceiver)
        handleIntentIfExist(intent)
        setAdView()

        if (BuildConfig.DEBUG) { current_version_text.text = getString(
            R.string.main_version,
            BuildConfig.VERSION_NAME
        ) }

        current_version_text.setOnClickListener { viewModel.obtainEvent(MainEvent.SendEmail) }

        viewModel.viewStates().observe(this, { bindViewState(it) })
        viewModel.viewEffects().observe(this, { bindViewAction(it) })

        Glide.with(this).load(R.drawable.ic_black_o).into(letter_0_1)

        look_text.setOnClickListener { viewModel.obtainEvent(MainEvent.DiscoveringIsStarted) }
        offline_text.setSafeOnClickListener { viewModel.obtainEvent(MainEvent.ChangeStatus) }
        contacts_text.setOnClickListener { viewModel.obtainEvent(MainEvent.OpenContacts) }
        settings_text.setOnClickListener { viewModel.obtainEvent(MainEvent.OpenSettings) }
        main_look_gender_text.setOnClickListener { viewModel.obtainEvent(MainEvent.ChangeLookGender) }
    }

    private fun handleIntentIfExist(intent: Intent?) = intent?.extras?.let { startLookActivity() }

    private fun bindViewState(viewState: MainViewState) {
        when (viewState.fetchStatus) {
            is FetchMainStatus.DefaultState -> { setLookGenderText(viewState.data?.lookGender) }
            is FetchMainStatus.OnLookClickedState -> {
                stopService()
                startLookActivity()
            }
            is FetchMainStatus.LoadingState -> { }
            is FetchMainStatus.OnContactsClickedState -> { startContactsActivity() }
            is FetchMainStatus.OnSettingsClickedState -> { startSettingsActivity() }
            is FetchMainStatus.EnablingOfflineState -> { stopService() }
            is FetchMainStatus.EnablingOnlineState -> { startService() }
            is FetchMainStatus.OnlineEnabledState -> {
                offline_text.text = getString(R.string.main_online_mode)
                letter_0_1.animateDot(this)
                unblockInput()
                showSnackbarError(R.string.main_advertising_has_started)
            }
            is FetchMainStatus.OfflineEnabledState -> {
                connectionClient.stopAllEndpoints()
                offline_text.text = getString(R.string.main_offline_mode)
                letter_0_1.clearAnimation()
                unblockInput()
                showSnackbarError(R.string.main_advertising_has_stopped)
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

    private fun setLookGenderText(lookGender: String?) {
        when (lookGender) {
            MALE -> main_look_gender_text.text = getString(R.string.main_man)
            FEMALE -> main_look_gender_text.text = getString(R.string.main_woman)
            ALL -> main_look_gender_text.text = getString(R.string.main_all)
        }
    }

    override fun onResume() {
        super.onResume()
        contactAdvertiserUnseenState.observe(this, { state ->
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
        unregisterBroadcastReceiver(mMessageReceiver)
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

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            // Get extra data included in the Intent
            when (intent.action) {
                ADVERTISING_SUCCEEDED_EVENT -> { viewModel.obtainEvent(MainEvent.OnlineIsEnabled) }
                ADVERTISING_FAILED_EVENT, SERVICE_IS_DESTROYED -> {
                    viewModel.obtainEvent(MainEvent.OfflineIsEnabled)
                }
            }
        }
    }
}
