package com.artamonov.look4.main

import androidx.lifecycle.MutableLiveData
import com.artamonov.look4.base.BaseViewModel
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.service.ForegroundService
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.default
import com.artamonov.look4.utils.set

sealed class MainState {
    object DefaultState : MainState()
    object LoadingState : MainState()
    class SucceededState<User>(val user: User) : MainState()
    object OnLookClickedState : MainState()
    object OnContactsClickedState : MainState()
    object OnSettingsClickedState : MainState()
    object LookGenderManState : MainState()
    object LookGenderWomenState : MainState()
    object LookGenderAllState : MainState()
    object OnlineState : MainState()
    object ErrorState : MainState()
}

class MainViewModel : BaseViewModel() {

    val state = MutableLiveData<MainState>().default(initialValue = MainState.DefaultState)
    var isInForeground: MutableLiveData<Boolean> = MutableLiveData()

    fun populateData() {
        state.set(newValue = MainState.SucceededState(user = PreferenceHelper.getUserProfile()))
    }

    fun changeAdvertisingStatus() {
        when (ForegroundService.isAppInForeground) {
            true -> state.set(newValue = MainState.DefaultState)
            false -> state.set(newValue = MainState.OnlineState)
        }
    }

    fun isInForeground() { isInForeground.set(newValue = ForegroundService.isAppInForeground) }

    fun changeLookGenderText() {
        state.set(newValue = MainState.LoadingState)
        when (PreferenceHelper.getUserProfile()?.lookGender) {
            UserGender.MALE -> {
                val isUpdated = PreferenceHelper.updateLookGender(UserGender.FEMALE)
                if (isUpdated) state.set(newValue = MainState.LookGenderWomenState) else {
                    state.set(newValue = MainState.ErrorState)
                }
            }
            UserGender.FEMALE -> {
                val isUpdated = PreferenceHelper.updateLookGender(UserGender.ALL)
                if (isUpdated) state.set(newValue = MainState.LookGenderAllState) else {
                    state.set(newValue = MainState.ErrorState)
                }
            }
            UserGender.ALL -> {
                val isUpdated = PreferenceHelper.updateLookGender(UserGender.MALE)
                if (isUpdated) state.set(newValue = MainState.LookGenderManState) else {
                    state.set(newValue = MainState.ErrorState)
                }
            }
        }
    }

    fun startDiscovering() { state.set(newValue = MainState.OnLookClickedState) }

    fun openContacts() { state.set(newValue = MainState.OnContactsClickedState) }

    fun openSettings() { state.set(newValue = MainState.OnSettingsClickedState) }
}
