package com.artamonov.look4

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.Manifest.permission.CHANGE_WIFI_STATE
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.View.GONE
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.UserGender.Companion.ALL
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE
import com.artamonov.look4.utils.UserRole.Companion.ADVERTISER
import com.artamonov.look4.utils.UserRole.Companion.DISCOVERER
import com.artamonov.look4.utils.isValidPhoneNumber
import com.bumptech.glide.Glide
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy.P2P_POINT_TO_POINT
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_look.*
import org.koin.android.ext.android.inject
import java.io.File
import java.nio.charset.Charset

class LookActivity : BaseActivity() {

    private val preferenceHelper: PreferenceHelper by inject()

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            BLUETOOTH,
            BLUETOOTH_ADMIN,
            ACCESS_WIFI_STATE,
            CHANGE_WIFI_STATE,
            ACCESS_COARSE_LOCATION,
            ACCESS_FINE_LOCATION)

        private const val REQUEST_CODE_REQUIRED_PERMISSIONS = 1
        private val STRATEGY = P2P_POINT_TO_POINT
        const val LOG_TAG = "Look4"
        var endpointIdSaved: String? = null
        private var discovererPhoneNumber: String? = null
        private var discovererName: String? = null
        private var discovererFilePath: String? = null
        private var advertiserName: String? = null
        private var advertiserPhoneNumber: String? = null
        private var isGenderValid: Boolean = true
        lateinit var deviceId: String
        private var timer: CountDownTimer? = null
        private var file: File? = null
        private var newFile: File? = null

        private var connectionClient: ConnectionsClient? = null

        private val discOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        discovererName = intent?.getStringExtra("discovererName")
        discovererPhoneNumber = intent?.getStringExtra("discovererPhoneNumber")
        discovererFilePath = intent?.getStringExtra("discovererFilePath")
        endpointIdSaved = intent?.getStringExtra("endpointId")

        if (discovererName != null && discovererPhoneNumber != null && discovererFilePath != null) {
            shouldDisplayIncomeContactViews(true)
            searchingInProgressText.visibility = View.VISIBLE
            searchingInProgressText.text = discovererName
            searchButtonVisibility(false)
            // Toast.makeText(applicationContext, "discovererFilePath: $discovererFilePath in onNewIntent()", Toast.LENGTH_LONG).show()
            profile_image.setImageDrawable(Drawable.createFromPath(discovererFilePath))
        } else if (advertiserPhoneNumber != null) {
            searchingInProgressText.text = advertiserPhoneNumber
            shouldDisplayIncomeContactViews(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_look)
        checkForPermissions()
        connectionClient = Nearby.getConnectionsClient(this)
        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        searchBtn.setOnClickListener { startClient() }
        look_back.setOnClickListener { onBackPressed() }

        no_button.setOnClickListener {
            connectionClient?.stopAllEndpoints()
            shouldDisplayIncomeContactViews(false)
            searchButtonVisibility(true)
        }

        yes_button.setOnClickListener {
            when (preferenceHelper.getUserProfile()?.role) {
                ADVERTISER -> {
                    savePhoneNumberToDB(discovererPhoneNumber, ADVERTISER)
                    shouldDisplayIncomeContactViews(false)
                    searchButtonVisibility(true)

                    showSnackbarWithAction()
//                    Toast.makeText(applicationContext, "Endpoint: $endpointIdSaved", Toast.LENGTH_LONG)
//                        .show()
                    Log.v(LOG_TAG, "Endpoint: $endpointIdSaved")
                    endpointIdSaved?.let {
                        connectionClient?.sendPayload(
                            endpointIdSaved!!, Payload.fromBytes(preferenceHelper.getUserProfile()?.phoneNumber!!.toByteArray()))?.addOnFailureListener { e ->
                            showSnackbarError(e.toString())
                            connectionClient ?.disconnectFromEndpoint(
                                endpointIdSaved!!) }
                    }
                }
                DISCOVERER -> {
//                    Toast.makeText(applicationContext, "Endpoint: $endpointIdSaved", Toast.LENGTH_SHORT)
//                        .show()
                    connectionClient?.stopDiscovery()
                    val pfd: ParcelFileDescriptor? = contentResolver.openFileDescriptor(Uri.parse(preferenceHelper.getUserProfile()?.imagePath!!), "r")
                    val pFilePayload = Payload.fromFile(pfd!!)
                    endpointIdSaved?.let {
                        Log.v(LOG_TAG, "Endpoint: $endpointIdSaved")
                        connectionClient?.sendPayload(endpointIdSaved!!,
                            Payload.fromBytes("${getUserProfile()?.name};${getUserProfile()?.phoneNumber};${getUserProfile()?.gender}".toByteArray()))?.addOnFailureListener {
                            e -> showSnackbarError(e.toString())
                    }
                        connectionClient?.sendPayload(endpointIdSaved!!, pFilePayload)?.addOnFailureListener {
                                e -> showSnackbarError(e.toString())
                        }
                    }
                    shouldDisplayIncomeContactViews(false)
                    searchButtonVisibility(true)
                    searchingInProgressText.visibility = GONE
                }
            }
        }

        onNewIntent(intent)

//        disconnectButton.setOnClickListener {
//            connClient.apply {
//             stopAdvertising()
//               stopDiscovery()
//                stopAllEndpoints()
//           }
//        }
    }

    private fun showSnackbarWithAction() {
        val snackbar = Snackbar.make(findViewById(android.R.id.content),
            getString(R.string.look_you_received_phone_number), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.look_view)) {
                startActivity(Intent(this, ContactsActivity::class.java))
            }

        snackbar.show()
    }

    private fun getUserProfile(): User? {
        return preferenceHelper.getUserProfile()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        if (!hasPermissions(this, REQUIRED_PERMISSIONS.toString())) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS)
        }
    }

    private fun checkForPermissions() {
        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(READ_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
        }
    }

    /** Returns true if the app was granted all the permissions. Otherwise, returns false.  */
    private fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                            context,
                            permission
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun startClient() {
        searchButtonVisibility(false)
        searchingInProgressText.visibility = View.VISIBLE
        searchingInProgressText.isAllCaps = true
        searchingInProgressText.text = resources.getString(R.string.look_searching_in_progress)
        talk_in_person_text.visibility = GONE
        look_divider.visibility = GONE

        startTimer()
        connectionClient?.startDiscovery(packageName, endpointDiscoveryCallback, discOptions)
        ?.addOnSuccessListener {
                    // logD( "$deviceId started discovering.")
                    preferenceHelper.updateRole(DISCOVERER)
                }?.addOnFailureListener { e ->
//                Toast.makeText(applicationContext,
//                    "Couldn't connect because of: $e", Toast.LENGTH_LONG).show()
                showSnackbarError(e.toString())
                searchingInProgressText.text = resources.getString(R.string.look_no_found)
                talk_in_person_text.visibility = View.VISIBLE
                look_divider.visibility = View.VISIBLE
                searchBtn.text = resources.getString(R.string.look_search_again)
                // We're unable to start discovering.
                // logE("We're unable to start discovering.", e)
                }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(25000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdown_view.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                connectionClient?.stopAllEndpoints()
                setCountDownViewsVisibility(false)
                searchingInProgressText.text = resources.getString(R.string.look_no_found)
                talk_in_person_text.visibility = View.VISIBLE
                look_divider.visibility = View.VISIBLE
                searchBtn.text = resources.getString(R.string.look_search_again)
                searchButtonVisibility(true)
            }
        }
        setCountDownViewsVisibility(true)
        timer?.start()
    }

    private fun setCountDownViewsVisibility(state: Boolean) {
        if (state) {
            countdown_view.visibility = View.VISIBLE
            countdown_label.visibility = View.VISIBLE
        } else {
            countdown_view.visibility = View.GONE
            countdown_label.visibility = View.GONE
        }
    }

    private val endpointDiscoveryCallback: EndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // An endpoint was found. We request a connection to it.
            // logD( "onEndpointFound: endpointId: $endpointId")
            // endpointIdSaved = endpointId
            Log.v(LOG_TAG, "in endpointDiscoveryCallbackEndpoint: $endpointId")

            connectionClient?.requestConnection(getUserProfile()?.name!!, endpointId, connectionLifecycleCallback)
                    ?.addOnSuccessListener {
//                        Toast.makeText(
//                                applicationContext,
//                                "Requested connection to $endpointId !",
//                                Toast.LENGTH_SHORT
//                        ).show()
//
// logD( "Requested connection to $endpointId !")
                        //  searchingInProgressText.text = "You are connected!"

                        // We successfully requested a connection. Now both sides
                        // must accept before the connection is established.
                    }
                ?.addOnFailureListener { e ->
                    handleFailedResponse()
                    showSnackbarError(e.toString())
                    connectionClient?.stopAllEndpoints()
                    // logW( "Connection request to $endpointId failed!")
                        // Nearby Connections failed to request the connection.
                    }
        }

        override fun onEndpointLost(endpointId: String) {
            showSnackbarError("Endpoint: $endpointId lost!")
            // logW("Endpoint: $endpointId lost!")
            // A previously discovered endpoint has gone away.
        }
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
//            Toast.makeText(applicationContext, "Connection with $p0 initiated.", Toast.LENGTH_SHORT)
//                .show()
            //  logD("Connection initiated : $p0 ,$p1")
            connectionClient?.acceptConnection(p0, payloadCallback)?.addOnFailureListener { e ->
                showSnackbarError(e.toString()) }
            // Automatically accept the connection on both sides.
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            // logD( "onConnectionResult")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    endpointIdSaved = endpointId
                    Log.v(LOG_TAG, "Connection initiated - endpoint: $endpointIdSaved")
                }

                // We're connected! Can now start sending and receiving data.

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    handleFailedResponse()
                    showSnackbarError("Connection attempt to $endpointId was rejected")

                    //   logW("Connection attempt to $endpointId was rejected")
                }
                // The connection was rejected by one or both sides.

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    handleFailedResponse()
                    showSnackbarError("Connected attempt to $endpointId failed")

                    //   logW( "Connected attempt to $endpointId failed")
                }
                // The connection broke before it was able to be accepted.

                else -> {
                    handleFailedResponse()
                    showSnackbarError("Unknown status code")
                    //    logW( "Unknown status code")
                }
                // Unknown status code
            }
        }

        override fun onDisconnected(p0: String) {
            // logW("onDisconnected")
            // We've been disconnected from this endpoint. No more data can be
            // sent or received.
            showSnackbarError("Disconnected")
        }
    }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            when (preferenceHelper.getUserProfile()?.role) {
                DISCOVERER -> {
                        when (p1.type) {
                            Payload.Type.BYTES -> {
                                val byteString = p1.asBytes()?.toString(Charset.defaultCharset())
                                val textArray = byteString?.split(";")?.toTypedArray()

                                when (textArray?.size) {
                                    0, 1 -> isGenderValid = true
                                    2 -> if (!isGenderValid(textArray[1])) { return }
                                }

                                if (isMobileNumber(textArray?.get(0))) {
                                    savePhoneNumberToDB(textArray?.get(0), DISCOVERER)
                                    showSnackbarWithAction()
                                    shouldDisplayIncomeContactViews(false)
                                } else {
                                    advertiserName = textArray?.get(0)
                                }
                            }
                            Payload.Type.FILE -> {
                                file = p1.asFile()?.asJavaFile()
                                file?.renameTo(File(file?.parentFile, "look4.jpg"))
                                newFile = File(file?.parentFile, "look4.jpg")
                                Log.v("Look4", "newFile.path: ${newFile?.path}")
                                Log.v("Look4", "newFile.exists(): ${newFile?.exists()}")
                            }
                        }
                }
                ADVERTISER -> {
                    searchButtonVisibility(false)
                    val receivedContact = p1.asBytes()?.toString(Charset.defaultCharset())
                    val textArray = receivedContact?.split(";")?.toTypedArray()
                    discovererPhoneNumber = textArray?.get(1)
                    shouldDisplayIncomeContactViews(true)
                }
            }
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            if (p1.status == PayloadTransferUpdate.Status.SUCCESS && p1.totalBytes > 1000) {
                if (profile_image.drawable == null && isGenderValid) {
                    timer?.cancel()
                    setCountDownViewsVisibility(false)
                    advertiserName?.let { searchingInProgressText.text = advertiserName }
                    setCountDownViewsVisibility(false)
                    shouldDisplayIncomeContactViews(true)
                    Log.v("Look4", "onPayloadTransferUpdate - newFile.path: ${newFile?.path}")
                    Log.v("Look4", "onPayloadTransferUpdate newFile.exists(): ${newFile?.exists()}")
                    Glide.with(applicationContext).load(newFile).into(profile_image)
                }
            }
        }
    }

    private fun isGenderValid(advertiserGender: @UserGender.AnnotationUserGender String?): Boolean {
        when (preferenceHelper.getUserProfile()?.lookGender) {
            MALE -> {
                isGenderValid = advertiserGender == MALE
                return isGenderValid
            }
            FEMALE -> {
                isGenderValid = advertiserGender == FEMALE
                return isGenderValid
            }
            ALL -> {
                isGenderValid = true
                return isGenderValid
            }
        }
        isGenderValid = true
        return isGenderValid
    }

    private fun savePhoneNumberToDB(phoneNumber: String?, userRole: String) {
        when (userRole) {
            DISCOVERER -> {
                if (phoneNumber != null && advertiserName != null) {
                    preferenceHelper.saveContact(name = advertiserName!!, phoneNumber = phoneNumber)
                }
            }
            ADVERTISER -> {
                if (phoneNumber != null && discovererName != null) {
                    preferenceHelper.saveContact(name = discovererName!!, phoneNumber = phoneNumber)
                }
            }
        }
    }

    private fun shouldDisplayIncomeContactViews(isVisible: Boolean) {
        if (isVisible) {
            no_button.visibility = View.VISIBLE
            yes_button.visibility = View.VISIBLE
            profile_image.visibility = View.VISIBLE
            found_view.visibility = View.VISIBLE
        } else {
            no_button.visibility = View.GONE
            yes_button.visibility = View.GONE
            profile_image.visibility = View.GONE
            found_view.visibility = View.GONE
        }
    }

    private fun searchButtonVisibility(isVisible: Boolean) {
        if (isVisible) {
            searchBtn.visibility = View.VISIBLE
        } else {
            searchBtn.visibility = View.GONE
        }
    }

    private fun isMobileNumber(string: String?): Boolean {
        return string?.isValidPhoneNumber() ?: false
    }

    private fun handleFailedResponse() {
        connectionClient?.stopAllEndpoints()
        searchButtonVisibility(true)
        timer?.cancel()
        setCountDownViewsVisibility(false)
        searchingInProgressText.text = resources.getString(R.string.look_no_found)
        searchBtn.text = resources.getString(R.string.look_search_again)
        searchButtonVisibility(true)
    }
}