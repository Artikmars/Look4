package com.artamonov.look4

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.Manifest.permission.CHANGE_WIFI_STATE
import android.Manifest.permission.ACCESS_COARSE_LOCATION
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.artamonov.look4.utils.UserRole.Companion.ADVERTISER
import com.artamonov.look4.utils.UserRole.Companion.DISCOVERER
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
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy.P2P_CLUSTER
import kotlinx.android.synthetic.main.activity_look.*
import java.io.File
import java.nio.charset.Charset

class LookActivity : AppCompatActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            BLUETOOTH,
            BLUETOOTH_ADMIN,
            ACCESS_WIFI_STATE,
            CHANGE_WIFI_STATE,
            ACCESS_COARSE_LOCATION)

        private const val REQUEST_CODE_REQUIRED_PERMISSIONS = 1

        private val STRATEGY = P2P_CLUSTER
        const val LOG_TAG = "Look4"
        var endpointIdSaved: String? = null
        var userRole = ADVERTISER
        private var discovererPhoneNumber: String? = null
        private var discovererName: String? = null
        private var discovererFilePath: String? = null
        private var advertiserName: String? = null
        private var advertiserPhoneNumber: String? = null
        lateinit var deviceId: String
        private var timer: CountDownTimer? = null
        private var file: File? = null
        private var newFile: File? = null
        private lateinit var userName: String
        private lateinit var userPhoneNumber: String
        private lateinit var userImagePath: String

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
            Toast.makeText(applicationContext, "discovererFilePath: $discovererFilePath in onNewIntent()", Toast.LENGTH_LONG).show()
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
        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        searchBtn.setOnClickListener { startClient() }

        userName = PreferenceManager.getDefaultSharedPreferences(this).getString(
            USER_NAME, null)!!
        userPhoneNumber = PreferenceManager.getDefaultSharedPreferences(this).getString(
            USER_PHONE_NUMBER, null)!!
        userImagePath = PreferenceManager.getDefaultSharedPreferences(this).getString(
            USER_IMAGE_URI, null)!!

        no_button.setOnClickListener {
            Nearby.getConnectionsClient(this).stopAllEndpoints()
            shouldDisplayIncomeContactViews(false)
            searchButtonVisibility(true)
        }

        yes_button.setOnClickListener {
            when (userRole) {
                ADVERTISER -> {
                    searchingInProgressText.text = "Congratulations! Here is the phone number: $discovererPhoneNumber"
                    shouldDisplayIncomeContactViews(false)
                    searchButtonVisibility(true)
//                    Toast.makeText(applicationContext, "Endpoint: $endpointIdSaved", Toast.LENGTH_LONG)
//                        .show()
                    Log.v(LOG_TAG, "Endpoint: $endpointIdSaved")
                    endpointIdSaved?.let {
                        Nearby.getConnectionsClient(applicationContext).sendPayload(
                            endpointIdSaved!!, Payload.fromBytes(userPhoneNumber.toByteArray()))
                    }
                }
                DISCOVERER -> {
//                    Toast.makeText(applicationContext, "Endpoint: $endpointIdSaved", Toast.LENGTH_SHORT)
//                        .show()
                    //  val file = File(Uri.parse(userImagePath).path!!)
                    Nearby.getConnectionsClient(applicationContext).stopDiscovery()
                    val pfd: ParcelFileDescriptor? = contentResolver.openFileDescriptor(Uri.parse(userImagePath), "r")
                    val pFilePayload = Payload.fromFile(pfd!!)
                    endpointIdSaved?.let {
                        Log.v(LOG_TAG, "Endpoint: $endpointIdSaved")
                        Nearby.getConnectionsClient(applicationContext).sendPayload(endpointIdSaved!!,
                            Payload.fromBytes("$userName;$userPhoneNumber".toByteArray()))
                        Nearby.getConnectionsClient(applicationContext).sendPayload(endpointIdSaved!!, pFilePayload)
                    }
                    shouldDisplayIncomeContactViews(false)
                    searchButtonVisibility(true)
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

    override fun onStop() {
        Nearby.getConnectionsClient(this).stopAdvertising()
        Nearby.getConnectionsClient(this).stopAllEndpoints()
        super.onStop()
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
        searchingInProgressText.text = resources.getString(R.string.searching_in_progress)

        startTimer()
        Nearby.getConnectionsClient(applicationContext).stopDiscovery()
        Nearby.getConnectionsClient(applicationContext).startDiscovery(packageName, endpointDiscoveryCallback, discOptions)
                .addOnSuccessListener {
                    // logD( "$deviceId started discovering.")
                    userRole = DISCOVERER
                }.addOnFailureListener { e ->
                Toast.makeText(applicationContext,
                    "Couldn't connect because of: $e", Toast.LENGTH_LONG).show()
                searchingInProgressText.text = resources.getString(R.string.no_found)
                searchBtn.text = resources.getString(R.string.search_again)
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
                Nearby.getConnectionsClient(applicationContext).stopDiscovery()
                setCountDownViewsVisibility(false)
                searchingInProgressText.text = resources.getString(R.string.no_found)
                searchBtn.text = resources.getString(R.string.search_again)
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

            Nearby.getConnectionsClient(applicationContext).requestConnection(info.endpointName + ".1", endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener {
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
                .addOnFailureListener {
                    Nearby.getConnectionsClient(applicationContext).stopDiscovery()
                    searchButtonVisibility(true)
                    timer?.cancel()
                    setCountDownViewsVisibility(false)
                    searchingInProgressText.text = resources.getString(R.string.no_found)
                    searchBtn.text = resources.getString(R.string.search_again)
                    searchButtonVisibility(true)
//                        Toast.makeText(
//                                applicationContext,
//                                "Connection request to $endpointId failed!",
//                                Toast.LENGTH_SHORT
//                        ).show()
                        // logW( "Connection request to $endpointId failed!")
                        // Nearby Connections failed to request the connection.
                    }
        }

        override fun onEndpointLost(endpointId: String) {
            Toast.makeText(applicationContext, "Endpoint: $endpointId lost!", Toast.LENGTH_SHORT)
                    .show()
            // logW("Endpoint: $endpointId lost!")
            // A previously discovered endpoint has gone away.
        }
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
//            Toast.makeText(applicationContext, "Connection with $p0 initiated.", Toast.LENGTH_SHORT)
//                .show()
            //  logD("Connection initiated : $p0 ,$p1")
            Nearby.getConnectionsClient(applicationContext).acceptConnection(p0, payloadCallback)
            // Automatically accept the connection on both sides.
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            // logD( "onConnectionResult")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    endpointIdSaved = endpointId
                    Log.v(LOG_TAG, "Connection initiated - endpoint: $endpointIdSaved")

//                    Toast.makeText(
//                            applicationContext,
//                            "Connected to $endpointId successfully",
//                            Toast.LENGTH_LONG
//                    ).show()
                    //   logD("Connected to $endpointId successfully")
                    if (userRole == ADVERTISER) {
                        val myInfo = "Вика"
                        Nearby.getConnectionsClient(applicationContext).sendPayload(
                            endpointId, Payload.fromBytes(myInfo.toByteArray()))
                    }
                }

                // We're connected! Can now start sending and receiving data.

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Nearby.getConnectionsClient(applicationContext).stopDiscovery()
                    searchButtonVisibility(true)
                    timer?.cancel()
                    setCountDownViewsVisibility(false)
                    searchingInProgressText.text = resources.getString(R.string.no_found)
                    searchBtn.text = resources.getString(R.string.search_again)
                    searchButtonVisibility(true)
                    Toast.makeText(
                            applicationContext,
                            "Connection attempt to $endpointId was rejected",
                            Toast.LENGTH_SHORT
                    ).show()
                    //   logW("Connection attempt to $endpointId was rejected")
                }
                // The connection was rejected by one or both sides.

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Nearby.getConnectionsClient(applicationContext).stopDiscovery()
                    searchButtonVisibility(true)
                    timer?.cancel()
                    setCountDownViewsVisibility(false)
                    searchingInProgressText.text = resources.getString(R.string.no_found)
                    searchBtn.text = resources.getString(R.string.search_again)
                    searchButtonVisibility(true)
                    Toast.makeText(
                            applicationContext,
                            "Connected attempt to $endpointId failed",
                            Toast.LENGTH_SHORT
                    ).show()
                    //   logW( "Connected attempt to $endpointId failed")
                }
                // The connection broke before it was able to be accepted.

                else -> {
                    Nearby.getConnectionsClient(applicationContext).stopDiscovery()
                    searchButtonVisibility(true)
                    timer?.cancel()
                    setCountDownViewsVisibility(false)
                    searchingInProgressText.text = resources.getString(R.string.no_found)
                    searchBtn.text = resources.getString(R.string.search_again)
                    searchButtonVisibility(true)
                    Toast.makeText(
                            applicationContext,
                            "Unknown status code",
                            Toast.LENGTH_SHORT
                    ).show()
                    //    logW( "Unknown status code")
                }
                // Unknown status code
            }
        }

        override fun onDisconnected(p0: String) {
            // logW("onDisconnected")
            // We've been disconnected from this endpoint. No more data can be
            // sent or received.
            Toast.makeText(
                applicationContext,
                "Disconnected",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            when (userRole) {
                DISCOVERER -> {
                        when (p1.type) {
                            Payload.Type.BYTES -> {
                                advertiserName = p1.asBytes()?.toString(Charset.defaultCharset())
                                Log.v("Look4", "advertiserName: $advertiserName")
                                if (isMobileNumber(advertiserName)) {
                                    searchingInProgressText.text = advertiserName
                                    shouldDisplayIncomeContactViews(false)
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
                    val receivedContact: String = p1.asBytes()!!.toString(Charset.defaultCharset())
                    val textArray = receivedContact.split(";").toTypedArray()
                    searchingInProgressText.visibility = View.VISIBLE
                    searchingInProgressText.text = textArray[0]
                    searchingInProgressText.isAllCaps = false
                    discovererPhoneNumber = textArray[1]
                    shouldDisplayIncomeContactViews(true)
                }
            }
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            if (p1.status == PayloadTransferUpdate.Status.SUCCESS && p1.totalBytes > 1000) {
                if (profile_image.drawable == null) {
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
        return string?.startsWith("+") ?: false
    }
}