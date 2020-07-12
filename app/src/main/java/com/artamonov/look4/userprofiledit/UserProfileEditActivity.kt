package com.artamonov.look4.userprofiledit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.artamonov.look4.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.userprofiledit.models.FetchStatus
import com.artamonov.look4.userprofiledit.models.ProfileEditAction
import com.artamonov.look4.userprofiledit.models.ProfileEditEvent
import com.artamonov.look4.userprofiledit.models.ProfileEditViewState
import com.artamonov.look4.utils.PostTextChangeWatcher
import com.artamonov.look4.utils.UserGender
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_user_profile_edit.*

class UserProfileEditActivity : BaseActivity() {

    private lateinit var viewModel: UserProfileEditViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile_edit)

        viewModel = ViewModelProvider(this).get(UserProfileEditViewModel::class.java)

        viewModel.viewStates().observe(this, Observer { bindViewState(it) })
        viewModel.viewEffects().observe(this, Observer { bindViewActions(it) })

        viewModel.obtainEvent(ProfileEditEvent.ScreenShown)
        checkForPermissions()

        user_edit_phone_number.addTextChangedListener(PostTextChangeWatcher {
            viewModel.phoneNumberChanged(it) })

        user_edit_name.addTextChangedListener(PostTextChangeWatcher {
            viewModel.nameChanged(it) })

        radioGroup.setOnCheckedChangeListener { _, i ->
            viewModel.setCheckedRadioButton(i)
        }

        profile_edit_back.setOnClickListener { onBackPressed() }

        user_edit_submit_button.setOnClickListener {
            viewModel.obtainEvent(ProfileEditEvent.SaveClicked)
        }

        user_edit_add_image.setOnClickListener {
            dispatchTakePictureIntent() }

        viewModel.phoneNumberLayoutErrorLiveData.observe(this, Observer { state ->
            if (state == true) { user_edit_phone_number_layout.error =
                resources.getString(R.string.welcome_phone_number_warning)
            } else { user_edit_phone_number_layout.error = null }
        })
    }

    private fun bindViewState(viewState: ProfileEditViewState) {
        when (viewState.fetchStatus) {
            FetchStatus.LoadingState -> {
                user_edit_progress_bar.visibility = VISIBLE
            }
            FetchStatus.SucceededState -> { finish() }
            FetchStatus.PhoneValidationErrorState -> {
                user_edit_progress_bar.visibility = GONE
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.error_blank_fields),
                    Snackbar.LENGTH_SHORT).show()
            }
            FetchStatus.ProfileWasNotUpdatedErrorState -> {
                user_edit_progress_bar.visibility = GONE
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.error_general),
                    Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindViewActions(viewAction: ProfileEditAction) {
        when (viewAction) {
            is ProfileEditAction.ShowSnackbar -> {
                user_edit_progress_bar.visibility = VISIBLE
            }
            is ProfileEditAction.UpdateImage -> {
                updateImage(viewAction.uri)
            }
            is ProfileEditAction.PopulateCurrentProfileData -> {
                populateData(viewAction)
            }
        }
    }

    private fun populateData(viewAction: ProfileEditAction.PopulateCurrentProfileData) {
        user_edit_name.setText(viewAction.name)
        user_edit_phone_number.setText(viewAction.phoneNumber)
        setRadioButtonState(viewAction.gender)
        val imageString = viewAction.imagePath
        imageString?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(imageString))
            Glide.with(this).load(bitmap).apply(RequestOptions.circleCropTransform()).into(user_edit_add_image)
        }
    }

    private fun setRadioButtonState(gender: @UserGender.AnnotationUserGender String?) {
        when (gender) {
            UserGender.MALE -> radioMale.isChecked = true
            UserGender.FEMALE -> radioFemale.isChecked = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                data?.data?.let {
                    viewModel.setImagePath(it)
                    viewModel.obtainEvent(ProfileEditEvent.ProfilePhotoClicked)
                }
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
            else -> {
                //   Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
        }

    private fun updateImage(uri: Uri) {
        Glide.with(this).load(uri).apply(RequestOptions.circleCropTransform()).into(user_edit_add_image)
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
            viewModel.obtainEvent(ProfileEditEvent.CurrentProfileDataLoaded)
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
                    viewModel.obtainEvent(ProfileEditEvent.CurrentProfileDataLoaded)
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
