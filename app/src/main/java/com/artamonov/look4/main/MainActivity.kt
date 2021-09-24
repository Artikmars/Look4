package com.artamonov.look4.main

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.databinding.ActivityMainBinding
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

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.entries.all { it.value == true }
        if (granted) {
            viewModel.obtainEvent(MainEvent.ChangeStatus)
        } else {
            showSnackbarError(R.string.error_permissions_are_not_granted_for_discovering)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.supportActionBar?.hide()

        registerBroadcastReceiver(mMessageReceiver)
        handleIntentIfExist(intent)
        setAdView()

        viewModel.viewStates().observe(this, { bindViewState(it) })

        Glide.with(this).load(R.drawable.ic_black_o).into(binding.letter01)

       // look_text.setOnClickListener { viewModel.obtainEvent(MainEvent.DiscoveringIsStarted) }
        binding.offlineText.setSafeOnClickListener { checkForPermissions() }
        binding.contactsText.setOnClickListener { viewModel.obtainEvent(MainEvent.OpenContacts) }
        binding.settingsText.setOnClickListener { viewModel.obtainEvent(MainEvent.OpenSettings) }
        binding.mainLookGenderText.setOnClickListener { viewModel.obtainEvent(MainEvent.ChangeLookGender) }
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
                binding.offlineText.text = getString(R.string.main_online_mode)
                binding.letter01.animateDot(this)
                unblockInput()
                showSnackbarError(R.string.main_advertising_has_started)
            }
            is FetchMainStatus.OfflineEnabledState -> {
                connectionClient.stopAllEndpoints()
                binding.offlineText.text = getString(R.string.main_offline_mode)
                binding.letter01.clearAnimation()
                unblockInput()
                showSnackbarError(R.string.main_advertising_has_stopped)
            }
            is FetchMainStatus.LookGenderManState -> {
                binding.mainLookGenderText.text = getString(R.string.main_man)
            }
            is FetchMainStatus.LookGenderWomenState -> {
                binding.mainLookGenderText.text = getString(R.string.main_woman)
            }
            is FetchMainStatus.LookGenderAllState -> {
                binding.mainLookGenderText.text = getString(R.string.main_all)
            }
        }
    }

    private fun setLookGenderText(lookGender: String?) {
        binding.mainLookGenderText.apply {
            when (lookGender) {
                MALE -> text = getString(R.string.main_man)
                FEMALE -> text = getString(R.string.main_woman)
                ALL -> text = getString(R.string.main_all)
            }
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
            requestPermissions.launch(requiredPermissions)
        } else {
            firebaseCrashlytics.log("Permission is given in checkForPermissions(): ${Manifest.permission.ACCESS_COARSE_LOCATION}")
            viewModel.obtainEvent(MainEvent.ChangeStatus)
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
