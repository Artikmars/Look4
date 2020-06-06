package com.artamonov.look4.base

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.artamonov.look4.ContactsActivity
import com.artamonov.look4.R
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactUnseenState
import com.artamonov.look4.utils.default
import com.artamonov.look4.utils.set
import com.google.android.material.snackbar.Snackbar

abstract class BaseActivity : AppCompatActivity() {

    fun showSnackbarError(resourceId: Int) { Snackbar.make(findViewById(android.R.id.content),
        getString(resourceId), Snackbar.LENGTH_LONG).show() }

    fun showSnackbarError(stringMsg: String) { Snackbar.make(findViewById(android.R.id.content),
        stringMsg, Snackbar.LENGTH_LONG).show() }

    fun showSnackbarWithAction() {
        val snackbar = Snackbar.make(findViewById(android.R.id.content),
            getString(R.string.look_you_received_phone_number), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.look_view)) {
                startActivity(Intent(this, ContactsActivity::class.java))
            }
        snackbar.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactUnseenState.default(ContactUnseenState.DisabledState)
        contactUnseenState.observe(this, Observer { state ->
            when (state) {
                ContactUnseenState.EnabledState -> showSnackbarWithAction()
            }
        })
    }
}