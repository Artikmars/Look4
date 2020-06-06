package com.artamonov.look4

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.main.MainActivity
import com.artamonov.look4.utils.PostTextChangeWatcher
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE
import com.artamonov.look4.utils.isValidPhoneNumber
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_welcome.*

const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 5545
var selectedImage: Uri? = null
var enteredPhoneNumber: String? = null

class WelcomeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        this.supportActionBar?.hide()

        etPhoneNumber.addTextChangedListener(PostTextChangeWatcher { phoneNumberChanged(it) })

        submit_button.setOnClickListener {
            if (fieldsAreValid()) {
                val isSaved = PreferenceHelper.createUserProfile(name = etName.text.toString(), phoneNumber =
                etPhoneNumber.text.toString(), imagePath = selectedImage.toString(), gender = getChosenGender())
                if (isSaved) { startMainActivity() }
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Please, don't leave fields blank", Snackbar.LENGTH_SHORT).show()
            }
        }

        welcome_add_image.setOnClickListener {
            dispatchTakePictureIntent() }

        if (PreferenceHelper.userAvailable()) {
            startMainActivity()
        }
    }

    private fun getChosenGender(): @UserGender.AnnotationUserGender String {
        when (welcome_radioGroup.checkedRadioButtonId) {
            R.id.welcome_radioFemale -> {
                return FEMALE
            }
            R.id.welcome_radioMale -> {
                return MALE
            }
        }
        return MALE
    }

    private fun phoneNumberChanged(newText: String?) {
        enteredPhoneNumber = newText?.trim()
        validatePhoneNumber()
    }

    private fun isPhoneNumberValid(phoneNumber: String?): Boolean {
        return phoneNumber?.isValidPhoneNumber() ?: false
    }

    private fun validatePhoneNumber() {
        return if (isPhoneNumberValid(enteredPhoneNumber)) {
            showPhoneNumberWarning(false)
        } else {
            showPhoneNumberWarning(true)
        }
    }

    private fun showPhoneNumberWarning(state: Boolean) {
        if (state) {
            etPhoneNumberLayout.error = resources.getString(R.string.welcome_phone_number_warning)
        } else {
            etPhoneNumberLayout.error = null
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivity(Intent(this, MainActivity::class.java),
                ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun fieldsAreValid(): Boolean {
        return !etName.text?.trim().isNullOrEmpty() && enteredPhoneNumber?.isValidPhoneNumber()
                ?: false && selectedImage != null
    }
}
