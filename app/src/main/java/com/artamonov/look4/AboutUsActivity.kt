package com.artamonov.look4

import android.os.Bundle
import android.text.util.Linkify
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.databinding.ActivityAboutUsBinding

class AboutUsActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutUsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutUsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Linkify.addLinks(binding.aboutUsDescription, Linkify.WEB_URLS or Linkify.PHONE_NUMBERS)
        Linkify.addLinks(binding.aboutUsDescription, Linkify.ALL)
        binding.aboutUsBack.setOnClickListener { onBackPressed() }
    }
}
