package com.artamonov.look4.data.database

data class ContactRequest(
    var discovererName: String?,
    var discovererPhoneNumber: String?,
    var discovererFilePath: String?,
    var advertiserPhoneNumber: String?,
    var endpointIdSaved: String?
)
