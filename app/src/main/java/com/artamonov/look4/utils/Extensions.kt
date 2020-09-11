package com.artamonov.look4.utils

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.view.View
import com.artamonov.look4.look.LookActivity
import com.artamonov.look4.userprofiledit.UserProfileEditActivity
import com.google.android.gms.nearby.connection.ConnectionsClient
import java.util.regex.Pattern

private val PHONE_NUMBER = Pattern.compile("""^\+?(?:[0-9] ?){6,14}[0-9]${'$'}""")

fun String?.isValidPhoneNumber() = if (this != null) PHONE_NUMBER.matcher(this).matches() else false

fun String?.disconnectFromEndpoint(connectionClient: ConnectionsClient) = this?.let {
    connectionClient.disconnectFromEndpoint(it)
}

fun Activity.startUserProfileEditActivity() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    startActivity(Intent(this, UserProfileEditActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
} else {
    startActivity(Intent(this, UserProfileEditActivity::class.java))
}

fun Activity.startAboutUsActivity() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    startActivity(Intent(this, UserProfileEditActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
} else {
    startActivity(Intent(this, UserProfileEditActivity::class.java))
}

fun Activity.startWebViewActivity() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    startActivity(Intent(this, UserProfileEditActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
} else {
    startActivity(Intent(this, UserProfileEditActivity::class.java))
}

fun Activity.startLookActivity() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    startActivity(Intent(this, LookActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
} else {
    startActivity(Intent(this, LookActivity::class.java))
}

fun Int.setVisibility(state: Boolean?) = if (state != null && state) View.VISIBLE else View.GONE
