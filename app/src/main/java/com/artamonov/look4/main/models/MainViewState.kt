package com.artamonov.look4.main.models
import com.artamonov.look4.data.database.ContactRequest
import com.artamonov.look4.data.database.User

data class MainViewState(
    val fetchStatus: FetchMainStatus,
    val data: User?,
    val contactRequest: ContactRequest?
)

sealed class FetchMainStatus {
    object DefaultState : FetchMainStatus()
    object LoadingState : FetchMainStatus()
    class SucceededState<User>(val user: User) : FetchMainStatus()
    object OnLookClickedState : FetchMainStatus()
    object OnContactsClickedState : FetchMainStatus()
    object OnSettingsClickedState : FetchMainStatus()
    object LookGenderManState : FetchMainStatus()
    object LookGenderWomenState : FetchMainStatus()
    object LookGenderAllState : FetchMainStatus()
    object OnlineEnabledState : FetchMainStatus()
    object OfflineEnabledState : FetchMainStatus()
    object EnablingOnlineState : FetchMainStatus()
    object EnablingOfflineState : FetchMainStatus()
    object ErrorState : FetchMainStatus()
}
