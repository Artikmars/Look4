package com.artamonov.look4.base

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.artamonov.look4.R
import com.artamonov.look4.contacts.ContactsActivity
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.main.MainActivity
import com.artamonov.look4.service.ForegroundService.Companion.ADVERTISING_FAILED
import com.artamonov.look4.service.ForegroundService.Companion.ADVERTISING_FAILED_EVENT
import com.artamonov.look4.service.ForegroundService.Companion.ADVERTISING_SUCCEEDED_EVENT
import com.artamonov.look4.service.ForegroundService.Companion.SERVICE_IS_DESTROYED
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactDiscovererUnseenState
import com.artamonov.look4.utils.NotificationHandler
import com.artamonov.look4.utils.colourMainButtonsToNormal
import com.artamonov.look4.utils.set
import com.artamonov.look4.utils.unblockInput
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_main.*

@AndroidEntryPoint
abstract class BaseActivity(contentLayoutId: Int) : AppCompatActivity(contentLayoutId) {

    private var contactState: ContactUnseenState = ContactUnseenState.DisabledState

    @Inject lateinit var prefs: PreferenceHelper
    @Inject lateinit var connectionClient: ConnectionsClient
    @Inject lateinit var firebaseCrashlytics: FirebaseCrashlytics

    fun showSnackbarError(resourceId: Int) { Snackbar.make(
        findViewById(android.R.id.content),
        getString(resourceId), Snackbar.LENGTH_LONG
    ).show() }

    fun showSnackbarError(stringMsg: String) { Snackbar.make(
        findViewById(android.R.id.content),
        stringMsg, Snackbar.LENGTH_LONG
    ).show() }

    fun showSnackbarWithAction() {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            getString(R.string.look_you_received_phone_number), Snackbar.LENGTH_LONG
        )
            .setAction(getString(R.string.look_view)) {
                startActivity(
                    Intent(this, ContactsActivity::class.java),
                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
                )
            }
        snackbar.show()
    }

    private fun showToast() {
        Toast.makeText(this, getString(R.string.look_you_received_phone_number), Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactDiscovererUnseenState.observe(this, { state ->
            contactState = state
            when (state) {
                ContactUnseenState.EnabledState -> {
                    showSnackbarError(R.string.look_you_received_phone_number)
                    contactDiscovererUnseenState.set(newValue = ContactUnseenState.DisabledState)
                }
            }
        })
    }

    override fun onResume() {
        val filter = IntentFilter(ADVERTISING_FAILED_EVENT)
        filter.addAction(ADVERTISING_SUCCEEDED_EVENT)
        filter.addAction(SERVICE_IS_DESTROYED)
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver, IntentFilter(filter)
        )
        super.onResume()
        if (prefs.getContactRequest().name != null) {
            val notificationHandler = NotificationHandler()
            startActivity(
                notificationHandler.createIntent(
                    this,
                    prefs.getContactRequest()
                )
            )
        }
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
        super.onPause()
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            // Get extra data included in the Intent
            when (intent.action) {
                ADVERTISING_SUCCEEDED_EVENT -> {
                    if (this@BaseActivity is MainActivity) {
                        offline_text.text = getString(R.string.main_online_mode)
                        showSnackbarError(R.string.main_advertising_has_started)
                        animateDot()
                        unblockInput()
                        colourMainButtonsToNormal()
                    }
                }
                ADVERTISING_FAILED_EVENT, SERVICE_IS_DESTROYED -> {
                    val message = intent.getStringExtra(ADVERTISING_FAILED)
                    message?.let { showSnackbarError(message) }
                    if (this@BaseActivity is MainActivity) {
                        offline_text.text = getString(R.string.main_offline_mode)
                        showSnackbarError(R.string.main_advertising_has_stopped)
                        connectionClient.stopAllEndpoints()
                        letter_0_1.clearAnimation()
                        unblockInput()
                        colourMainButtonsToNormal()
                    }
                }
            }
        }
    }
}
