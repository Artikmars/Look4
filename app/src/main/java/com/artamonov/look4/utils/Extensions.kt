package com.artamonov.look4.utils

import android.app.Activity
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.artamonov.look4.AboutUsActivity
import com.artamonov.look4.BuildConfig
import com.artamonov.look4.R
import com.artamonov.look4.WebViewActivity
import com.artamonov.look4.WelcomeActivity
import com.artamonov.look4.contacts.ContactsActivity
import com.artamonov.look4.databinding.ActivityMainBinding
import com.artamonov.look4.look.LookActivity
import com.artamonov.look4.main.MainActivity
import com.artamonov.look4.service.ForegroundService
import com.artamonov.look4.service.ForegroundService.Companion.ADVERTISING_FAILED_EVENT
import com.artamonov.look4.service.ForegroundService.Companion.ADVERTISING_SUCCEEDED_EVENT
import com.artamonov.look4.service.ForegroundService.Companion.SERVICE_IS_DESTROYED
import com.artamonov.look4.settings.SettingsActivity
import com.artamonov.look4.userprofiledit.UserProfileEditActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import java.util.regex.Pattern

private val PHONE_NUMBER = Pattern.compile("""^\+?(?:[0-9] ?){6,14}[0-9]${'$'}""")
const val FAQ_TYPE = "FAQ_TYPE"

fun String?.isValidPhoneNumber() = if (this != null) PHONE_NUMBER.matcher(this).matches() else false

fun String?.disconnectFromEndpoint(connectionClient: ConnectionsClient) = this?.let {
    connectionClient.disconnectFromEndpoint(it)
}

