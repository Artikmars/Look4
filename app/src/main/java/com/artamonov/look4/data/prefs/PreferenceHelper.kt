package com.artamonov.look4.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.artamonov.look4.data.database.User
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE
import com.artamonov.look4.utils.UserRole
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.koin.core.KoinComponent
import java.lang.reflect.Type

class PreferenceHelper(private var context: Context) : KoinComponent {

    companion object {
        const val CONTACT_LIST = "CONTACT_LIST"
        const val USER_PROFILE = "USER_PROFILE"
    }

    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun getSharedEditor(): SharedPreferences.Editor {
        return prefs.edit()
    }

    fun getUserProfile(): User? {
        val profile = prefs.getString(USER_PROFILE, null)
        return try {
            Gson().fromJson(profile, User::class.java)
        } catch (jse: JsonSyntaxException) {
            jse.printStackTrace()
            null
        }
    }

    fun getContactList(): ArrayList<User>? {
        val type: Type = object : TypeToken<List<User?>?>() {}.type
        return Gson().fromJson(prefs.getString(CONTACT_LIST, null), type)
    }

    fun updateUserProfile(name: String?, phoneNumber: String?, gender: @UserGender.AnnotationUserGender String?, imagePath: String?): Boolean {
        val profile = getUserProfile()
        name?.let { profile?.name = name }
        phoneNumber?.let { profile?.phoneNumber = phoneNumber }
        imagePath?.let { profile?.imagePath = imagePath }
        gender?.let { profile?.gender = gender }
        return getSharedEditor().putString(USER_PROFILE, Gson().toJson(profile)).commit()
    }

    fun createUserProfile(name: String, phoneNumber: String, imagePath: String, gender: @UserGender.AnnotationUserGender String): Boolean {
        lateinit var profile: User
        when (gender) {
            MALE -> {
                profile = User(creationDate = System.currentTimeMillis(), name = name,
            phoneNumber = phoneNumber, gender = gender, lookGender = FEMALE, imagePath = imagePath) }
            FEMALE -> {
                profile = User(creationDate = System.currentTimeMillis(), name = name,
                phoneNumber = phoneNumber, gender = gender, lookGender = MALE, imagePath = imagePath
                ) }
        }

        return getSharedEditor().putString(USER_PROFILE, Gson().toJson(profile)).commit()
    }

    fun saveContact(name: String, phoneNumber: String): Boolean {
        var contactList = getContactList()
        val newContact = User(creationDate = System.currentTimeMillis(), name = name, phoneNumber = phoneNumber)
        if (getContactList()?.size == null) { contactList = arrayListOf() }
        contactList?.add(newContact)
        return getSharedEditor().putString(CONTACT_LIST, Gson().toJson(contactList)).commit()
    }

    fun updateRole(role: @UserRole.AnnotationUserRole String): Boolean {
        val user = getUserProfile()
        user?.role = role
        return getSharedEditor().putString(USER_PROFILE, Gson().toJson(user)).commit()
    }

    fun updateLookGender(lookGender: @UserRole.AnnotationUserRole String): Boolean {
        val user = getUserProfile()
        user?.lookGender = lookGender
        return getSharedEditor().putString(USER_PROFILE, Gson().toJson(user)).commit()
    }

    fun userAvailable(): Boolean {
        return getUserProfile() != null
    }

    fun deleteContactItemFromDB(position: Int): Boolean {
        val contacts = getContactList()
        contacts?.removeAt(position)
        val json = Gson().toJson(contacts)
        return getSharedEditor().putString(CONTACT_LIST, json).commit()
    }
    }
