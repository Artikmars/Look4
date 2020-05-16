package com.artamonov.look4

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_welcome.*
import org.koin.android.ext.android.inject

const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 5545
var selectedImage: Uri? = null

class WelcomeActivity : BaseActivity() {

    private val preferenceHelper: PreferenceHelper by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        submit_button.setOnClickListener {
            if (fieldsAreValid()) {
                val isSaved = preferenceHelper.createUserProfile(name = etName.text.toString(), phoneNumber =
                etPhoneNumber.text.toString(), imagePath = selectedImage.toString())
                if (isSaved) { startMainActivity() }
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Please, don't leave fields blank", Snackbar.LENGTH_SHORT).show()
            }
        }

        welcome_add_image.setOnClickListener {
            dispatchTakePictureIntent() }

        if (preferenceHelper.userAvailable()) {
            startMainActivity()
        }
    }

    private fun dispatchTakePictureIntent() {
        ImagePicker.with(this)
            .cropSquare()
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

    private fun fieldsAreValid(): Boolean {
        return !etName.text?.trim().isNullOrEmpty() && !etPhoneNumber.text?.trim().isNullOrEmpty() && selectedImage != null
    }
}
