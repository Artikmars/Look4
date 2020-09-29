package com.artamonov.look4

import android.os.Bundle
import android.view.View.VISIBLE
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.utils.FAQ_TYPE
import com.artamonov.look4.utils.WebViewType.DEFAULT_FAQ
import com.artamonov.look4.utils.WebViewType.PRIVACY_POLICY
import com.artamonov.look4.utils.startMainActivity
import com.artamonov.look4.utils.startWelcomeActivity
import kotlinx.android.synthetic.main.activity_web_view.*

class WebViewActivity : BaseActivity(R.layout.activity_web_view) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webViewTypeIntent = intent.getStringExtra(FAQ_TYPE)

        if (webViewTypeIntent == null && prefs.userAvailable()) {
            finish()
            startMainActivity()
        }

        web_view_back.setOnClickListener { onBackPressed() }
        webView.loadUrl(getString(R.string.splash_faq_link))

        when (webViewTypeIntent) {
                DEFAULT_FAQ -> {
                    webView.loadUrl(getString(R.string.faq_link))
                    web_view_back.visibility = VISIBLE
                }
                PRIVACY_POLICY -> {
                    web_view_title.text = getString(R.string.settings_privacy_policy)
                    webView.loadUrl(getString(R.string.privacy_policy_link))
                    web_view_back.visibility = VISIBLE
                }
            }
        ok_button.setOnClickListener {
            finish()
            startWelcomeActivity()
        }
    }
}
