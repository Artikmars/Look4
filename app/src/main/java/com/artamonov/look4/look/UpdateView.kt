package com.artamonov.look4.look

import android.app.Activity
import androidx.core.view.isVisible
import com.artamonov.look4.R
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.activity_look.*

fun Activity.populateDefaultView() {
    searchingInProgressText.isAllCaps = true
    talk_in_person_text.isVisible = false
    look_divider.isVisible = false
    searchBtn.isVisible = false
    no_button.isVisible = false
    yes_button.isVisible = false
    profile_image.isVisible = false
    found_view.isVisible = false
    searchingInProgressText.isVisible = false
    countdown_view.isVisible = false
    FirebaseCrashlytics.getInstance().log("Default view is populated")
}

fun Activity.populateScanningView() {
    talk_in_person_text.isVisible = false
    look_divider.isVisible = false
    searchBtn.isVisible = false
    no_button.isVisible = false
    yes_button.isVisible = false
    profile_image.isVisible = false
    found_view.isVisible = false
    searchingInProgressText.isVisible = false
    countdown_view.isVisible = true
    FirebaseCrashlytics.getInstance().log("Scanning view is populated")
}

fun Activity.populateNoFoundView() {
    searchingInProgressText.text = resources.getString(R.string.look_no_found)
    searchBtn.text = resources.getString(R.string.look_search_again)
    countdown_view.isVisible = false
    no_button.isVisible = false
    yes_button.isVisible = false
    profile_image.isVisible = false
    found_view.isVisible = false
    talk_in_person_text.isVisible = true
    look_divider.isVisible = true
    searchBtn.isVisible = true
    searchingInProgressText.isVisible = true
    FirebaseCrashlytics.getInstance().log("No found view is populated")
}

fun Activity.populateSucceedView() {
    countdown_view.isVisible = false
    talk_in_person_text.isVisible = false
    look_divider.isVisible = false
    searchBtn.isVisible = false
    no_button.isVisible = true
    yes_button.isVisible = true
    profile_image.isVisible = true
    found_view.isVisible = true
    FirebaseCrashlytics.getInstance().log("Succeed view is populated")
}

fun Activity.populatePendingView() {
    countdown_view.isVisible = false
    talk_in_person_text.isVisible = false
    look_divider.isVisible = false
    searchBtn.isVisible = false
    no_button.isVisible = false
    yes_button.isVisible = false
    profile_image.isVisible = true
    found_view.isVisible = true
    FirebaseCrashlytics.getInstance().log("Succeed view is populated")
}
