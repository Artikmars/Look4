package com.artamonov.look4.utils

import android.view.View
import java.util.regex.Pattern

private val PHONE_NUMBER = Pattern.compile("""^\+?(?:[0-9] ?){6,14}[0-9]${'$'}""")

fun String.isValidPhoneNumber() = PHONE_NUMBER.matcher(this).matches()

fun Int.setVisibility(state: Boolean?) = if (state != null && state) View.VISIBLE else View.GONE
