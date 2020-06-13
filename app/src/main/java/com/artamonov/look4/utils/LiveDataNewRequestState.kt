package com.artamonov.look4.utils

import androidx.lifecycle.MutableLiveData

sealed class NewRequestState {
    object EnabledState : NewRequestState()
    object DisabledState : NewRequestState()
}

object LiveDataNewRequestState {
    val newRequestState = MutableLiveData<NewRequestState>().default(initialValue = NewRequestState.DisabledState)
}
