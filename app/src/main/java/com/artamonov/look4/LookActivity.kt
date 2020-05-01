package com.artamonov.look4

import android.Manifest.permission.*
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
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
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

       // private const val packgName = "com.artamonov.look4"

//        private val deviceId = "Player " + Random.nextInt(1, 10)
        private lateinit var deviceId: String

        private val advOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        private val discOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()

    }

    //lateinit var connClient: ConnectionsClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_look)
      //  connClient = Nearby.getConnectionsClient(this)

        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
            // mDebugLogView.movementMethod = ScrollingMovementMethod()
     //   mDebugLogView.setTextIsSelectable(true)
       // advertButton.setOnClickListener { startServer() }
        searchBtn.setOnClickListener { startClient() }
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
        searchBtn.visibility = View.GONE
        searchingInProgressText.visibility = View.VISIBLE
        Nearby.getConnectionsClient(applicationContext).startDiscovery(packageName, endpointDiscoveryCallback, discOptions)
                .addOnSuccessListener {
          //          logD( "$deviceId started discovering.")
                }.addOnFailureListener { e ->
                    // We're unable to start discovering.
           //         logE("We're unable to start discovering.", e)
                }
    }

    private val endpointDiscoveryCallback: EndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // An endpoint was found. We request a connection to it.
        //    logD( "onEndpointFound: endpointId: $endpointId")
            searchingInProgressText.text = "We have found a device! Let me connect to it ..."


            Nearby.getConnectionsClient(applicationContext).requestConnection(info.endpointName + ".1", endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener {
                        Toast.makeText(
                                applicationContext,
                                "Requested connection to $endpointId !",
                                Toast.LENGTH_SHORT
                        ).show()
                //        logD( "Requested connection to $endpointId !")
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
                //        logW( "Connection request to $endpointId failed!")
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
                ConnectionsStatusCodes.STATUS_OK ->  {
                    Toast.makeText(
                            applicationContext,
                            "Connected to $endpointId successfully",
                            Toast.LENGTH_SHORT
                    ).show()
                 //   logD("Connected to $endpointId successfully")
                    Nearby.getConnectionsClient(applicationContext
                    !!).stopAdvertising()
                }
                        // We're connected! Can now start sending and receiving data.

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED ->  {
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
            //logW("onDisconnected")
            // We've been disconnected from this endpoint. No more data can be
            // sent or received.
            }
    }




    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            val receivedText: String = p1.asBytes()!!.toString(Charset.defaultCharset())
            searchingInProgressText.text = receivedText
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
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