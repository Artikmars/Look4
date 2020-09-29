package com.artamonov.look4.utils

import androidx.annotation.StringDef

object WebViewType {
    const val DEFAULT_FAQ = "DEFAULT_FAQ"
    const val PRIVACY_POLICY = "PRIVACY_POLICY"

    @StringDef(DEFAULT_FAQ, PRIVACY_POLICY)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class AnnotationWebViewType
}
