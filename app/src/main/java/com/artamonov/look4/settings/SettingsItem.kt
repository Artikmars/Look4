package com.artamonov.look4.settings

import androidx.annotation.StringRes
import com.artamonov.look4.R

sealed class SettingsItem(@StringRes var id: Int) {
    object Profile : SettingsItem(R.string.profile_edit_title)
    object Faq : SettingsItem(R.string.settings_faq)
    object AboutUs : SettingsItem(R.string.settings_about_us)
    object PrivacyPolicy : SettingsItem(R.string.settings_privacy_policy)
}