package com.artamonov.look4.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.artamonov.look4.helpers.SingleLiveAction

abstract class BaseVM<S, A, E> : ViewModel() {

    private val TAG = BaseVM::class.java.simpleName
    private val _viewStates: MutableLiveData<S> = MutableLiveData()
    fun viewStates(): LiveData<S> = _viewStates

    private var _viewState: S? = null
    protected var viewState: S
        get() = _viewState
            ?: throw UninitializedPropertyAccessException("\"viewState\" was queried before being initialized")
        set(value) {
            _viewState = value
            _viewStates.postValue(value)
        }

    private val _viewActions: SingleLiveAction<A> = SingleLiveAction()
    fun viewEffects(): SingleLiveAction<A> = _viewActions

    private var _viewAction: A? = null
    protected var viewAction: A
        get() = _viewAction
            ?: throw UninitializedPropertyAccessException("\"viewAction\" was queried before being initialized")
        set(value) {
            _viewAction = value
            _viewActions.postValue(value)
        }

    abstract fun obtainEvent(viewEvent: E)
}
