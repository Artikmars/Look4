package com.artamonov.look4.utils

import androidx.lifecycle.MutableLiveData

sealed class ContactsState {
    object DefaultState : ContactsState()
    object LoadedState : ContactsState()

    /***
     UpdateListState is used to update the contact list if discoverer stays in contact view and have
     received new contact.
     */
    object UpdatedListState : ContactsState()
    object NoContactsState : ContactsState()
}

object LiveDataContactListState {
    val contactListState = MutableLiveData<ContactsState>().default(initialValue = ContactsState.DefaultState)
}
