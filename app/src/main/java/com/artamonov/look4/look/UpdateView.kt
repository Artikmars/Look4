package com.artamonov.look4.look

import android.app.Activity
import android.view.View
import com.artamonov.look4.R
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.activity_look.*

fun Activity.populateDefaultView() {
    searchingInProgressText.isAllCaps = true
    talk_in_person_text.visibility = View.GONE
    look_divider.visibility = View.GONE
    searchBtn.visibility = View.GONE
    no_button.visibility = View.GONE
    yes_button.visibility = View.GONE
    profile_image.visibility = View.GONE
    found_view.visibility = View.GONE
    searchingInProgressText.visibility = View.GONE
    countdown_view.visibility = View.GONE
    Crashlytics.log("Default view is populated")
}

fun Activity.populateScanningView() {
    talk_in_person_text.visibility = View.GONE
    look_divider.visibility = View.GONE
    searchBtn.visibility = View.GONE
    no_button.visibility = View.GONE
    yes_button.visibility = View.GONE
    profile_image.visibility = View.GONE
    found_view.visibility = View.GONE
    searchingInProgressText.visibility = View.GONE
    countdown_view.visibility = View.VISIBLE
    Crashlytics.log("Scanning view is populated")
}
fun Activity.populateNoFoundView() {
    searchingInProgressText.text = resources.getString(R.string.look_no_found)
    searchBtn.text = resources.getString(R.string.look_search_again)
    countdown_view.visibility = View.GONE
    no_button.visibility = View.GONE
    yes_button.visibility = View.GONE
    profile_image.visibility = View.GONE
    found_view.visibility = View.GONE
    talk_in_person_text.visibility = View.VISIBLE
    look_divider.visibility = View.VISIBLE
    searchBtn.visibility = View.VISIBLE
    searchingInProgressText.visibility = View.VISIBLE
    Crashlytics.log("No found view is populated")
}

fun Activity.populateSucceedView() {
    countdown_view.visibility = View.GONE
    talk_in_person_text.visibility = View.GONE
    look_divider.visibility = View.GONE
    searchBtn.visibility = View.GONE
    no_button.visibility = View.VISIBLE
    yes_button.visibility = View.VISIBLE
    profile_image.visibility = View.VISIBLE
    found_view.visibility = View.VISIBLE
    Crashlytics.log("Succeed view is populated")
}
