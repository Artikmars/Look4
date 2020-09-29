package com.artamonov.look4.look

import android.app.Activity
import android.view.View.GONE
import android.view.View.VISIBLE
import com.artamonov.look4.R
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.activity_look.*

fun Activity.populateDefaultView() {
    searchingInProgressText.isAllCaps = true
    talk_in_person_text.visibility = GONE
    look_divider.visibility = GONE
    searchBtn.visibility = GONE
    no_button.visibility = GONE
    yes_button.visibility = GONE
    profile_image.visibility = GONE
    found_view.visibility = GONE
    searchingInProgressText.visibility = GONE
    countdown_view.visibility = GONE
    FirebaseCrashlytics.getInstance().log("Default view is populated")
}

fun Activity.populateScanningView() {
    talk_in_person_text.visibility = GONE
    look_divider.visibility = GONE
    searchBtn.visibility = GONE
    no_button.visibility = GONE
    yes_button.visibility = GONE
    profile_image.visibility = GONE
    found_view.visibility = GONE
    searchingInProgressText.visibility = GONE
    countdown_view.visibility = VISIBLE
    FirebaseCrashlytics.getInstance().log("Scanning view is populated")
}
fun Activity.populateNoFoundView() {
    searchingInProgressText.text = resources.getString(R.string.look_no_found)
    searchBtn.text = resources.getString(R.string.look_search_again)
    countdown_view.visibility = GONE
    no_button.visibility = GONE
    yes_button.visibility = GONE
    profile_image.visibility = GONE
    found_view.visibility = GONE
    talk_in_person_text.visibility = VISIBLE
    look_divider.visibility = VISIBLE
    searchBtn.visibility = VISIBLE
    searchingInProgressText.visibility = VISIBLE
    FirebaseCrashlytics.getInstance().log("No found view is populated")
}

fun Activity.populateSucceedView() {
    countdown_view.visibility = GONE
    talk_in_person_text.visibility = GONE
    look_divider.visibility = GONE
    searchBtn.visibility = GONE
    no_button.visibility = VISIBLE
    yes_button.visibility = VISIBLE
    profile_image.visibility = VISIBLE
    found_view.visibility = VISIBLE
    FirebaseCrashlytics.getInstance().log("Succeed view is populated")
}

fun Activity.populatePendingView() {
    countdown_view.visibility = GONE
    talk_in_person_text.visibility = GONE
    look_divider.visibility = GONE
    searchBtn.visibility = GONE
    no_button.visibility = GONE
    yes_button.visibility = GONE
    profile_image.visibility = VISIBLE
    found_view.visibility = VISIBLE
    FirebaseCrashlytics.getInstance().log("Succeed view is populated")
}
