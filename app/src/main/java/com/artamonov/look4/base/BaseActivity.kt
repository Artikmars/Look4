package com.artamonov.look4.base

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.artamonov.look4.ContactsActivity
import com.artamonov.look4.R
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactUnseenState
import com.artamonov.look4.utils.UserRole
import com.artamonov.look4.utils.default
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactUnseenState.default(ContactUnseenState.DisabledState)
        contactUnseenState.observe(this, Observer { state ->
            contactState = state
            when (state) {
                ContactUnseenState.EnabledState -> if (PreferenceHelper.getUserProfile()?.role == UserRole.DISCOVERER)
                    showSnackbarWithAction()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (contactState == ContactUnseenState.EnabledState) {
            showSnackbarWithAction()
            contactUnseenState.set(newValue = ContactUnseenState.DisabledState)
        }
    }
}
