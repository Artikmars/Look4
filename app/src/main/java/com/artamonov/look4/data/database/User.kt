package com.artamonov.look4.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "User")
data class User (
    @PrimaryKey
    val creationDate: Long,
    val phoneNumber: String,
    val name: String,
    val gender: String? = null,
    val imagePath: String? = null
)