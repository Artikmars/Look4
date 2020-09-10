package com.artamonov.look4.data.database

import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.UserRole

data class User(
    val creationDate: Long? = null,
    val phoneNumber: String? = null,
    val name: String? = null,
    val gender: @UserGender.AnnotationUserGender String? = null,
    val lookGender: @UserGender.AnnotationUserGender String? = null,
    val imagePath: String? = null,
    val role: @UserRole.AnnotationUserRole String? = null,
    val unseenContactCounter: Int = 0
)
