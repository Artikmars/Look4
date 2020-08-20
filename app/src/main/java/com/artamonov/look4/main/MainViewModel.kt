package com.artamonov.look4.main

import androidx.hilt.lifecycle.ViewModelInject
import com.artamonov.look4.base.BaseVM
import com.artamonov.look4.data.database.ContactRequest
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.main.models.FetchMainStatus
import com.artamonov.look4.main.models.MainAction
import com.artamonov.look4.main.models.MainEvent
import com.artamonov.look4.main.models.MainViewState
import com.artamonov.look4.service.ForegroundService
import com.artamonov.look4.utils.UserGender.Companion.ALL
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE

class MainViewModel @ViewModelInject constructor(
    private val prefs: PreferenceHelper
) : BaseVM<MainViewState, MainAction, MainEvent>() {

    override fun obtainEvent(viewEvent: MainEvent) {
        when (viewEvent) {
            MainEvent.DiscoveringIsStarted -> {
                viewState = viewState.copy(fetchStatus = FetchMainStatus.OnLookClickedState)
            }
            MainEvent.ChangeStatus -> {
                changeAdvertisingStatus()
            }
            MainEvent.OpenContacts -> {
                viewState = viewState.copy(fetchStatus = FetchMainStatus.OnContactsClickedState)
            }
            MainEvent.OpenSettings -> {
                viewState = viewState.copy(fetchStatus = FetchMainStatus.OnSettingsClickedState)
            }
            MainEvent.ChangeLookGender -> {
                changeLookGenderText()
            }
            MainEvent.SendEmail -> {
                viewAction = MainAction.SendEmail
            }
        }
    }

    init {
        viewState = MainViewState(
            fetchStatus = FetchMainStatus.DefaultState,
            data = prefs.getUserProfile(), contactRequest = ContactRequest()
        )
    }

    private fun changeAdvertisingStatus() {
        viewState = when (ForegroundService.isForegroundServiceRunning) {
            true -> viewState.copy(fetchStatus = FetchMainStatus.OfflineState)
            false -> viewState.copy(fetchStatus = FetchMainStatus.OnlineState)
        }
    }

    private fun changeLookGenderText() {
        when (viewState.data?.lookGender) {
            MALE -> {
                if (prefs.updateLookGender(FEMALE)) {
                    viewState = viewState.copy(
                        data = prefs.getUserProfile(),
                        fetchStatus = FetchMainStatus.LookGenderWomenState
                    )
                }
            }
            FEMALE -> {
                if (prefs.updateLookGender(ALL)) {
                    viewState = viewState.copy(
                        data = prefs.getUserProfile(),
                        fetchStatus = FetchMainStatus.LookGenderAllState
                    )
                }
            }
            ALL -> {
                if (prefs.updateLookGender(MALE)) {
                    viewState = viewState.copy(
                        data = prefs.getUserProfile(),
                        fetchStatus = FetchMainStatus.LookGenderManState
                    )
                }
            }
        }
    }
}
