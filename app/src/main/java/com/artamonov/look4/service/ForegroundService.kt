package com.artamonov.look4.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.artamonov.look4.R
import com.artamonov.look4.data.database.ContactRequest
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.look.LookActivity.Companion.LOG_TAG
import com.artamonov.look4.look.populateScanningView
import com.artamonov.look4.main.MainActivity
import com.artamonov.look4.utils.*
import com.artamonov.look4.utils.UserGender.Companion.FEMALE
import com.artamonov.look4.utils.UserGender.Companion.MALE
import com.artamonov.look4.utils.UserRole.Companion.ADVERTISER
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.Strategy.P2P_STAR
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.nio.charset.Charset
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundService : Service() {

    var endpointIdSaved: String? = null
    private var advertiserPhoneNumber: String? = null
    private var discovererPhoneNumber: String? = null
    private var discovererName: String? = null
    private var discovererFilePath: String? = null
    private lateinit var deviceId: String
    private val STRATEGY = P2P_STAR
    private var isGenderValid: Boolean = true
    private val advOptions: AdvertisingOptions =
        AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
    private val discOptions by lazy { DiscoveryOptions.Builder().setStrategy(STRATEGY).build() }
    var notification: Notification? = null
    private lateinit var notificationManager: NotificationManager
    private var newFile: File? = null
    private var file: File? = null
    private var filePayload: Payload? = null
    private lateinit var bytePayload: Payload
    private var soundUri: Uri? = null

    private lateinit var notificationHandler: NotificationHandler

    @Inject lateinit var prefs: PreferenceHelper
    @Inject lateinit var connectionClient: ConnectionsClient
    @Inject lateinit var firebaseCrashlytics: FirebaseCrashlytics

    override fun onCreate() {
        super.onCreate()
        isForegroundServiceRunning = true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        deviceId = UUID.randomUUID().toString()
        startServer(intent)
        startClient()
        // do heavy work on a background thread
        // stopSelf();
        return START_NOT_STICKY
    }

    private fun createNotification(intent: Intent): Notification {
        val input = intent.getStringExtra("inputExtra")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        soundUri = Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                    packageName + "/" + R.raw.look4_notification_sound
        )
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.main_online_mode))
            .setSmallIcon(R.drawable.ic_o_1)
            .setContentText(input)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionClient.stopAllEndpoints()
        isForegroundServiceRunning = false
        val broadcastIntent = Intent(SERVICE_IS_DESTROYED)
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }

    private fun startServer(intent: Intent) {
        connectionClient.startAdvertising(
            deviceId, packageName, connectionLifecycleCallback,
            advOptions
        )?.addOnSuccessListener {
            startForeground(1, createNotification(intent))
            prefs.updateRole(ADVERTISER)
            val broadcastIntent = Intent(ADVERTISING_SUCCEEDED_EVENT)
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
            firebaseCrashlytics.log("Advertising has been started")
        }?.addOnFailureListener { e ->
            val broadcastIntent = Intent(ADVERTISING_FAILED_EVENT)
            broadcastIntent.putExtra(ADVERTISING_FAILED, e)
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
            endpointIdSaved.disconnectFromEndpoint(connectionClient)
                Toast.makeText(this, "$e", Toast.LENGTH_LONG).show()
                stopSelf()
            }
    }


    private fun startClient() {
        endpointDiscoveryCallback?.let {
            connectionClient.startDiscovery(packageName, it, discOptions)
                ?.addOnSuccessListener {
                    firebaseCrashlytics.log("Discovery has been started")
                }?.addOnFailureListener { e ->
                    // We're unable to start discovering.
                }
        }
    }


    private var endpointDiscoveryCallback: EndpointDiscoveryCallback? =
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    connectionClient.requestConnection("test", endpointId, connectionLifecycleCallback)
                        ?.addOnFailureListener { e ->
                            // We're unable to connect.
                        }
            }

            override fun onEndpointLost(endpointId: String) {
                firebaseCrashlytics.log("onEndpointLost: $endpointId")
            }
        }


    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
                connectionClient.acceptConnection(p0, payloadCallback)
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                endpointIdSaved = endpointId
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        Log.v(LOG_TAG, "in service onConnectionResult: $endpointId")
                        //  bytePayload = Payload.fromBytes(preferenceHelper.getUserProfile()?.name!!.toByteArray())
                        bytePayload = Payload.fromBytes(
                            (prefs.getUserProfile().name +
                                    ";" + prefs.getUserProfile().gender).toByteArray()
                        )
                        connectionClient.sendPayload(endpointId, bytePayload)

                        // Toast.makeText(applicationContext, "userImagePath: $advertiserFilePath", Toast.LENGTH_LONG).show()
                        prefs.getUserProfile().imagePath.let {
                            val imageUri = Uri.parse(prefs.getUserProfile().imagePath)
                            // val imageUri = URI.create(userImagePath!!)
                            Log.v("Look4", "imageUri: $imageUri")
                            // Toast.makeText(applicationContext, "imageUri: $imageUri", Toast.LENGTH_LONG).show()
                            val pfd: ParcelFileDescriptor? =
                                contentResolver.openFileDescriptor(imageUri, "r")
                            // val file = File(imageUri.path!!)
                            Log.v("Look4", "imageUri.path: ${imageUri.path}")
                            //  Log.v("Look4", "file: $file")

                            filePayload = Payload.fromFile(pfd!!)
                            connectionClient.sendPayload(endpointId, filePayload!!)
                        }
                    }

                    // We're connected! Can now start sending and receiving data.

                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        endpointIdSaved.disconnectFromEndpoint(connectionClient)
