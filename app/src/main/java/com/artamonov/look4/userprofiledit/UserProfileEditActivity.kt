package com.artamonov.look4.userprofiledit

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.databinding.ActivityUserProfileEditBinding
import com.artamonov.look4.userprofiledit.models.FetchStatus
import com.artamonov.look4.userprofiledit.models.ProfileEditAction
import com.artamonov.look4.userprofiledit.models.ProfileEditEvent
import com.artamonov.look4.userprofiledit.models.ProfileEditViewState
import com.artamonov.look4.utils.PostTextChangeWatcher
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.showSnackbarError
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker

class UserProfileEditActivity : BaseActivity() {

    private lateinit var binding: ActivityUserProfileEditBinding
    private lateinit var viewModel: UserProfileEditViewModel
    private val takePhotoForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    viewModel.setImagePath(result.data?.data)
                    viewModel.obtainEvent(ProfileEditEvent.ProfilePhotoClicked)
                }
                ImagePicker.RESULT_ERROR -> {
                    Toast.makeText(this, ImagePicker.getError(result.data), Toast.LENGTH_SHORT)
                        .show()
                }
                else -> {
                    //   Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                viewModel.obtainEvent(ProfileEditEvent.CurrentProfileDataLoaded)
            } else {
                showSnackbarError(R.string.error_permissions_are_not_granted_for_setting_new_picture)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(UserProfileEditViewModel::class.java)

        viewModel.viewStates().observe(this) { bindViewState(it) }
        viewModel.viewEffects().observe(this) { bindViewActions(it) }

        viewModel.obtainEvent(ProfileEditEvent.ScreenShown)
        checkForPermissions()

        binding.userEditPhoneNumber.addTextChangedListener(PostTextChangeWatcher {
            viewModel.phoneNumberChanged(it)
        })

        binding.userEditName.addTextChangedListener(PostTextChangeWatcher {
            viewModel.nameChanged(it)
        })

        binding.radioGroup.setOnCheckedChangeListener { _, i ->
            viewModel.setCheckedRadioButton(i)
        }

        binding.profileEditBack.setOnClickListener { onBackPressed() }

        binding.userEditSubmitButton.setOnClickListener {
            viewModel.obtainEvent(ProfileEditEvent.SaveClicked)
        }

        binding.userEditAddImage.setOnClickListener {
            dispatchTakePictureIntent()
        }

        viewModel.phoneNumberLayoutErrorLiveData.observe(this) { state ->
            binding.userEditPhoneNumberLayout.apply {
                error = if (state == true) {
                    resources.getString(R.string.welcome_phone_number_warning)
                } else {
                    null
                }
            }
        }
    }

    private fun bindViewState(viewState: ProfileEditViewState) {
        when (viewState.fetchStatus) {
            FetchStatus.LoadingState -> {
                binding.userEditProgressBar.isVisible = true
            }
            FetchStatus.SucceededState -> {
                finish()
            }
            FetchStatus.PhoneValidationErrorState -> {
                binding.userEditProgressBar.isVisible = false
                showSnackbarError(R.string.error_blank_fields)
            }
            FetchStatus.ProfileWasNotUpdatedErrorState -> {
                binding.userEditProgressBar.isVisible = false
                showSnackbarError(R.string.error_general)
            }
            else -> {
                // nothing
            }
        }
    }

    private fun bindViewActions(viewAction: ProfileEditAction) {
        when (viewAction) {
            is ProfileEditAction.ShowSnackbar -> {
                binding.userEditProgressBar.isVisible = true
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
        binding.userEditName.setText(viewAction.name)
        binding.userEditPhoneNumber.setText(viewAction.phoneNumber)
        setRadioButtonState(viewAction.gender)
        val imageString = viewAction.imagePath
        if (!imageString.isNullOrEmpty()) {
            val bitmap =
                MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(imageString))
            Glide.with(this).load(bitmap).apply(RequestOptions.circleCropTransform())
                .into(binding.userEditAddImage)
        }
    }

    private fun setRadioButtonState(gender: @UserGender.AnnotationUserGender String?) {
        when (gender) {
            UserGender.MALE -> binding.radioMale.isChecked = true
            UserGender.FEMALE -> binding.radioFemale.isChecked = true
        }
    }

    private fun updateImage(uri: Uri) {
        Glide.with(this).load(uri).apply(RequestOptions.circleCropTransform())
            .into(binding.userEditAddImage)
    }

    private fun dispatchTakePictureIntent() {
        ImagePicker.with(this)
            .cropSquare()
            .compress(1024)
            .maxResultSize(300, 300)
            .createIntent { takePhotoForResult.launch(it) }
    }

    private fun checkForPermissions() {
        if (viewModel.hasPermissionsGranted(readExternalPermission)) {
            viewModel.obtainEvent(ProfileEditEvent.CurrentProfileDataLoaded)
        } else {
            requestPermissions.launch(readExternalPermission)

        }
    }

    companion object {
        val readExternalPermission = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    }
}