fun Activity.startUserProfileEditActivity() =
    startActivity(
        Intent(this, UserProfileEditActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
    )

fun Activity.startAboutUsActivity() =
    startActivity(
        Intent(this, AboutUsActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
    )

fun Activity.startSettingsActivity() =
    startActivity(
        Intent(this, SettingsActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
    )

fun Activity.startWelcomeActivity() =
    startActivity(
        Intent(this, WelcomeActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
    )

fun Activity.startWebViewActivity(faqType: String) {
    val intent = Intent(this, WebViewActivity::class.java)
    intent.putExtra(FAQ_TYPE, faqType)
    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
}

fun Activity.startLookActivity() =
    startActivity(
        Intent(this, LookActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
    )

fun Activity.startContactsActivity() =
    startActivity(
        Intent(this, ContactsActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
    )

fun Activity.startMainActivity() =
    startActivity(Intent(this, MainActivity::class.java),
        ActivityOptions.makeSceneTransitionAnimation(this).toBundle())

fun AppCompatActivity.blockInput(binding: ActivityMainBinding) {
    window.setFlags(FLAG_NOT_TOUCHABLE, FLAG_NOT_TOUCHABLE)
    colourMainButtons(binding, R.color.lightGrey)
}

fun AppCompatActivity.colourMainButtons(binding: ActivityMainBinding, color: Int) {
    binding.offlineText.setTextColor(ContextCompat.getColor(this, color))
    binding.contactsText.setTextColor(ContextCompat.getColor(this, color))
    binding.settingsText.setTextColor(ContextCompat.getColor(this, color))
    binding.mainLookGenderText.setTextColor(ContextCompat.getColor(this, color))
}

fun AppCompatActivity.unblockInput(binding: ActivityMainBinding) {
    window.clearFlags(FLAG_NOT_TOUCHABLE)
    colourMainButtons(binding, R.color.black)
}

fun AppCompatActivity.updateMainUIState(binding: ActivityMainBinding) {
    unblockInput(binding)
    colourMainButtons(binding, R.color.black)
    if (ForegroundService.isForegroundServiceRunning) {
        binding.offlineText.text = getString(R.string.main_online_mode)
    } else {
        binding.offlineText.text = getString(R.string.main_offline_mode)
    }
}

fun ActivityMainBinding.setAdView() = this.adView.loadAd(AdRequest.Builder().build())

fun ImageView.animateDot(context: Context) {
    this.startAnimation(LookRotateAnimation(context).init())
}

fun AppCompatActivity.keepScreenOn() = window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

fun AppCompatActivity.unKeepScreenOn() = window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

fun AppCompatActivity.blockInputForTask(task: () -> Unit,  binding: ActivityMainBinding) {
    blockInput(binding)
    task.invoke()
    unblockInput(binding)
}

fun AppCompatActivity.startService(binding: ActivityMainBinding) {
    if (ForegroundService.isForegroundServiceRunning) { return }
    val serviceIntent = Intent(this, ForegroundService::class.java)
    serviceIntent.putExtra("inputExtra", getString(R.string.main_advertising_title))
    ContextCompat.startForegroundService(this, serviceIntent)
    blockInput(binding)
}

fun AppCompatActivity.stopService(binding: ActivityMainBinding) {
    if (!ForegroundService.isForegroundServiceRunning) { return }
    stopService(Intent(this, ForegroundService::class.java))
    blockInput(binding)
}

fun AppCompatActivity.showSnackbarError(resourceId: Int) {
    Snackbar.make(
        findViewById(android.R.id.content),
        getString(resourceId), Snackbar.LENGTH_LONG
    ).show()
}

fun AppCompatActivity.getSnackbar(resourceId: Int): Snackbar {
    return Snackbar.make(
        findViewById(android.R.id.content),
        getString(resourceId), Snackbar.LENGTH_LONG
    )
}

fun AppCompatActivity.showSnackbarError(stringMsg: String?) {
    stringMsg?.let { Snackbar.make(findViewById(android.R.id.content), stringMsg, LENGTH_LONG)
        .show() } }

fun AppCompatActivity.showSnackbarWithAction() {
    val snackbar = Snackbar.make(
        findViewById(android.R.id.content),
        getString(R.string.look_you_received_phone_number), LENGTH_LONG
    )
        .setAction(getString(R.string.look_view)) { startContactsActivity() }
    snackbar.show()
}

fun Context.getMissingInternetSnackbar(snackbar: Snackbar?): Snackbar? {
    snackbar?.let {
        val view = snackbar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        view.layoutParams = params
        view.background = ContextCompat.getDrawable(this, R.drawable.snackbar_custom)
        snackbar.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
        val mainTextView =
            view.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
        mainTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            R.drawable.ic_no_connection, 0, 0, 0
        )
        mainTextView.compoundDrawablePadding = 20
        val font = ResourcesCompat.getFont(this, R.font.semibold)
        mainTextView.typeface = font
        return snackbar
    } ?: return null
}

fun AppCompatActivity.registerBroadcastReceiver(mMessageReceiver: BroadcastReceiver) {
    val filter = IntentFilter(ADVERTISING_FAILED_EVENT)
    filter.addAction(ADVERTISING_SUCCEEDED_EVENT)
    filter.addAction(SERVICE_IS_DESTROYED)
    LocalBroadcastManager.getInstance(this).registerReceiver(
        mMessageReceiver, IntentFilter(filter)
    )
}

fun AppCompatActivity.unregisterBroadcastReceiver(mMessageReceiver: BroadcastReceiver) {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
}

fun Context.isNetworkNotAvailable(): Boolean {
    val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return true
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return true
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> false
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> false
            // for other device how are able to connect with Ethernet
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> false
            // for check internet over Bluetooth
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> false
            else -> true
        }
}

fun Context.sendEmail() {
    val emailIntent = Intent(
        Intent.ACTION_SENDTO,
        Uri.fromParts("mailto", "artamonov06@gmail.com", null)
    )
    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Look4 - logs")

    emailIntent.putExtra(
        Intent.EXTRA_TEXT,
        "Device model: " +
                Build.MODEL +
                " (" +
                Build.PRODUCT +
                ")\n" +
                "Android version: " +
                Build.VERSION.RELEASE +
                "\nApp Version: " +
                BuildConfig.VERSION_NAME
    )
    val path = Uri.fromFile(LogHandler.saveLogsToFile(this))
    emailIntent.putExtra(Intent.EXTRA_STREAM, path)
    startActivity(Intent.createChooser(emailIntent, "Send email..."))
}
