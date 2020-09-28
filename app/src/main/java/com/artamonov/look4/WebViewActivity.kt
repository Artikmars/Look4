package com.artamonov.look4

import android.os.Bundle
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.utils.IS_FAQ
import kotlinx.android.synthetic.main.activity_web_view.*

class WebViewActivity : BaseActivity(R.layout.activity_web_view) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web_view_back.setOnClickListener { onBackPressed() }

        val showFaq = intent.getBooleanExtra(IS_FAQ, true)
        if (showFaq) {
            web_view_title.text = getString(R.string.settings_faq)
            webView.loadUrl(getString(R.string.faq_link))
        } else {
            web_view_title.text = getString(R.string.settings_privacy_policy)
            webView.loadUrl(getString(R.string.privacy_policy_link))
        }
    }
}
