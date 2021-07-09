package com.artamonov.look4.look

import androidx.core.view.isVisible
import com.artamonov.look4.R
import com.artamonov.look4.databinding.ActivityLookBinding
import com.google.firebase.crashlytics.FirebaseCrashlytics

fun ActivityLookBinding.populateDefaultView() {
    searchingInProgressText.isAllCaps = true
    talkInPersonText.isVisible = false
    lookDivider.isVisible = false
    searchBtn.isVisible = false
    noButton.isVisible = false
    yesButton.isVisible = false
    profileImage.isVisible = false
    foundView.isVisible = false
    searchingInProgressText.isVisible = false
    countdownView.isVisible = false
    FirebaseCrashlytics.getInstance().log("Default view is populated")
}

fun ActivityLookBinding.populateScanningView() {
    talkInPersonText.isVisible = false
    lookDivider.isVisible = false
    searchBtn.isVisible = false
    noButton.isVisible = false
    yesButton.isVisible = false
    profileImage.isVisible = false
    foundView.isVisible = false
    searchingInProgressText.isVisible = false
    countdownView.isVisible = true
    FirebaseCrashlytics.getInstance().log("Scanning view is populated")
}

fun ActivityLookBinding.populateNoFoundView() {
    searchingInProgressText.text = this.root.resources.getString(R.string.look_no_found)
    searchBtn.text = this.root.resources.getString(R.string.look_search_again)
    countdownView.isVisible = false
    noButton.isVisible = false
    yesButton.isVisible = false
    profileImage.isVisible = false
    foundView.isVisible = false
    talkInPersonText.isVisible = true
    lookDivider.isVisible = true
    searchBtn.isVisible = true
    searchingInProgressText.isVisible = true
    FirebaseCrashlytics.getInstance().log("No found view is populated")
}

fun ActivityLookBinding.populateSucceedView() {
    countdownView.isVisible = false
    talkInPersonText.isVisible = false
    lookDivider.isVisible = false
    searchBtn.isVisible = false
    noButton.isVisible = true
    yesButton.isVisible = true
    profileImage.isVisible = true
    foundView.isVisible = true
    FirebaseCrashlytics.getInstance().log("Succeed view is populated")
}

fun ActivityLookBinding.populatePendingView() {
    countdownView.isVisible = false
    talkInPersonText.isVisible = false
    lookDivider.isVisible = false
    searchBtn.isVisible = false
    noButton.isVisible = false
    yesButton.isVisible = false
    profileImage.isVisible = true
    foundView.isVisible = true
    FirebaseCrashlytics.getInstance().log("Succeed view is populated")
}
