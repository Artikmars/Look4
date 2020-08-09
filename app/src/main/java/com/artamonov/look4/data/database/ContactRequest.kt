package com.artamonov.look4.data.database

import java.io.File

data class ContactRequest(
    val name: String? = null,
    val phoneNumber: String? = null,
    val advertiserPhoneNumber: String? = null,
    val filePath: String? = null,
    val image: File? = null,
    val endpointId: String? = null
)
