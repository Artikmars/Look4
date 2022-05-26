package com.artamonov.look4.userprofiledit

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseVM
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.userprofiledit.models.FetchStatus
import com.artamonov.look4.userprofiledit.models.ProfileEditAction
import com.artamonov.look4.userprofiledit.models.ProfileEditEvent
import com.artamonov.look4.userprofiledit.models.ProfileEditViewState
import com.artamonov.look4.utils.PermissionChecker
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE
import com.artamonov.look4.utils.isValidPhoneNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserProfileEditViewModel @Inject constructor(
    private val prefs: PreferenceHelper,
    private val permissionChecker: PermissionChecker
) : BaseVM<ProfileEditViewState, ProfileEditAction, ProfileEditEvent>() {
    var phoneNumberLayoutErrorLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private var newPhoneNumber: String? = null
    private var newName: String? = null
    private var newImageUri: Uri? = null
    private var checkedRadioButtonId: Int? = null

    init {
        viewState = ProfileEditViewState(
            fetchStatus = FetchStatus.DefaultState,
            data = prefs.getUserProfile()
        )
    }

    override fun obtainEvent(viewEvent: ProfileEditEvent) {
        when (viewEvent) {
            ProfileEditEvent.ProfilePhotoClicked -> {
                newImageUri?.let { viewAction = ProfileEditAction.UpdateImage(it) }
            }
            ProfileEditEvent.SaveClicked ->
                if (shouldUpdate()) submitProfile(
                    User(
                        name = newName ?: viewState.data?.name,
                        phoneNumber = newPhoneNumber
                            ?: viewState.data?.name,
                        imagePath = if (newImageUri != null) newImageUri.toString()
                        else viewState.data?.imagePath
                    )
                ) else {
                    viewState.copy(fetchStatus = FetchStatus.SucceededState)
                }
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

    private fun submitProfile(user: User) {
        viewState = viewState.copy(fetchStatus = FetchStatus.LoadingState)
        if (!fieldsAreValid(user.name, user.phoneNumber)) {
            viewState = viewState.copy(fetchStatus = FetchStatus.PhoneValidationErrorState)
            return
        }
        val isUpdated =
            prefs.updateUserProfile(user.copy(gender = getChosenGender(checkedRadioButtonId)))
        viewState = if (isUpdated) viewState.copy(
            fetchStatus = FetchStatus.SucceededState,
            data = prefs.getUserProfile()
        )
        else viewState.copy(fetchStatus = FetchStatus.ProfileWasNotUpdatedErrorState)
    }

    private fun getChosenGender(id: Int?): @UserGender.AnnotationUserGender String? {
        when (id) {
            R.id.radioFemale -> {
                return FEMALE
            }
            R.id.radioMale -> {
                return MALE
            }
        }
        return viewState.data?.gender
    }

    fun phoneNumberChanged(newText: String?) {
        newPhoneNumber = newText?.trim()
        phoneNumberLayoutErrorLiveData.value = !isPhoneNumberValid(newPhoneNumber)
    }

    fun nameChanged(newText: String?) {
        newName = newText?.trim()
    }

    fun setImagePath(uri: Uri?) {
        uri?.let { newImageUri = it }
    }

    fun setCheckedRadioButton(resourceId: Int) {
        checkedRadioButtonId = resourceId
    }

    private fun isPhoneNumberValid(phoneNumber: String?): Boolean {
        return phoneNumber?.isValidPhoneNumber() ?: false
    }

    private fun shouldUpdate(): Boolean {
        return newName != null || newImageUri != null || newPhoneNumber != null
    }

    fun hasPermissionsGranted(permissions: Array<String>): Boolean {
        return permissionChecker.hasPermissionsGranted(permissions)
    }
}
