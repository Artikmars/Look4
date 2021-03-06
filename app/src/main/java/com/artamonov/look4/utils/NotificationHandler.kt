package com.artamonov.look4.utils

import android.content.Context
import android.content.Intent
import com.artamonov.look4.data.database.ContactRequest
import com.artamonov.look4.look.LookActivity
import com.artamonov.look4.main.MainActivity

class NotificationHandler() {

    private var advertiserName: String? = null
    private var discovererName: String? = null
    private var discovererPhoneNumber: String? = null
    private var discovererFilePath: String? = null
    private var advertiserPhoneNumber: String? = null
    private var endpointIdSaved: String? = null

    constructor(intent: Intent) : this() {
        discovererName = intent.getStringExtra(EXTRA_DISCOVERER_NAME)
        discovererPhoneNumber = intent.getStringExtra(EXTRA_DISCOVERER_PHONE_NUMBER)
        discovererFilePath = intent.getStringExtra(EXTRA_DISCOVERER_FILE_PATH)
        endpointIdSaved = intent.getStringExtra(EXTRA_ENDPOINT_ID)
    }

    constructor(
        discovererName: String?,
        discovererPhoneNumber: String?,
        discovererFilePath: String?,
        advertiserPhoneNumber: String?,
        endpointIdSaved: String
    ) : this() {
        this.advertiserPhoneNumber = advertiserPhoneNumber
        this.discovererFilePath = discovererFilePath
        this.discovererName = discovererName
        this.discovererPhoneNumber = discovererPhoneNumber
        this.endpointIdSaved = endpointIdSaved
    }

    fun isNotificationValid(): Boolean = (discovererFilePath != null && discovererName != null &&
            discovererPhoneNumber != null && endpointIdSaved != null)

    fun getAdvertiserName() = this.advertiserName
    fun getDiscovererName() = this.discovererName
    fun getDiscovererFilePath() = this.discovererFilePath
    fun getDiscovererPhoneNumber() = this.discovererPhoneNumber
    fun getAdvertiserPhoneNumber() = this.advertiserPhoneNumber
    fun getEndpointId() = this.endpointIdSaved

    fun createIntent(context: Context): Intent {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(EXTRA_DISCOVERER_NAME, discovererName)
        intent.putExtra(EXTRA_DISCOVERER_PHONE_NUMBER, discovererPhoneNumber)
        intent.putExtra(EXTRA_DISCOVERER_FILE_PATH, discovererFilePath)
        intent.putExtra(EXTRA_ADVERTISER_PHONE_NUMBER, advertiserPhoneNumber)
        intent.putExtra(EXTRA_ENDPOINT_ID, endpointIdSaved)
        return intent
    }

    fun createIntent(context: Context, request: ContactRequest): Intent {
        val intent = Intent(context, LookActivity::class.java)
        intent.putExtra(EXTRA_DISCOVERER_NAME, request.name)
        intent.putExtra(EXTRA_DISCOVERER_PHONE_NUMBER, request.phoneNumber)
        intent.putExtra(EXTRA_DISCOVERER_FILE_PATH, request.filePath)
        intent.putExtra(EXTRA_ENDPOINT_ID, request.endpointId)
        return intent
    }

    companion object {
        const val EXTRA_DISCOVERER_NAME = "discovererName"
        const val EXTRA_DISCOVERER_PHONE_NUMBER = "discovererPhoneNumber"
        const val EXTRA_DISCOVERER_FILE_PATH = "discovererFilePath"
        const val EXTRA_ADVERTISER_PHONE_NUMBER = "advertiserPhoneNumber"
        const val EXTRA_ENDPOINT_ID = "endpointId"
    }
}
