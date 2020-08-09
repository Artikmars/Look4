package com.artamonov.look4.main.models

import android.net.Uri
import com.artamonov.look4.utils.UserGender

sealed class MainAction {
    data class ShowSnackbar(val stringResourceId: Int) : MainAction()
    data class UpdateImage(val uri: Uri) : MainAction()
    object CloseActivity : MainAction()
    object SendEmail : MainAction()
    data class LoadingState(val state: Boolean) : MainAction()
    object StartCountDown : MainAction()
    data class PopulateCurrentProfileData(
        val name: String?,
        val phoneNumber: String?,
        val imagePath: String?,
        val gender: @UserGender.AnnotationUserGender String?
    ) :
        MainAction()
}
