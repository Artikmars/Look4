package com.artamonov.look4.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.Service
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.artamonov.look4.look.LookActivity.Companion.LOG_TAG
import com.artamonov.look4.main.MainActivity
import com.artamonov.look4.R
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.NotificationHandler
import com.artamonov.look4.utils.UserGender
import com.artamonov.look4.utils.UserRole.Companion.ADVERTISER
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy.P2P_POINT_TO_POINT
import java.io.File
import java.nio.charset.Charset

class ForegroundService : Service() {

    var endpointIdSaved: String? = null
    private var advertiserPhoneNumber: String? = null
    private var discovererPhoneNumber: String? = null
    private var discovererName: String? = null
    private var discovererFilePath: String? = null
    lateinit var deviceId: String
    private val STRATEGY = P2P_POINT_TO_POINT
    private var isGenderValid: Boolean = true
    private val advOptions: AdvertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
    var notification: Notification? = null
    var notificationManager: NotificationManager? = null
    private var newFile: File? = null
    private var file: File? = null
    private var filePayload: Payload? = null
    private var bytePayload: Payload? = null
    private var connectionClient: ConnectionsClient? = null

    private lateinit var notificationHandler: NotificationHandler

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        connectionClient = Nearby.getConnectionsClient(applicationContext)
        val input = intent.getStringExtra("inputExtra")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0,
            notificationIntent, FLAG_UPDATE_CURRENT)
        notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Online")
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
        connectionClient?.stopAllEndpoints()
        isAppInForeground = false
        super.onDestroy()
    }

    private fun startServer() {
        connectionClient?.startAdvertising(
            deviceId,
            packageName,
            connectionLifecycleCallback,
            advOptions
        )?.addOnSuccessListener {
            startForeground(1, notification)
            isAppInForeground = true
            PreferenceHelper.updateRole(ADVERTISER)
        }
            ?.addOnFailureListener { e ->
            isAppInForeground = false
            Toast.makeText(this, "$e", Toast.LENGTH_LONG).show()
            stopForeground(true)
            stopSelf()
        }
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
            connectionClient?.acceptConnection(p0, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    endpointIdSaved = endpointId
                    Log.v(LOG_TAG, "in service onConnectionResult: $endpointId")
                    //  bytePayload = Payload.fromBytes(preferenceHelper.getUserProfile()?.name!!.toByteArray())
                    bytePayload = Payload.fromBytes((PreferenceHelper.getUserProfile()?.name +
                            ";" + PreferenceHelper.getUserProfile()?.gender).toByteArray())
                    connectionClient?.sendPayload(endpointId, bytePayload!!)

                    // Toast.makeText(applicationContext, "userImagePath: $advertiserFilePath", Toast.LENGTH_LONG).show()
                    PreferenceHelper.getUserProfile()?.imagePath?.let {
                        val imageUri = Uri.parse(PreferenceHelper.getUserProfile()?.imagePath)
                        // val imageUri = URI.create(userImagePath!!)
                        Log.v("Look4", "imageUri: $imageUri")
                        //Toast.makeText(applicationContext, "imageUri: $imageUri", Toast.LENGTH_LONG).show()
                        val pfd: ParcelFileDescriptor? = contentResolver.openFileDescriptor(imageUri, "r")
                        // val file = File(imageUri.path!!)
                        Log.v("Look4", "imageUri.path: ${imageUri.path}")
                        //  Log.v("Look4", "file: $file")

                        filePayload = Payload.fromFile(pfd!!)
                        connectionClient?.sendPayload(endpointId, filePayload!!)
                    }
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
            when (p1.type) {
                Payload.Type.BYTES -> {
                    val receivedContact: String = p1.asBytes()!!.toString(Charset.defaultCharset())
                    if (";" in receivedContact) {
                        val textArray = receivedContact.split(";").toTypedArray()
                        discovererName = textArray[0]
                        discovererPhoneNumber = textArray[1]
                        isGenderValid(textArray[2])
                    } else { advertiserPhoneNumber = receivedContact }
                }
                Payload.Type.FILE -> {
                    file = p1.asFile()?.asJavaFile()
                    file?.renameTo(File(file?.parentFile, "look4.jpg"))
                    newFile = File(file?.parentFile, "look4.jpg")
                }
            }
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            if (!isGenderValid) { return }
            if (p1.status == PayloadTransferUpdate.Status.SUCCESS && p1.totalBytes > 1000 && advertiserPhoneNumber == null &&
                p1.payloadId != bytePayload?.id && p1.payloadId != filePayload?.id) {
                //  Toast.makeText(applicationContext, "discovererFilePath: $discovererFilePath", Toast.LENGTH_LONG).show(
                discovererFilePath = newFile?.toString()
                showPendingContactNotification(2)
            } else if (p1.status == PayloadTransferUpdate.Status.SUCCESS && advertiserPhoneNumber != null) {
                showPendingContactNotification(3)
            }
        }
    }

    private fun showPendingContactNotification(notificationId: Int) {
        // Create an explicit intent for an Activity in your app
        notificationHandler = NotificationHandler(discovererName,
            discovererPhoneNumber, discovererFilePath, advertiserPhoneNumber, endpointIdSaved)

        val intent = notificationHandler.createIntent(this)
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
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun isGenderValid(advertiserGender: @UserGender.AnnotationUserGender String?): Boolean {
        when (PreferenceHelper.getUserProfile()?.lookGender) {
            UserGender.MALE -> {
                isGenderValid = advertiserGender == UserGender.MALE
                return isGenderValid
            }
            UserGender.FEMALE -> {
                isGenderValid = advertiserGender == UserGender.FEMALE
                return isGenderValid
            }
            UserGender.ALL -> {
                isGenderValid = true
                return isGenderValid
            }
        }
        isGenderValid = true
        return isGenderValid
    }

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
        var isAppInForeground: Boolean = false
    }
}