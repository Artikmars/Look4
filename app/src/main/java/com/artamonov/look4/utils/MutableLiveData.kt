package com.artamonov.look4.utils

import androidx.lifecycle.MutableLiveData

// Set default value for any type of MutableLiveData
fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }

// Set new value for any type of MutableLiveData
fun <T> MutableLiveData<T>.set(newValue: T) = apply { setValue(newValue) }
