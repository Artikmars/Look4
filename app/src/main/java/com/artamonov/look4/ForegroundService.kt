package com.artamonov.look4

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.Service
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.artamonov.look4.utils.UserRole
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import java.nio.charset.Charset

class ForegroundService : Service() {

    var endpointIdSaved: String? = null
    var userRole = UserRole.ADVERTISER
    private var advertiserName: String? = null
    private var discovererPhoneNumber: String? = null
    private var discovererName: String? = null
    lateinit var deviceId: String
    private val STRATEGY = Strategy.P2P_CLUSTER
    val advOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
    var notification: Notification? = null
    var notificationManager: NotificationManager? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val input = intent.getStringExtra("inputExtra")
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Advertising")
                .setSmallIcon(R.drawable.ic_o_1)
                .setContentText(input)
                .setContentIntent(pendingIntent)
                .build()
        //do heavy work on a background thread
        // stopSelf();
        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        startServer()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        isAppInForeground = false
        super.onDestroy()
    }

    private fun startServer() {
        Nearby.getConnectionsClient(this).startAdvertising(
            deviceId,
            packageName,
            connectionLifecycleCallback,
            advOptions
        ).addOnSuccessListener {
            startForeground(1, notification)
            isAppInForeground = true
        }.addOnFailureListener { exception ->
//            Toast.makeText(applicationContext, "Couldn't advertise because of: $exception", Toast.LENGTH_SHORT)
//                .show()
        }
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
//            Toast.makeText(applicationContext, "Connection with $p0 initiated.", Toast.LENGTH_SHORT)
//                .show()
            Nearby.getConnectionsClient(applicationContext).acceptConnection(p0, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    endpointIdSaved = endpointId
//                    Toast.makeText(
//                        applicationContext,
//                        "Connected to $endpointId successfully",
//                        Toast.LENGTH_LONG
//                    ).show()

                    val myInfo = "Вика"
                    Nearby.getConnectionsClient(applicationContext).sendPayload(
                        endpointId, Payload.fromBytes(myInfo.toByteArray()))
                }

                // We're connected! Can now start sending and receiving data.

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
//                    Toast.makeText(
//                        applicationContext,
//                        "Connection attempt to $endpointId was rejected",
//                        Toast.LENGTH_SHORT
//                    ).show()
                }
                // The connection was rejected by one or both sides.

                ConnectionsStatusCodes.STATUS_ERROR -> {
//                    Toast.makeText(
//                        applicationContext,
//                        "Connected attempt to $endpointId failed",
//                        Toast.LENGTH_SHORT
//                    ).show()
                }
                // The connection broke before it was able to be accepted.

                else -> {
//                    Toast.makeText(
//                        applicationContext,
//                        "Unknown status code",
//                        Toast.LENGTH_SHORT
//                    ).show()
                }
                // Unknown status code
            }
        }

        override fun onDisconnected(p0: String) {
            // We've been disconnected from this endpoint. No more data can be
            // sent or received.
        }
    }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            val receivedContact: String = p1.asBytes()!!.toString(Charset.defaultCharset())
            val textArray = receivedContact.split(";").toTypedArray()
            //  Toast.makeText(applicationContext, textArray[0], Toast.LENGTH_SHORT).show()
            //  Toast.makeText(applicationContext, textArray[1], Toast.LENGTH_SHORT).show()
            discovererName = textArray[0]
            discovererPhoneNumber = textArray[1]
            //createNotificationChannel(CONTACT_REQUEST_CHANNEL_ID)
            showPendingContactNotification(2)
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
        }
    }

    private fun showPendingContactNotification(notificationId: Int) {
        // Create an explicit intent for an Activity in your app
        val intent = Intent(this, LookActivity::class.java)
        //.apply {
        //  flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        //  }
        intent.putExtra("discovererName", discovererName)
        intent.putExtra("discovererPhoneNumber", discovererPhoneNumber)
        intent.putExtra("endpointId", endpointIdSaved)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Look4")
            .setSmallIcon(R.drawable.ic_o_2)
            .setContentText("You have received a contact request")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager?.notify(notificationId, builder.build())

//        with(NotificationManagerCompat.from(this)) {
//            notify(notificationId, builder.build())
//        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.enableVibration(true)
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
        var isAppInForeground: Boolean = false
    }
}