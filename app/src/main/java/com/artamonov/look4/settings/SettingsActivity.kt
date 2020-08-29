package com.artamonov.look4.settings

import android.os.Bundle
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.utils.startAboutUsActivity
import com.artamonov.look4.utils.startUserProfileEditActivity
import com.artamonov.look4.utils.startWebViewActivity
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : BaseActivity(R.layout.activity_settings) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings_profile.setOnClickListener { startUserProfileEditActivity() }
        settings_about_us.setOnClickListener { startAboutUsActivity() }
        settings_privacy_policy.setOnClickListener { startWebViewActivity() }
        settings_back.setOnClickListener { onBackPressed() }
    }
}
