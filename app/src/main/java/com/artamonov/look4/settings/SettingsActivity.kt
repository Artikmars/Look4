package com.artamonov.look4.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.artamonov.look4.AboutUsActivity
import com.artamonov.look4.R
import com.artamonov.look4.userprofiledit.UserProfileEditActivity
import com.artamonov.look4.WebViewActivity
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings_profile.setOnClickListener {
            startActivity(Intent(this, UserProfileEditActivity::class.java))
        }

        settings_about_us.setOnClickListener {
            startActivity(Intent(this, AboutUsActivity::class.java))
        }

        settings_privacy_policy.setOnClickListener {
            startActivity(Intent(this, WebViewActivity::class.java))
        }

        settings_back.setOnClickListener { onBackPressed() }
    }
}
