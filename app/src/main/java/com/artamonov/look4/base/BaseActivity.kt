package com.artamonov.look4.base

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

abstract class BaseActivity : AppCompatActivity() {

    fun showSnackbarError(stringMsg: String) { Snackbar.make(findViewById(android.R.id.content), stringMsg, Snackbar.LENGTH_LONG).show() }
}