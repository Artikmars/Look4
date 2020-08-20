package com.artamonov.look4

import android.os.Bundle
import com.artamonov.look4.base.BaseActivity
import kotlinx.android.synthetic.main.activity_web_view.*

class WebViewActivity : BaseActivity(R.layout.activity_web_view) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web_view_back.setOnClickListener { onBackPressed() }
        webView.loadUrl("https://project77341.tilda.ws/page11400033.html")
    }
}