//                    Toast.makeText(
//                        applicationContext,
//                        "Connection attempt to $endpointId was rejected",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    }
                    // The connection was rejected by one or both sides.

                    ConnectionsStatusCodes.STATUS_ERROR -> {
                        endpointIdSaved.disconnectFromEndpoint(connectionClient)
//                    Toast.makeText(
//                        applicationContext,
//                        "Connected attempt to $endpointId failed",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    }
                    // The connection broke before it was able to be accepted.

                    else -> {
                        endpointIdSaved.disconnectFromEndpoint(connectionClient)
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
                endpointIdSaved.disconnectFromEndpoint(connectionClient)
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
                    } else {
                        advertiserPhoneNumber = receivedContact
                    }
                }
                Payload.Type.FILE -> {
                    file = p1.asFile()?.asJavaFile()
                    file?.renameTo(File(file?.parentFile, "look4.jpg"))
                    newFile = File(file?.parentFile, "look4.jpg")
                }
            }
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            require(isGenderValid) { "Gender is not valid" }
            if (!isGenderValid || p1.status != PayloadTransferUpdate.Status.SUCCESS) {
                return
            }
            if (p1.totalBytes > 1000 && advertiserPhoneNumber == null &&
                p1.payloadId != bytePayload.id && p1.payloadId != filePayload?.id
            ) {
                //  Toast.makeText(applicationContext, "discovererFilePath: $discovererFilePath", Toast.LENGTH_LONG).show(
                discovererFilePath = newFile?.toString()

                showPendingContactNotification(2)
            } else advertiserPhoneNumber?.let { showPendingContactNotification(3) }
        }
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    private fun showPendingContactNotification(notificationId: Int) {
        requireNotNull(endpointIdSaved) { "Endpoint ID is null" }
        // Create an explicit intent for an Activity in your app
        notificationHandler = NotificationHandler(
            discovererName, discovererPhoneNumber,
            discovererFilePath, advertiserPhoneNumber, endpointIdSaved!!
        )

        val intent = notificationHandler.createIntent(this)

        prefs.saveContactRequest(
            ContactRequest(
                name = discovererName, phoneNumber = discovererPhoneNumber,
                filePath = discovererFilePath, advertiserPhoneNumber = advertiserPhoneNumber,
                endpointId = endpointIdSaved
            )
        )

        if (isAppInForeground() && notificationId == 2) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }

        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, intent, FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Look4")
            .setSmallIcon(R.drawable.ic_o_2)
            .setContentText(getString(R.string.service_new_request))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(soundUri)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
//        with(NotificationManagerCompat.from(this)) {
//            notify(notificationId, builder.build())
//        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.enableVibration(true)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            serviceChannel.setSound(soundUri, audioAttributes)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun isGenderValid(advertiserGender: @UserGender.AnnotationUserGender String?): Boolean {
        when (prefs.getUserProfile().lookGender) {
            MALE -> {
                return advertiserGender == MALE
            }
            FEMALE -> {
                return advertiserGender == FEMALE
            }
            UserGender.ALL -> {
                return true
            }
        }
        return true
    }

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
        var isForegroundServiceRunning: Boolean = false
        const val ADVERTISING_FAILED = "ADVERTISING_FAILED"
        const val ADVERTISING_FAILED_EVENT = "ADVERTISING_FAILED_EVENT"
        const val ADVERTISING_SUCCEEDED_EVENT = "ADVERTISING_SUCCEEDED_EVENT"
        const val SERVICE_IS_DESTROYED = "SERVICE_IS_DESTROYED"
    }
}
