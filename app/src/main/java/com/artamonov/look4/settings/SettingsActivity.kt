package com.artamonov.look4.settings

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.artamonov.look4.AboutUsActivity
import com.artamonov.look4.R
import com.artamonov.look4.userprofiledit.UserProfileEditActivity
import com.artamonov.look4.WebViewActivity
import com.artamonov.look4.base.BaseActivity
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings_profile.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivity(Intent(this, UserProfileEditActivity::class.java),
                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            } else {
                startActivity(Intent(this, UserProfileEditActivity::class.java))
            } }

        settings_about_us.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivity(Intent(this, AboutUsActivity::class.java),
                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            } else {
                startActivity(Intent(this, AboutUsActivity::class.java))
            } }

        settings_privacy_policy.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivity(Intent(this, WebViewActivity::class.java),
                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            } else {
                startActivity(Intent(this, WebViewActivity::class.java))
            } }

        settings_back.setOnClickListener { onBackPressed() }
    }
}
