package com.artamonov.look4

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_welcome.*

const val USER_NAME = "USER_NAME"
const val USER_PHONE_NUMBER = "USER_PHONE_NUMBER"

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        submit_button.setOnClickListener {
            if (fieldsAreValid()) {
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
                val editor = sharedPref.edit()
                val isSaved = editor
                    .putString(USER_NAME, etName.text.toString())
                    .putString(USER_PHONE_NUMBER, etPhoneNumber.text.toString())
                    .commit()
                if (isSaved) { startMainActivity() }
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Please, don't leave fields blank", Snackbar.LENGTH_SHORT).show()
            }
        }

        if (userExists()) {
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun userExists(): Boolean {
        return !PreferenceManager.getDefaultSharedPreferences(this).getString(USER_NAME, null).isNullOrEmpty() &&
                !PreferenceManager.getDefaultSharedPreferences(this).getString(USER_PHONE_NUMBER, null).isNullOrEmpty()
    }

    private fun fieldsAreValid(): Boolean {
        return !etName.text?.trim().isNullOrEmpty() && !etPhoneNumber.text?.trim().isNullOrEmpty()
    }
}
