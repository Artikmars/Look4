package com.artamonov.look4.main

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.look.LookActivity.Companion.REQUEST_CODE_REQUIRED_PERMISSIONS
import com.artamonov.look4.look.LookActivity.Companion.requiredPermissions
import com.artamonov.look4.main.models.FetchMainStatus
import com.artamonov.look4.main.models.MainEvent
import com.artamonov.look4.main.models.MainViewState
import com.artamonov.look4.service.ForegroundService.Companion.ADVERTISING_FAILED_EVENT
import com.artamonov.look4.service.ForegroundService.Companion.ADVERTISING_SUCCEEDED_EVENT
import com.artamonov.look4.service.ForegroundService.Companion.SERVICE_IS_DESTROYED
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactAdvertiserUnseenState
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
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity(R.layout.activity_main) {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.supportActionBar?.hide()

        registerBroadcastReceiver(mMessageReceiver)
        handleIntentIfExist(intent)
        setAdView()

        viewModel.viewStates().observe(this, { bindViewState(it) })

        Glide.with(this).load(R.drawable.ic_black_o).into(letter_0_1)

        look_text.setOnClickListener { viewModel.obtainEvent(MainEvent.DiscoveringIsStarted) }
        offline_text.setSafeOnClickListener { checkForPermissions() }
        contacts_text.setOnClickListener { viewModel.obtainEvent(MainEvent.OpenContacts) }
        settings_text.setOnClickListener { viewModel.obtainEvent(MainEvent.OpenSettings) }
        main_look_gender_text.setOnClickListener { viewModel.obtainEvent(MainEvent.ChangeLookGender) }
    }

    private fun handleIntentIfExist(intent: Intent?) = intent?.extras?.let {
        Log.v("Look4", "main model: handleIntentIfExist: ${intent.extras}")
        startLookActivity()
    }

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

    private fun checkForPermissions() {
        if (!hasPermissions(this, requiredPermissions)) {
            firebaseCrashlytics.log("Permission is missing in checkForPermissions(): ${Manifest.permission.ACCESS_COARSE_LOCATION}")
            ActivityCompat.requestPermissions(this, requiredPermissions,
                REQUEST_CODE_REQUIRED_PERMISSIONS
            )
        } else {
            firebaseCrashlytics.log("Permission is given in checkForPermissions(): ${Manifest.permission.ACCESS_COARSE_LOCATION}")
            viewModel.obtainEvent(MainEvent.ChangeStatus)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_REQUIRED_PERMISSIONS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED
                ) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    viewModel.obtainEvent(MainEvent.ChangeStatus)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showSnackbarError(R.string.error_permissions_are_not_granted_for_discovering)
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    /** Returns true if the app was granted all the permissions. Otherwise, returns false.  */
    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PERMISSION_GRANTED
            ) {
                firebaseCrashlytics.log("Permission is missing: $permission")
                return false
            }
        }
        return true
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            Log.v("Look4", "onReceive: $intent")
            Log.v("Look4", "onReceive intent.action: : ${intent.action}")
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
