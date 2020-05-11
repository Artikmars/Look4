package com.artamonov.look4

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_welcome.*

const val USER_NAME = "USER_NAME"
const val USER_PHONE_NUMBER = "USER_PHONE_NUMBER"
const val USER_IMAGE_URI = "USER_IMAGE_URI"
const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 5545
var selectedImage: Uri? = null

class WelcomeActivity : BaseActivity() {

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
                    .putString(USER_IMAGE_URI, selectedImage.toString())
                    .commit()
                if (isSaved) { startMainActivity() }
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Please, don't leave fields blank", Snackbar.LENGTH_SHORT).show()
            }
        }

        welcome_add_image.setOnClickListener {
            dispatchTakePictureIntent() }

        if (userExists()) {
            startMainActivity()
        }
    }

    private fun dispatchTakePictureIntent() {
        ImagePicker.with(this)
            .crop()
            .compress(1024)
            .maxResultSize(300, 300)
            .start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                selectedImage = data?.data
                Glide.with(this).load(selectedImage).apply(RequestOptions.circleCropTransform()).into(welcome_add_image)
            }
            ImagePicker.RESULT_ERROR -> {
                showSnackbarError("Error while processing your photo")
            }
            else -> {
                //   Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun userExists(): Boolean {
        return !PreferenceManager.getDefaultSharedPreferences(this).getString(USER_NAME, null).isNullOrEmpty() &&
                !PreferenceManager.getDefaultSharedPreferences(this).getString(USER_PHONE_NUMBER, null).isNullOrEmpty() &&
                !PreferenceManager.getDefaultSharedPreferences(this).getString(USER_IMAGE_URI, null).isNullOrEmpty()
    }

    private fun fieldsAreValid(): Boolean {
        return !etName.text?.trim().isNullOrEmpty() && !etPhoneNumber.text?.trim().isNullOrEmpty() && selectedImage != null
    }
}
