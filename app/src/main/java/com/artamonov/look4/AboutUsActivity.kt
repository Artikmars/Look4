package com.artamonov.look4

import android.os.Bundle
import android.text.util.Linkify
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_about_us.*

class AboutUsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)
        Linkify.addLinks(about_us_description, Linkify.WEB_URLS or Linkify.PHONE_NUMBERS)
        Linkify.addLinks(about_us_description, Linkify.ALL)
        about_us_back.setOnClickListener { onBackPressed() }
    }
}
