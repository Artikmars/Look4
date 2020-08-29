package com.artamonov.look4.data.prefs

import android.content.SharedPreferences
import com.artamonov.look4.data.database.ContactRequest
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelperImpl.Consts.CONTACT_LIST
import com.artamonov.look4.data.prefs.PreferenceHelperImpl.Consts.CONTACT_REQUEST
import com.artamonov.look4.data.prefs.PreferenceHelperImpl.Consts.USER_PROFILE
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE
import com.artamonov.look4.utils.UserRole
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import javax.inject.Inject

class PreferenceHelperImpl
@Inject constructor(private val prefs: SharedPreferences) : PreferenceHelper {

    object Consts {
        const val CONTACT_LIST = "CONTACT_LIST"
        const val USER_PROFILE = "USER_PROFILE"
        const val CONTACT_REQUEST = "CONTACT_REQUEST"
    }

    override fun getSharedEditor(): SharedPreferences.Editor {
        return prefs.edit()
    }

    override fun getUserProfile(): User {
        val profile = prefs.getString(USER_PROFILE, null)
        return try {
            Gson().fromJson(profile, User::class.java)
        } catch (jse: JsonSyntaxException) {
            jse.printStackTrace()
            User()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            User()
        }
    }

    override fun getContactList(): ArrayList<User> {
        val type: Type = object : TypeToken<List<User?>?>() {}.type
        return try {
            Gson().fromJson(prefs.getString(CONTACT_LIST, null), type)
        } catch (jse: JsonSyntaxException) {
            jse.printStackTrace()
            arrayListOf()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            arrayListOf()
        }
    }

    override fun updateUserProfile(
        name: String?,
        phoneNumber: String?,
        gender: @UserGender.AnnotationUserGender String?,
        imagePath: String?
    ): Boolean {
        val profile = getUserProfile()
        name?.let { profile.name = name }
        phoneNumber?.let { profile.phoneNumber = phoneNumber }
        imagePath?.let { profile.imagePath = imagePath }
        gender?.let { profile.gender = gender }
        return getSharedEditor().putString(USER_PROFILE, Gson().toJson(profile)).commit()
    }

    override fun createUserProfile(
        name: String,
        phoneNumber: String,
        imagePath: String,
        gender: @UserGender.AnnotationUserGender String
    ): Boolean {
        lateinit var profile: User
        when (gender) {
            MALE -> {
                profile = User(
                    creationDate = System.currentTimeMillis(),
                    name = name,
                    phoneNumber = phoneNumber,
                    gender = gender,
                    lookGender = FEMALE,
                    imagePath = imagePath
                )
            }
            FEMALE -> {
                profile = User(
                    creationDate = System.currentTimeMillis(),
                    name = name,
                    phoneNumber = phoneNumber,
                    gender = gender,
                    lookGender = MALE,
                    imagePath = imagePath
                )
            }
        }

        return getSharedEditor().putString(USER_PROFILE, Gson().toJson(profile)).commit()
    }

    override fun saveContact(name: String, phoneNumber: String): Boolean {
        var contactList: ArrayList<User> = getContactList()
        val newContact =
            User(creationDate = System.currentTimeMillis(), name = name, phoneNumber = phoneNumber)
        if (getContactList().isEmpty()) {
            contactList = arrayListOf()
        }
        contactList.add(newContact)
        return getSharedEditor().putString(CONTACT_LIST, Gson().toJson(contactList)).commit()
    }

    override fun updateRole(role: @UserRole.AnnotationUserRole String): Boolean {
        val user = getUserProfile()
        user.role = role
        return getSharedEditor().putString(USER_PROFILE, Gson().toJson(user)).commit()
    }

    override fun updateLookGender(lookGender: @UserRole.AnnotationUserRole String): Boolean {
        val user = getUserProfile()
        user.lookGender = lookGender
        return getSharedEditor().putString(USER_PROFILE, Gson().toJson(user)).commit()
    }

    override fun userAvailable(): Boolean {
        return getUserProfile().name != null && getUserProfile().phoneNumber != null
    }

    override fun deleteContactItemFromDB(position: Int): Boolean {
        val contacts: ArrayList<User> = getContactList()
        contacts.removeAt(position)
        val json = Gson().toJson(contacts)
        return getSharedEditor().putString(CONTACT_LIST, json).commit()
    }

    override fun saveContactRequest(contactRequest: ContactRequest): Boolean {
        return getSharedEditor().putString(CONTACT_REQUEST, Gson().toJson(contactRequest)).commit()
    }

    override fun getContactRequest(): ContactRequest {
        val contactRequest = prefs.getString(CONTACT_REQUEST, null)
        return try {
            Gson().fromJson(contactRequest, ContactRequest::class.java)
        } catch (jse: JsonSyntaxException) {
            jse.printStackTrace()
            ContactRequest()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            ContactRequest()
        }
    }
}