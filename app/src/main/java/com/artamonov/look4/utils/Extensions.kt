package com.artamonov.look4.utils

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.artamonov.look4.AboutUsActivity
import com.artamonov.look4.R
import com.artamonov.look4.WebViewActivity
import com.artamonov.look4.contacts.ContactsActivity
import com.artamonov.look4.look.LookActivity
import com.artamonov.look4.main.MainActivity
import com.artamonov.look4.settings.SettingsActivity
import com.artamonov.look4.userprofiledit.UserProfileEditActivity
import com.google.android.gms.nearby.connection.ConnectionsClient
import java.util.regex.Pattern
import kotlinx.android.synthetic.main.activity_main.*

private val PHONE_NUMBER = Pattern.compile("""^\+?(?:[0-9] ?){6,14}[0-9]${'$'}""")

fun String?.isValidPhoneNumber() = if (this != null) PHONE_NUMBER.matcher(this).matches() else false

fun String?.disconnectFromEndpoint(connectionClient: ConnectionsClient) = this?.let {
    connectionClient.disconnectFromEndpoint(it)
}

fun Activity.startUserProfileEditActivity() =
    startActivity(Intent(this, UserProfileEditActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())

fun Activity.startAboutUsActivity() =
    startActivity(Intent(this, AboutUsActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())

fun Activity.startSettingsActivity() =
    startActivity(Intent(this, SettingsActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())

fun Activity.startWebViewActivity() =
    startActivity(Intent(this, WebViewActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())

fun Activity.startLookActivity() =
    startActivity(Intent(this, LookActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())

fun Activity.startContactsActivity() =
    startActivity(Intent(this, ContactsActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())

fun Activity.startMainActivity() =
    startActivity(Intent(this, MainActivity::class.java))

fun AppCompatActivity.blockInput() {
    window.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
}

fun AppCompatActivity.colourMainButtonsToGrey() {
    look_text.setTextColor(ContextCompat.getColor(this, R.color.lightGrey))
    offline_text.setTextColor(ContextCompat.getColor(this, R.color.lightGrey))
    contacts_text.setTextColor(ContextCompat.getColor(this, R.color.lightGrey))
    settings_text.setTextColor(ContextCompat.getColor(this, R.color.lightGrey))
    main_look_gender_text.setTextColor(ContextCompat.getColor(this, R.color.lightGrey))
}

fun AppCompatActivity.colourMainButtonsToNormal() {
    look_text.setTextColor(ContextCompat.getColor(this, R.color.black))
    offline_text.setTextColor(ContextCompat.getColor(this, R.color.black))
    contacts_text.setTextColor(ContextCompat.getColor(this, R.color.black))
    settings_text.setTextColor(ContextCompat.getColor(this, R.color.black))
    main_look_gender_text.setTextColor(ContextCompat.getColor(this, R.color.black))
}

fun AppCompatActivity.unblockInput() {
    window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
}

fun AppCompatActivity.blockInputForTask(task: () -> Unit) {
    blockInput()
    task.invoke()
    unblockInput()
}

fun Int.setVisibility(state: Boolean?) = if (state != null && state) View.VISIBLE else View.GONE
