package com.artamonov.look4.base

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.artamonov.look4.R
import com.artamonov.look4.contacts.ContactsActivity
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactDiscovererUnseenState
import com.artamonov.look4.utils.NotificationHandler
import com.artamonov.look4.utils.set
import com.google.android.material.snackbar.Snackbar

abstract class BaseActivity : AppCompatActivity() {

    private var contactState: ContactUnseenState = ContactUnseenState.DisabledState

    fun showSnackbarError(resourceId: Int) { Snackbar.make(findViewById(android.R.id.content),
        getString(resourceId), Snackbar.LENGTH_LONG).show() }

    fun showSnackbarError(stringMsg: String) { Snackbar.make(findViewById(android.R.id.content),
        stringMsg, Snackbar.LENGTH_LONG).show() }

    fun showSnackbarWithAction() {
        val snackbar = Snackbar.make(findViewById(android.R.id.content),
            getString(R.string.look_you_received_phone_number), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.look_view)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startActivity(Intent(this, ContactsActivity::class.java),
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                } else {
                    startActivity(Intent(this, ContactsActivity::class.java))
                }
            }
        snackbar.show()
    }

    private fun showToast() {
        Toast.makeText(this, getString(R.string.look_you_received_phone_number), Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactDiscovererUnseenState.observe(this, Observer { state ->
            contactState = state
            when (state) {
                ContactUnseenState.EnabledState -> {
                    showToast()
                    contactDiscovererUnseenState.set(newValue = ContactUnseenState.DisabledState)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (PreferenceHelper.getContactRequest() != null) {
            val notificationHandler = NotificationHandler()
            startActivity(notificationHandler.createIntent(this,
                PreferenceHelper.getContactRequest()!!))
        }
    }
}
