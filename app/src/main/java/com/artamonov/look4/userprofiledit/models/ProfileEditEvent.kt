package com.artamonov.look4.userprofiledit.models

sealed class ProfileEditEvent {
    object ScreenShown : ProfileEditEvent()
    object CurrentProfileDataLoaded : ProfileEditEvent()
    object ChangeNameClicked : ProfileEditEvent()
    object ChangePhoneNumber : ProfileEditEvent()
    object ProfilePhotoClicked : ProfileEditEvent()
    object GenderClicked : ProfileEditEvent()
    object SaveClicked : ProfileEditEvent()
}
