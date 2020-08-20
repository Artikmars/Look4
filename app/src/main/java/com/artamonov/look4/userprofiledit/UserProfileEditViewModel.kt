package com.artamonov.look4.userprofiledit

import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseVM
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.userprofiledit.models.FetchStatus
import com.artamonov.look4.userprofiledit.models.ProfileEditAction
import com.artamonov.look4.userprofiledit.models.ProfileEditEvent
import com.artamonov.look4.userprofiledit.models.ProfileEditViewState
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.isValidPhoneNumber

class UserProfileEditViewModel @ViewModelInject constructor(
    private val prefs: PreferenceHelper
) : BaseVM<ProfileEditViewState, ProfileEditAction, ProfileEditEvent>() {
    var phoneNumberLayoutErrorLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private var enteredPhoneNumber: String? = null
    private var enteredName: String? = null
    private var imageUri: Uri? = null
    var checkedRadioButtonId: Int? = null

    init {
        viewState = ProfileEditViewState(
            fetchStatus = FetchStatus.DefaultState,
            data = prefs.getUserProfile()
        )
    }

    override fun obtainEvent(viewEvent: ProfileEditEvent) {
        when (viewEvent) {
            ProfileEditEvent.ProfilePhotoClicked -> {
                imageUri?.let { viewAction = ProfileEditAction.UpdateImage(it) }
            }
            ProfileEditEvent.SaveClicked -> submitProfile(
                name = enteredName,
                phoneNumber = enteredPhoneNumber,
                imagePath = if (imageUri != null) imageUri.toString() else null,
                radioButtonId = checkedRadioButtonId
            )
            ProfileEditEvent.CurrentProfileDataLoaded -> viewAction = ProfileEditAction
                .PopulateCurrentProfileData(
                    name = viewState.data?.name, phoneNumber =
                    viewState.data?.phoneNumber, imagePath = viewState.data?.imagePath, gender =
                    viewState.data?.gender
                )
        }
    }

    private fun fieldsAreValid(name: String?, phoneNumber: String?): Boolean {
        return !name?.trim().isNullOrEmpty() && !phoneNumber?.trim().isNullOrEmpty()
    }

    private fun submitProfile(
        name: String?,
        phoneNumber: String?,
        radioButtonId: Int?,
        imagePath: String?
    ) {
        viewState = viewState.copy(fetchStatus = FetchStatus.LoadingState)
        if (!fieldsAreValid(name, phoneNumber)) {
            viewState = viewState.copy(fetchStatus = FetchStatus.PhoneValidationErrorState)
            return
        }
        val gender = getChosenGender(radioButtonId)
        val isUpdated = updateUserProfile(name, phoneNumber, gender, imagePath)

        viewState = if (isUpdated) viewState.copy(fetchStatus = FetchStatus.SucceededState)
        else viewState.copy(fetchStatus = FetchStatus.ProfileWasNotUpdatedErrorState)
    }

    private fun updateUserProfile(
        name: String?,
        phoneNumber: String?,
        gender: @UserGender.AnnotationUserGender String?,
        imagePath: String?
    ): Boolean {
        val isUpdated = prefs.updateUserProfile(name, phoneNumber, gender, imagePath)
        viewState.copy(data = prefs.getUserProfile())
        return isUpdated
    }

    private fun getChosenGender(id: Int?): @UserGender.AnnotationUserGender String? {
        when (id) {
            R.id.radioFemale -> {
                return UserGender.FEMALE
            }
            R.id.radioMale -> {
                return UserGender.MALE
            }
        }
        return null
    }

    fun phoneNumberChanged(newText: String?) {
        enteredPhoneNumber = newText?.trim()
        phoneNumberLayoutErrorLiveData.value = !isPhoneNumberValid(enteredPhoneNumber)
    }

    fun nameChanged(newText: String?) {
        enteredName = newText?.trim()
    }

    fun setImagePath(uri: Uri?) {
        uri?.let { imageUri = it }
    }

    fun setCheckedRadioButton(resourceId: Int) {
        checkedRadioButtonId = resourceId
    }

    private fun isPhoneNumberValid(phoneNumber: String?): Boolean {
        return phoneNumber?.isValidPhoneNumber() ?: false
    }
}
