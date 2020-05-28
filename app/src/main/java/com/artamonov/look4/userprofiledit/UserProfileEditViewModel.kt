package com.artamonov.look4.userprofiledit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseViewModel
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.isValidPhoneNumber

class UserProfileEditViewModel : BaseViewModel() {

    private var userProfileLiveData: MutableLiveData<User> = MutableLiveData()
    private var phoneNumberLayoutErrorLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private var enteredPhoneNumber: String? = null

    init {
        loadUserFromDB()
    }

    fun getUser(): LiveData<User> {
        return userProfileLiveData
    }

    fun getPhoneNumberLayoutErrorState(): LiveData<Boolean> {
        return phoneNumberLayoutErrorLiveData
    }

    fun updateUserProfile(
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

    fun getChosenGender(id: Int): @UserGender.AnnotationUserGender String? {
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
