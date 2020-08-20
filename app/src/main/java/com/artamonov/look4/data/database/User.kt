package com.artamonov.look4.data.database

import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.UserRole

data class User(
    val creationDate: Long? = null,
    var phoneNumber: String? = null,
    var name: String? = null,
    var gender: @UserGender.AnnotationUserGender String? = null,
    var lookGender: @UserGender.AnnotationUserGender String? = null,
    var imagePath: String? = null,
    var role: @UserRole.AnnotationUserRole String? = null,
    var unseenContactCounter: Int = 0
)
