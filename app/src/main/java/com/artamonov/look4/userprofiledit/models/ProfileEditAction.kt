package com.artamonov.look4.userprofiledit.models

import android.net.Uri
import com.artamonov.look4.utils.UserGender

sealed class ProfileEditAction {
    data class ShowSnackbar(val message: String) : ProfileEditAction()
    data class UpdateImage(val uri: Uri) : ProfileEditAction()
    data class PopulateCurrentProfileData(
        val name: String?,
        val phoneNumber: String?,
        val imagePath: String?,
        val gender: @UserGender.AnnotationUserGender String?
    ) :
        ProfileEditAction()
}
