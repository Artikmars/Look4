package com.artamonov.look4.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.artamonov.look4.R
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactDiscovererUnseenState
import com.artamonov.look4.utils.NotificationHandler
import com.artamonov.look4.utils.set
import com.artamonov.look4.utils.showSnackbarError
import com.google.android.gms.nearby.connection.ConnectionsClient
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
}
