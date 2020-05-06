package com.artamonov.look4

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_user_profile_edit.*
import kotlinx.android.synthetic.main.activity_welcome.*

class UserProfileEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile_edit)

        populateData()

        user_edit_submit_button.setOnClickListener {
            if (!fieldsAreValid()) {
                Snackbar.make(findViewById(android.R.id.content), "Please, don't leave fields blank", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener }
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = sharedPref.edit()
            val isSaved = editor
                .putString(USER_NAME, user_edit_name.text.toString())
                .putString(USER_PHONE_NUMBER, user_edit_phone_number.text.toString())
                .commit()
            if (isSaved) { finish() }
        }
    }

    private fun populateData() {
        user_edit_name.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(USER_NAME, null))
        user_edit_phone_number.setText(PreferenceManager.getDefaultSharedPreferences(this).getString(
            USER_PHONE_NUMBER, null))
    }

    private fun fieldsAreValid(): Boolean {
        return !user_edit_name.text?.trim().isNullOrEmpty() && !user_edit_phone_number.text?.trim().isNullOrEmpty()
    }
}
