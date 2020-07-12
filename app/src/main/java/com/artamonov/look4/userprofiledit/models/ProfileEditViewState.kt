package com.artamonov.look4.userprofiledit.models
import com.artamonov.look4.data.database.User

data class ProfileEditViewState(
    val fetchStatus: FetchStatus,
    val data: User?
)

sealed class FetchStatus {
    object DefaultState : FetchStatus()
    object LoadingState : FetchStatus()
    object SucceededState : FetchStatus()
    object PhoneValidationErrorState : FetchStatus()
    object ProfileWasNotUpdatedErrorState : FetchStatus()
}
