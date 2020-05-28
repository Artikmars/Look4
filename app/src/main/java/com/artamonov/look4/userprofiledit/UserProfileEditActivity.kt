package com.artamonov.look4.userprofiledit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.artamonov.look4.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
import com.artamonov.look4.R
import com.artamonov.look4.data.database.User
import com.artamonov.look4.utils.PostTextChangeWatcher
import com.artamonov.look4.utils.UserGender
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_user_profile_edit.*

class UserProfileEditActivity : AppCompatActivity() {

    var newImage: Uri? = null
    private var userProfileEditViewModel = UserProfileEditViewModel()
    private var userProfile: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile_edit)

        userProfileEditViewModel.state.observe(this, Observer { state ->
            when (state) {
                UserEditProfileState.LoadingState -> {
                    user_edit_progress_bar.visibility = VISIBLE
                }
                UserEditProfileState.SucceededState -> {
                    finish()
                }
                UserEditProfileState.PhoneValidationErrorState -> {
                    user_edit_progress_bar.visibility = GONE
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.error_blank_fields),
                        Snackbar.LENGTH_SHORT).show()
                }
                UserEditProfileState.ProfileWasNotUpdatedErrorState -> {
                    user_edit_progress_bar.visibility = GONE
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.error_general),
                        Snackbar.LENGTH_SHORT).show()
                }
            }
        })

        userProfileEditViewModel.userProfileLiveData.observe(this, Observer { user ->
            userProfile = user
            checkForPermissions()
        })

        user_edit_phone_number.addTextChangedListener(PostTextChangeWatcher {
            userProfileEditViewModel.phoneNumberChanged(it) })

        profile_edit_back.setOnClickListener { onBackPressed() }

        user_edit_submit_button.setOnClickListener {
            userProfileEditViewModel.submitProfile(name = user_edit_name.text.toString(), phoneNumber =
            user_edit_phone_number.text.toString(), imagePath = newImage?.toString(),
                radioButtonId = radioGroup.checkedRadioButtonId)
        }

        user_edit_add_image.setOnClickListener {
            dispatchTakePictureIntent() }

        userProfileEditViewModel.phoneNumberLayoutErrorLiveData.observe(this, Observer { state ->
            if (state == true) { user_edit_phone_number_layout.error =
                resources.getString(R.string.welcome_phone_number_warning)
            } else { user_edit_phone_number_layout.error = null }
        })
    }

    private fun populateData() {
        user_edit_name.setText(userProfile?.name)
        user_edit_phone_number.setText(userProfile?.phoneNumber)
        setRadioButtonState()
        val imageString = userProfile?.imagePath
        imageString?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(imageString))
            Glide.with(this).load(bitmap).apply(RequestOptions.circleCropTransform()).into(user_edit_add_image)
        }
    }

    private fun setRadioButtonState() {
        when (userProfile?.gender) {
            UserGender.MALE -> radioMale.isChecked = true
            UserGender.FEMALE -> radioFemale.isChecked = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                newImage = data?.data
                Log.v("Look4", "uri : $newImage")
                Log.v("Look4", "uri.path : ${newImage?.path}")
                Glide.with(this).load(newImage).apply(RequestOptions.circleCropTransform()).into(user_edit_add_image)
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                //   Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
        }

    private fun dispatchTakePictureIntent() {
        ImagePicker.with(this)
            .cropSquare()
            .compress(1024)
            .maxResultSize(300, 300)
            .start()
    }

    private fun checkForPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                )
            } else {
            populateData()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    populateData()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish()
                    Snackbar.make(findViewById(android.R.id.content), "You need to grant the permission to read your profile picture",
                        Snackbar.LENGTH_SHORT).show()
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
}
