package com.artamonov.look4

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_web_view.*

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        web_view_back.setOnClickListener { onBackPressed() }
        webView.loadUrl("https://project77341.tilda.ws/page11400033.html")
    }
}
