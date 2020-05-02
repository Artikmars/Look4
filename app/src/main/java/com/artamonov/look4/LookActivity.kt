package com.artamonov.look4

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.Manifest.permission.CHANGE_WIFI_STATE
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.artamonov.look4.utils.UserRole.Companion.ADVERTISER
import com.artamonov.look4.utils.UserRole.Companion.DISCOVERER
import com.bumptech.glide.Glide
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
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
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.Charset

class LookActivity : AppCompatActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            BLUETOOTH,
            BLUETOOTH_ADMIN,
            ACCESS_WIFI_STATE,
            CHANGE_WIFI_STATE,
            ACCESS_COARSE_LOCATION
        )

        private const val REQUEST_CODE_REQUIRED_PERMISSIONS = 1

        private val STRATEGY = P2P_CLUSTER
        private const val LOG_TAG = "Look4"
        private var endpointIdSaved: String? = null
        private var userRole = ADVERTISER
        private var discovererPhoneNumber: String? = null
        private var advertiserName: String? = null
        private lateinit var deviceId: String

        private val advOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        private val discOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_look)
        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        searchBtn.setOnClickListener { startClient() }

        Glide
            .with(profile_image)
            .load(R.drawable.ic_face_black_18dp)
            .placeholder(R.drawable.ic_face_black_18dp)
            .into(profile_image)

        startServer()

        no_button.setOnClickListener {
            Nearby.getConnectionsClient(this).stopAllEndpoints()
            setYesNoButtonsVisibility(false)
            setSearchButttonVisibility(true)
        }

        yes_button.setOnClickListener {
            when (userRole) {
                ADVERTISER -> {
                    searchingInProgressText.text = "Congratulations! Here is the phone number: " + discovererPhoneNumber
                    setYesNoButtonsVisibility(false)
                    setSearchButttonVisibility(true)
                    val phoneNumber = "+496969696969"
                    Toast.makeText(applicationContext, "Endpoint: $endpointIdSaved", Toast.LENGTH_LONG)
                        .show()
                    Log.v(LOG_TAG, "Endpoint: $endpointIdSaved")
                    endpointIdSaved?.let {
                        Nearby.getConnectionsClient(applicationContext).sendPayload(
                            endpointIdSaved!!, Payload.fromBytes(phoneNumber.toByteArray())).addOnFailureListener { e ->
                            Toast.makeText(applicationContext, "Error: $e", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }

                DISCOVERER -> {
                    val phoneNumber = "Андрей;+4915256848384"
                    Toast.makeText(applicationContext, "Endpoint: $endpointIdSaved", Toast.LENGTH_SHORT)
                        .show()
                    endpointIdSaved?.let {
                        Nearby.getConnectionsClient(applicationContext).sendPayload(
                            endpointIdSaved!!, Payload.fromBytes(phoneNumber.toByteArray())).addOnFailureListener { e ->
                            Toast.makeText(applicationContext, "Error: $e", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                    setYesNoButtonsVisibility(false)
                    setSearchButttonVisibility(true)
                }
            }
        }

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

    private fun startServer() {
        Nearby.getConnectionsClient(this).startAdvertising(
                deviceId,
                packageName,
                connectionLifecycleCallback,
                advOptions
        ).addOnSuccessListener {
            //    logD( "$deviceId started advertising.")
        }.addOnFailureListener { exception ->
            //   logE("$deviceId failed to advertise.", exception)
        }
    }

    private fun startClient() {
        setSearchButttonVisibility(false)
        searchingInProgressText.visibility = View.VISIBLE
        Nearby.getConnectionsClient(applicationContext).startDiscovery(packageName, endpointDiscoveryCallback, discOptions)
                .addOnSuccessListener {
                    // logD( "$deviceId started discovering.")
                    userRole = DISCOVERER
                }.addOnFailureListener { e ->
                // We're unable to start discovering.
                // logE("We're unable to start discovering.", e)
                }
    }

    private val endpointDiscoveryCallback: EndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // An endpoint was found. We request a connection to it.
            // logD( "onEndpointFound: endpointId: $endpointId")
            // endpointIdSaved = endpointId
            searchingInProgressText.text = "We have found a device! Let me connect to it ..."

            Nearby.getConnectionsClient(applicationContext).requestConnection(info.endpointName + ".1", endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener {
                        Toast.makeText(
                                applicationContext,
                                "Requested connection to $endpointId !",
                                Toast.LENGTH_SHORT
                        ).show()
                        // logD( "Requested connection to $endpointId !")
                        searchingInProgressText.text = "You are connected!"

                        // We successfully requested a connection. Now both sides
                        // must accept before the connection is established.
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                                applicationContext,
                                "Connection request to $endpointId failed!",
                                Toast.LENGTH_SHORT
                        ).show()
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
            Toast.makeText(applicationContext, "Connection with $p0 initiated.", Toast.LENGTH_SHORT)
                .show()
            //  logD("Connection initiated : $p0 ,$p1")
            Nearby.getConnectionsClient(applicationContext).acceptConnection(p0, payloadCallback)
            // Automatically accept the connection on both sides.
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            // logD( "onConnectionResult")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    endpointIdSaved = endpointId
                    Toast.makeText(
                            applicationContext,
                            "Connected to $endpointId successfully",
                            Toast.LENGTH_LONG
                    ).show()
                    //   logD("Connected to $endpointId successfully")
                    if (userRole == ADVERTISER) {
                        val myInfo = "Вика"
                        Nearby.getConnectionsClient(applicationContext).sendPayload(
                            endpointId, Payload.fromBytes(myInfo.toByteArray()))
                    }
                    //  Nearby.getConnectionsClient(applicationContex!!).stopAdvertising()
                }

                // We're connected! Can now start sending and receiving data.

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Toast.makeText(
                            applicationContext,
                            "Connection attempt to $endpointId was rejected",
                            Toast.LENGTH_SHORT
                    ).show()
                    //   logW("Connection attempt to $endpointId was rejected")
                }
                // The connection was rejected by one or both sides.

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Toast.makeText(
                            applicationContext,
                            "Connected attempt to $endpointId failed",
                            Toast.LENGTH_SHORT
                    ).show()
                    //   logW( "Connected attempt to $endpointId failed")
                }
                // The connection broke before it was able to be accepted.

                else -> {
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
        }
    }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            when (userRole) {
                DISCOVERER -> {
                    if (advertiserName != null) {
                        //  Toast.makeText(applicationContext, "advertiserName: " + advertiserName, Toast.LENGTH_SHORT).show()
                        val advertisersPhoneNumber = p1.asBytes()!!.toString(Charset.defaultCharset())
                        searchingInProgressText.text = advertisersPhoneNumber
                    } else {
                        //  Toast.makeText(applicationContext, "advertiserName: " + advertiserName, Toast.LENGTH_SHORT).show()
                        advertiserName = p1.asBytes()!!.toString(Charset.defaultCharset())
                        searchingInProgressText.text = advertiserName
                        setYesNoButtonsVisibility(true)
                    }
                }
                ADVERTISER -> {
                    // endpointIdSaved = p0
                    setSearchButttonVisibility(false)
                    val receivedContact: String = p1.asBytes()!!.toString(Charset.defaultCharset())
                    val textArray = receivedContact.split(";").toTypedArray()
                    Toast.makeText(applicationContext, textArray[0], Toast.LENGTH_SHORT).show()
                    Toast.makeText(applicationContext, textArray[1], Toast.LENGTH_SHORT).show()
                    searchingInProgressText.visibility = View.VISIBLE
                    searchingInProgressText.text = textArray[0]
                    discovererPhoneNumber = textArray[1]
                    setYesNoButtonsVisibility(true)
                }
            }
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
        }
    }

    private fun setYesNoButtonsVisibility(isVisible: Boolean) {
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

    private fun setSearchButttonVisibility(isVisible: Boolean) {
        if (isVisible) {
            searchBtn.visibility = View.VISIBLE
            searchBtn.visibility = View.VISIBLE
        } else {
            searchBtn.visibility = View.GONE
            searchBtn.visibility = View.GONE
        }
    }

    protected fun logD(msg: String) {
        Log.d(LOG_TAG, msg)
        appendToLogs(toColor(msg, resources.getColor(R.color.colorPrimary)))
    }

    protected fun logW(msg: String) {
        Log.w(LOG_TAG, msg)
        appendToLogs(toColor(msg, resources.getColor(R.color.colorAccent)))
    }

    protected fun logW(msg: String, e: Throwable?) {
        Log.w(LOG_TAG, msg, e)
        appendToLogs(toColor(msg, resources.getColor(R.color.colorAccent)))
    }

    protected fun logE(msg: String, e: Throwable?) {
        Log.e(LOG_TAG, msg, e)
        appendToLogs(toColor(msg, resources.getColor(R.color.colorAccent)))
    }

    private fun appendToLogs(msg: CharSequence) {
        mDebugLogView.append("\n")
        mDebugLogView.append(DateFormat.format("hh:mm", System.currentTimeMillis()).toString() + ": ")
        mDebugLogView.append(msg)
    }

    private fun toColor(msg: String, color: Int): CharSequence {
        val spannable = SpannableString(msg)
        spannable.setSpan(ForegroundColorSpan(color), 0, msg.length, 0)
        return spannable
    }
}