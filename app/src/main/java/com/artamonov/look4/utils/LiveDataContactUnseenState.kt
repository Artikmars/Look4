package com.artamonov.look4.utils

import androidx.lifecycle.MutableLiveData

sealed class ContactUnseenState {
    object EnabledState : ContactUnseenState()
    object DisabledState : ContactUnseenState()
}

object LiveDataContactUnseenState {
    val contactDiscovererUnseenState = MutableLiveData<ContactUnseenState>().default(initialValue = ContactUnseenState.DisabledState)
    val contactAdvertiserUnseenState = MutableLiveData<ContactUnseenState>().default(initialValue = ContactUnseenState.DisabledState)
}
