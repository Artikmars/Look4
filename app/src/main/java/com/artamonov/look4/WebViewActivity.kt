package com.artamonov.look4

import android.os.Bundle
import androidx.core.view.isVisible
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.databinding.ActivityWebViewBinding
import com.artamonov.look4.utils.FAQ_TYPE
import com.artamonov.look4.utils.WebViewType.DEFAULT_FAQ
import com.artamonov.look4.utils.WebViewType.PRIVACY_POLICY
import com.artamonov.look4.utils.startMainActivity
import com.artamonov.look4.utils.startWelcomeActivity

class WebViewActivity : BaseActivity() {

    private lateinit var binding: ActivityWebViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webViewTypeIntent = intent.getStringExtra(FAQ_TYPE)

        if (webViewTypeIntent == null && prefs.userAvailable()) {
            finish()
            startMainActivity()
        }

        if (webViewTypeIntent == null) {
            binding.webViewTitle.isVisible = false
            binding.webViewBack.isVisible = false
        }

        binding.webViewBack.setOnClickListener { onBackPressed() }
        binding.webView.loadUrl(getString(R.string.splash_faq_link))

        when (webViewTypeIntent) {
                DEFAULT_FAQ -> {
                    binding.okButton.isVisible = false
                    binding.webView.loadUrl(getString(R.string.faq_link))
                }
                PRIVACY_POLICY -> {
                    binding.okButton.isVisible = false
                    binding.webViewTitle.isVisible = false
                    binding.webViewTitle.text = getString(R.string.settings_privacy_policy)
                    binding.webView.loadUrl(getString(R.string.privacy_policy_link))
                }
            }

        binding.okButton.setOnClickListener {
            finish()
            startWelcomeActivity()
        }
    }
}
