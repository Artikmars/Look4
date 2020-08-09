package com.artamonov.look4.main.models

sealed class MainEvent {
    object ScreenShown : MainEvent()
    object DiscoveringIsStarted : MainEvent()
    object ChangeStatus : MainEvent()
    object OpenContacts : MainEvent()
    object OpenSettings : MainEvent()
    object ChangeLookGender : MainEvent()
    object SendEmail : MainEvent()
    object NoButtonClicked : MainEvent()
    object YesButtonClicked : MainEvent()
    object GoBackClicked : MainEvent()
    object ChangeNameClicked : MainEvent()
    object ChangePhoneNumber : MainEvent()
    object ProfilePhotoClicked : MainEvent()
    object GenderClicked : MainEvent()
    object SaveClicked : MainEvent()
    object NoSearchResultFound : MainEvent()
}
