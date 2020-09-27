package com.artamonov.look4.settings

import android.os.Bundle
import com.artamonov.look4.BuildConfig
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.utils.getMissingInternetSnackbar
import com.artamonov.look4.utils.getSnackbar
import com.artamonov.look4.utils.isNetworkNotAvailable
import com.artamonov.look4.utils.sendEmail
import com.artamonov.look4.utils.startAboutUsActivity
import com.artamonov.look4.utils.startUserProfileEditActivity
import com.artamonov.look4.utils.startWebViewActivity
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : BaseActivity(R.layout.activity_settings) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings_profile.setOnClickListener { startUserProfileEditActivity() }
        settings_about_us.setOnClickListener { startAboutUsActivity() }
        settings_privacy_policy.setOnClickListener {
            if (isNetworkNotAvailable()) {
                getMissingInternetSnackbar(getSnackbar(R.string.network_error_unknown))?.show()
                return@setOnClickListener
            }
            startWebViewActivity() }
        settings_back.setOnClickListener { onBackPressed() }
        if (BuildConfig.DEBUG) {
            settings_current_version.text = getString(R.string.settings_debug_version, BuildConfig.VERSION_NAME)
        } else {
            settings_current_version.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
        }

        settings_current_version.setOnClickListener { sendEmail() }
    }
}
