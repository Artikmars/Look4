package com.artamonov.look4.data.prefs

import android.content.SharedPreferences
import com.artamonov.look4.data.database.ContactRequest
import com.artamonov.look4.data.database.User
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.UserRole

interface PreferenceHelper {

    fun createUserProfile(
        name: String,
        phoneNumber: String,
        imagePath: String,
        gender: @UserGender.AnnotationUserGender String
    ): Boolean

    fun deleteContactItemFromDB(position: Int): Boolean

    fun getContactList(): ArrayList<User>

    fun getContactRequest(): ContactRequest

    fun getSharedEditor(): SharedPreferences.Editor

    fun getUserProfile(): User

    fun saveContact(name: String, phoneNumber: String): Boolean

    fun saveContactRequest(contactRequest: ContactRequest): Boolean

    fun updateLookGender(lookGender: @UserRole.AnnotationUserRole String): Boolean

    fun updateRole(role: @UserRole.AnnotationUserRole String): Boolean

    fun updateUserProfile(user: User): Boolean

    fun userAvailable(): Boolean
}
