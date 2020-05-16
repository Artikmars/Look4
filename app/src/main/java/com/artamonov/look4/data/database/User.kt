package com.artamonov.look4.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.artamonov.look4.utils.UserRole

@Entity(tableName = "User")
data class User (
    @PrimaryKey
    val creationDate: Long,
    var phoneNumber: String,
    var name: String,
    val gender: String? = null,
    var imagePath: String? = null,
    var role: @UserRole.AnnotationUserRole String? = null
)