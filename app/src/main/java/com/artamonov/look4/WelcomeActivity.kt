package com.artamonov.look4

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.utils.PostTextChangeWatcher
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE
import com.artamonov.look4.utils.isValidPhoneNumber
import com.artamonov.look4.utils.startMainActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import kotlinx.android.synthetic.main.activity_welcome.*

const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 5545
var selectedImage: Uri? = null
var enteredPhoneNumber: String? = null

class WelcomeActivity : BaseActivity(R.layout.activity_welcome) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (prefs.userAvailable()) {
            startMainActivity()
            finish()
        }

        supportActionBar?.hide()

        etPhoneNumber.addTextChangedListener(PostTextChangeWatcher { phoneNumberChanged(it) })

        submit_button.setOnClickListener {
            if (fieldsAreValid()) {
                val isSaved = prefs.createUserProfile(name = etName.text.toString(), phoneNumber =
                etPhoneNumber.text.toString(), imagePath = selectedImage.toString(), gender = getChosenGender())
                if (isSaved) {
                    startMainActivity()
                    finish() }
            } else {
                showSnackbarError(R.string.error_blank_fields)
            }
        }

        welcome_add_image.setOnClickListener {
            dispatchTakePictureIntent() }
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
                showSnackbarError(getString(R.string.welcome_processinng_image_error))
            }
            else -> {
                //   Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fieldsAreValid(): Boolean {
        return !etName.text?.trim().isNullOrEmpty() && enteredPhoneNumber?.isValidPhoneNumber()
                ?: false && selectedImage != null
    }
}
