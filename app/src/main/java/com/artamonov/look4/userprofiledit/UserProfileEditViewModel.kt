package com.artamonov.look4.userprofiledit

import androidx.lifecycle.MutableLiveData
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseViewModel
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.default
import com.artamonov.look4.utils.isValidPhoneNumber
import com.artamonov.look4.utils.set

sealed class UserEditProfileState {
    object DefaultState : UserEditProfileState()
    object LoadingState : UserEditProfileState()
    object SucceededState : UserEditProfileState()
    object PhoneValidationErrorState : UserEditProfileState()
    object ProfileWasNotUpdatedErrorState : UserEditProfileState()
}

class UserProfileEditViewModel : BaseViewModel() {

    val state = MutableLiveData<UserEditProfileState>().default(initialValue = UserEditProfileState.DefaultState)

    var userProfileLiveData: MutableLiveData<User> = MutableLiveData()
    var phoneNumberLayoutErrorLiveData: MutableLiveData<Boolean> = MutableLiveData()
    var enteredPhoneNumber: String? = null

    init {
        loadUserFromDB()
    }

    private fun fieldsAreValid(name: String?, phoneNumber: String?): Boolean {
        return !name?.trim().isNullOrEmpty() && !phoneNumber?.trim().isNullOrEmpty()
    }

    fun submitProfile(
        name: String?,
        phoneNumber: String?,
        radioButtonId: Int,
        imagePath: String?
    ) {
        state.set(newValue = UserEditProfileState.LoadingState)
        if (!fieldsAreValid(name, phoneNumber)) {
            state.set(newValue = UserEditProfileState.PhoneValidationErrorState)
            return
        }
        val gender = getChosenGender(radioButtonId)
        val isUpdated = updateUserProfile(name, phoneNumber, gender, imagePath)

        if (isUpdated) state.set(newValue = UserEditProfileState.SucceededState) else
            state.set(newValue = UserEditProfileState.ProfileWasNotUpdatedErrorState)
    }

    private fun updateUserProfile(
        name: String?,
        phoneNumber: String?,
        gender: @UserGender.AnnotationUserGender String?,
        imagePath: String?
    ): Boolean {
        dataLoading.value = true
        val isUpdated = PreferenceHelper.updateUserProfile(name, phoneNumber, gender, imagePath)
        loadUserFromDB()
        dataLoading.value = false
        return isUpdated
    }

    private fun loadUserFromDB() {
        userProfileLiveData.postValue(PreferenceHelper.getUserProfile())
    }

    private fun getChosenGender(id: Int): @UserGender.AnnotationUserGender String? {
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

    private fun isPhoneNumberValid(phoneNumber: String?): Boolean {
        return phoneNumber?.isValidPhoneNumber() ?: false
    }
}
