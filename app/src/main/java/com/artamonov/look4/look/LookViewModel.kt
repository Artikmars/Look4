package com.artamonov.look4.look

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.artamonov.look4.base.BaseViewModel
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactUnseenState
import com.artamonov.look4.utils.NotificationHandler
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.UserRole
import com.artamonov.look4.utils.default
import com.artamonov.look4.utils.isValidPhoneNumber
import com.artamonov.look4.utils.set
import com.google.android.gms.nearby.connection.Payload
import java.io.File
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class LookState {
    object DefaultState : LookState()
    object SearchState : LookState()
    class SucceededAdvertiserIsFoundState<User>(val user: User) : LookState()
    class SucceededDiscoverIsFoundState<User>(val user: User) : LookState()
    object NoFoundState : LookState()
    object PhoneNumberReceived : LookState()
    object ErrorState : LookState()
}

class LookViewModel : BaseViewModel() {

    var isGenderValid: Boolean = true
    var advertiserName: String? = null
    var discovererName: String? = null
    var discovererPhoneNumber: String? = null
    var discovererFilePath: String? = null
    private var advertiserPhoneNumber: String? = null
    var endpointIdSaved: String? = null
    private var file: File? = null
    var newFile: File? = null
    private lateinit var notificationHandler: NotificationHandler
    val state = MutableLiveData<LookState>().default(initialValue = LookState.DefaultState)
    var user: MutableLiveData<User> = MutableLiveData()

    init {
        loadUserFromDB()
    }

    fun populateData() {
        state.set(newValue = LookState.SucceededAdvertiserIsFoundState(user = PreferenceHelper.getUserProfile()))
    }

    private fun loadUserFromDB() {
        user.postValue(PreferenceHelper.getUserProfile())
    }

    fun updateRole(role: @UserRole.AnnotationUserRole String) {
        viewModelScope.launch(Dispatchers.IO) {
            PreferenceHelper.updateRole(role)
            user.postValue(PreferenceHelper.getUserProfile())
        }
    }

    fun startDiscovering() { state.set(newValue = LookState.SearchState) }

    fun setNoFoundState() { state.set(newValue = LookState.NoFoundState) }

    fun handlePayload(p1: Payload) {
        when (PreferenceHelper.getUserProfile()?.role) {
            UserRole.DISCOVERER -> {
                when (p1.type) {
                    Payload.Type.BYTES -> {
                        val byteString = p1.asBytes()?.toString(Charset.defaultCharset())
                        val textArray = byteString?.split(";")?.toTypedArray()
                        when (textArray?.size) {
                            0, 1 -> isGenderValid = true
                            2 -> if (!isGenderValid(textArray[1])) { return }
                        }

                        if (isMobileNumber(textArray?.get(0))) {
                            savePhoneNumberToDB(textArray?.get(0), UserRole.DISCOVERER)
                            contactUnseenState.set(newValue = ContactUnseenState.EnabledState)
                            state.set(newValue = LookState.PhoneNumberReceived)
                        } else {
                            advertiserName = textArray?.get(0)
                        }
                    }
                    Payload.Type.FILE -> {
                        file = p1.asFile()?.asJavaFile()
                        file?.renameTo(
                            File(file?.parentFile, "look4.jpg")
                        )
                        newFile = File(file?.parentFile, "look4.jpg")
                    }
                }
            }
            UserRole.ADVERTISER -> {
                val receivedContact = p1.asBytes()?.toString(Charset.defaultCharset())
                val textArray = receivedContact?.split(";")?.toTypedArray()
                discovererPhoneNumber = textArray?.get(1)
                state.set(newValue = LookState.SucceededAdvertiserIsFoundState(user = PreferenceHelper.getUserProfile()))
            }
        }
    }

    private fun isMobileNumber(string: String?): Boolean {
        return string?.isValidPhoneNumber() ?: false
    }

    private fun isGenderValid(advertiserGender: @UserGender.AnnotationUserGender String?): Boolean {
        when (PreferenceHelper.getUserProfile()?.lookGender) {
            UserGender.MALE -> {
                isGenderValid = advertiserGender == UserGender.MALE
                return isGenderValid
            }
            UserGender.FEMALE -> {
                isGenderValid = advertiserGender == UserGender.FEMALE
                return isGenderValid
            }
            UserGender.ALL -> {
                isGenderValid = true
                return isGenderValid
            }
        }
        isGenderValid = true
        return isGenderValid
    }

    fun savePhoneNumberToDB(phoneNumber: String?, userRole: String) {
        when (userRole) {
            UserRole.DISCOVERER -> {
                if (phoneNumber != null && advertiserName != null) {
                    PreferenceHelper.saveContact(name = advertiserName!!, phoneNumber = phoneNumber)
                }
            }
            UserRole.ADVERTISER -> {
                if (phoneNumber != null && discovererName != null) {
                    PreferenceHelper.saveContact(name = discovererName!!, phoneNumber = phoneNumber)
                }
            }
        }
    }

    fun handleNewIntent(intent: Intent?) {
        if (intent == null || intent.extras == null) {
            return
        }
        notificationHandler = NotificationHandler(intent)

        if (notificationHandler.isNotificationValid()) {
            discovererFilePath = notificationHandler.getDiscovererFilePath()
            discovererName = notificationHandler.getDiscovererName()
            discovererPhoneNumber = notificationHandler.getDiscovererPhoneNumber()
            endpointIdSaved = notificationHandler.getEndpointId()
            state.set(newValue = LookState.SucceededDiscoverIsFoundState(user = PreferenceHelper.getUserProfile()))
        } else {
            state.set(newValue = LookState.DefaultState)
        }
//        else if (LookActivity.advertiserPhoneNumber != null) {
//            populateDefaultView()
//            searchingInProgressText.text =
//                LookActivity.advertiserPhoneNumber
//            searchingInProgressText.visibility = View.VISIBLE
//        }
    }
}
