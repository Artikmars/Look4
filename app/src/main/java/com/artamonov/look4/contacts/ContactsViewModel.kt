package com.artamonov.look4.contacts

import androidx.lifecycle.MutableLiveData
import com.artamonov.look4.base.BaseViewModel
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.ContactsState
import com.artamonov.look4.utils.LiveDataContactListState.contactListState
import com.artamonov.look4.utils.set
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val prefs: PreferenceHelper
) : BaseViewModel() {

    private val contacts: MutableLiveData<List<User>> = MutableLiveData()

    fun initList() {
        if (getContactList().isNullOrEmpty()) {
            contactListState.set(newValue = ContactsState.NoContactsState)
        } else {
            contacts.set(newValue = getContactList())
            contactListState.set(newValue = ContactsState.LoadedState)
        }
    }

    fun updateList() {
        if (getContactList().isNullOrEmpty()) {
            contactListState.set(newValue = ContactsState.NoContactsState)
        } else {
            contacts.set(newValue = getContactList())
            contactListState.set(newValue = ContactsState.UpdatedListState)
        }
    }

    fun getContactList(): List<User> {
        return prefs.getContactList().distinctBy { it.phoneNumber }
    }
}
