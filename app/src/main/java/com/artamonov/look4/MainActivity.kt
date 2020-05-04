package com.artamonov.look4

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.android.synthetic.main.activity_main.look_text
import kotlinx.android.synthetic.main.activity_main.offline_text

class MainActivity : AppCompatActivity() {

companion object {

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private const val REQUEST_CODE_REQUIRED_PERMISSIONS = 1

    private val STRATEGY = Strategy.P2P_CLUSTER
    private const val LOG_TAG = "Look4"

    // private const val packgName = "com.artamonov.look4"
    // private val deviceId = "Player " + Random.nextInt(1, 10)
    private lateinit var deviceId: String

    private val advOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        look_text.setOnClickListener {
            startActivity(Intent(this, LookActivity::class.java))
        }

        if (ForegroundService.isAppInForeground) {
            offline_text.text = resources.getString(R.string.online_mode)
        }

        offline_text.setOnClickListener {
            if (offline_text.text == resources.getString(R.string.online_mode)) {
                stopService()
                Nearby.getConnectionsClient(applicationContext).stopAllEndpoints()
                offline_text.text = resources.getString(R.string.offline_mode)
                //  look_text.setTextColor(ContextCompat.getColor(this, R.color.grey))
                // look_text.isClickable = false
            } else {
                startService()
                look_text.isEnabled = true
                look_text.isClickable = true
                offline_text.text = resources.getString(R.string.online_mode)
                look_text.setTextColor(ContextCompat.getColor(this, R.color.green))
            }
        }
        // startServer()
    }

    private fun startService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        serviceIntent.putExtra("inputExtra", "Is enabled ...")
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopService() {
        val serviceIntent = Intent(this, ForegroundService::class.java)
        stopService(serviceIntent)
    }

    private fun startServer() {
        Nearby.getConnectionsClient(this).startAdvertising(
            deviceId,
            packageName,
            connectionLifecycleCallback,
            advOptions
        ).addOnSuccessListener {
            //  logD( "${LookActivity.deviceId} started advertising.")
        }.addOnFailureListener { exception ->
            //  logE("${LookActivity.deviceId} failed to advertise.", exception)
        }
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
//            Toast.makeText(applicationContext, "Connection with $p0 initiated.", Toast.LENGTH_SHORT)
//                .show()

            // Nearby.getConnectionsClient(applicationContext).acceptConnection(p0, payloadCallback)
            // Automatically accept the connection on both sides.
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            // logD( "onConnectionResult")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
//                    Toast.makeText(
//                        applicationContext,
//                        "Connected to $endpointId successfully",
//                        Toast.LENGTH_SHORT
//                    ).show()

                    //    logD("Connected to $endpointId successfully")
                    val myInfo = "Вика"
                    Nearby.getConnectionsClient(applicationContext).sendPayload(
                        endpointId, Payload.fromBytes(myInfo.toByteArray()))
                    Nearby.getConnectionsClient(applicationContext
                    !!).stopAdvertising()
                }
                // We're connected! Can now start sending and receiving data.

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
//                    Toast.makeText(
//                        applicationContext,
//                        "Connection attempt to $endpointId was rejected",
//                        Toast.LENGTH_SHORT
//                    ).show()

                    //    logW("Connection attempt to $endpointId was rejected")
                }

                // The connection was rejected by one or both sides.

                ConnectionsStatusCodes.STATUS_ERROR -> {
//                    Toast.makeText(
//                        applicationContext,
//                        "Connected attempt to $endpointId failed",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    //   logW( "Connected attempt to $endpointId failed")
                }
                // The connection broke before it was able to be accepted.

                else -> {
//                    Toast.makeText(
//                        applicationContext,
//                        "Unknown status code",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    // logW( "Unknown status code")
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

//
//    private val payloadCallback = object : PayloadCallback() {
//        override fun onPayloadReceived(p0: String, p1: Payload) {
//            val receivedContact: String = p1.asBytes()!!.toString(Charset.defaultCharset())
//        }
//
//        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
//        }
//    }
}
