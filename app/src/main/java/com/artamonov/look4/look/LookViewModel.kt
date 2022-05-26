package com.artamonov.look4.look

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.artamonov.look4.base.BaseViewModel
import com.artamonov.look4.data.database.ContactRequest
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.*
import com.artamonov.look4.utils.LiveDataContactListState.contactListState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactDiscovererUnseenState
import com.artamonov.look4.utils.UserGender.Companion.ALL
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE
import com.google.android.gms.nearby.connection.Payload
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LookState {
    object DefaultState : LookState()
    object SearchState : LookState()
    class SucceededAdvertiserIsFoundState<User>(val user: User) : LookState()
    class SucceededDiscoverIsFoundState<User>(val user: User) : LookState()
    object NoFoundState : LookState()
    object PendingState : LookState()
    object PhoneNumberReceived : LookState()
    object ErrorState : LookState()
}

sealed class LookAction {
    object ShowDialog : LookAction()
}

@HiltViewModel
class LookViewModel @Inject constructor(
    private val prefs: PreferenceHelper,
    private val permissionChecker: PermissionChecker
) : BaseViewModel() {

    var isGenderValid: Boolean = true
    var advertiserName: String? = null
    var discovererName: String? = null
    var discovererPhoneNumber: String? = null
    var discovererFilePath: String? = null
    private var advertiserPhoneNumber: String? = null
    var endpointIdSaved: String? = null
    private var file: File? = null
    var newFile: File? = null
    private var notificationHandler: NotificationHandler? = null
    val state = MutableLiveData<LookState>().default(initialValue = LookState.DefaultState)
    val _state: LiveData<LookState> = state
    val action = MutableLiveData<LookAction>()
    val _action: LiveData<LookAction> = action
    val user: MutableLiveData<User> = MutableLiveData()
    val _user: LiveData<User> = user

    init {
        loadUserFromDB()
    }

    fun populateData() {
        state.set(newValue = LookState.SucceededAdvertiserIsFoundState(user = prefs.getUserProfile()))
    }

    private fun loadUserFromDB() {
        user.postValue(prefs.getUserProfile())
    }

    fun updateRole(role: @UserRole.AnnotationUserRole String) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.updateRole(role)
            user.postValue(prefs.getUserProfile())
        }
    }

    fun startDiscovering() {
        state.set(newValue = LookState.SearchState)
    }

    fun setNoFoundState() {
        state.set(newValue = LookState.NoFoundState)
    }

    fun setPendingState() {
        state.set(newValue = LookState.PendingState)
    }

    fun handlePayload(p1: Payload) {
        when (prefs.getUserProfile().role) {
            UserRole.DISCOVERER -> {
                when (p1.type) {
                    Payload.Type.BYTES -> {
                        val textArray = p1.asBytes()?.toString(Charset.defaultCharset())
                            ?.split(";")?.toTypedArray()
                        when (textArray?.size) {
                            0, 1 -> isGenderValid = true
                            2 -> if (!isGenderValid(textArray[1])) {
                                return
                            }
                        }

                        if (textArray?.get(0).isValidPhoneNumber()) {
                            savePhoneNumberToDB(textArray?.get(0), UserRole.DISCOVERER)
                            contactDiscovererUnseenState.set(newValue = ContactUnseenState.EnabledState)
                            contactListState.set(newValue = ContactsState.UpdatedListState)
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
                val textArray = p1.asBytes()?.toString(Charset.defaultCharset())
                    ?.split(";")?.toTypedArray()
                discovererPhoneNumber = textArray?.get(1)
                state.set(newValue = LookState.SucceededAdvertiserIsFoundState(user = prefs.getUserProfile()))
            }
        }
    }

    private fun isGenderValid(advertiserGender: @UserGender.AnnotationUserGender String?): Boolean {
        when (prefs.getUserProfile().lookGender) {
            MALE -> {
                isGenderValid = advertiserGender == MALE
                return isGenderValid
            }
            FEMALE -> {
                isGenderValid = advertiserGender == FEMALE
                return isGenderValid
            }
            ALL -> {
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
                    prefs.saveContact(name = advertiserName!!, phoneNumber = phoneNumber)
                }
            }
            UserRole.ADVERTISER -> {
                if (phoneNumber != null && discovererName != null) {
                    prefs.saveContact(name = discovererName!!, phoneNumber = phoneNumber)
                }
            }
        }
    }

    fun isIntentValid(): Boolean {
        return notificationHandler?.isNotificationValid() ?: false
    }

    fun handleNewIntent(intent: Intent?) {
        Log.v("Look4", "Look model: handleNewIntent")

        prefs.saveContactRequest(ContactRequest())

        if (intent == null || intent.extras == null) {
            return
        }
        notificationHandler = NotificationHandler(intent)

        if (notificationHandler?.isNotificationValid() == true) {
            Log.v("Look4", "Look model: notificationHandler?.isNotificationValid() == true : $notificationHandler")
            discovererFilePath = notificationHandler?.getDiscovererFilePath()
            discovererName = notificationHandler?.getDiscovererName()
            discovererPhoneNumber = notificationHandler?.getDiscovererPhoneNumber()
            endpointIdSaved = notificationHandler?.getEndpointId()
            state.set(newValue = LookState.SucceededDiscoverIsFoundState(user = prefs.getUserProfile()))
        } else {
            Log.v("Look4", "Look model: notificationHandler?.isNotificationValid() == false")
            state.set(newValue = LookState.DefaultState)
        }
//        else if (LookActivity.advertiserPhoneNumber != null) {
//            populateDefaultView()
//            searchingInProgressText.text =
//                LookActivity.advertiserPhoneNumber
//            searchingInProgressText.visibility = View.VISIBLE
//        }
    }

    fun showWarningIfPending(): Boolean {
        if (state.value == LookState.PendingState) {
            action.set(LookAction.ShowDialog)
            return true
        }
        return false
    }

    fun hasPermissionsGranted(permissions: Array<String>): Boolean {
        return permissionChecker.hasPermissionsGranted(permissions)
    }
}
