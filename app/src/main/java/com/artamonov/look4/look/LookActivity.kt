package com.artamonov.look4.look

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.CHANGE_WIFI_STATE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.ActivityOptions
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
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.artamonov.look4.PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.database.User
import com.artamonov.look4.main.MainActivity
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactAdvertiserUnseenState
import com.artamonov.look4.utils.UserRole.Companion.ADVERTISER
import com.artamonov.look4.utils.UserRole.Companion.DISCOVERER
import com.artamonov.look4.utils.set
import com.artamonov.look4.utils.setSafeOnClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.crashlytics.android.Crashlytics
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy.P2P_POINT_TO_POINT
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.activity_look.*

class LookActivity : BaseActivity(R.layout.activity_look) {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            BLUETOOTH,
            BLUETOOTH_ADMIN,
            ACCESS_WIFI_STATE,
            CHANGE_WIFI_STATE,
            ACCESS_COARSE_LOCATION,
            ACCESS_FINE_LOCATION,
            READ_EXTERNAL_STORAGE
        )

        private const val REQUEST_CODE_REQUIRED_PERMISSIONS = 1
        private val STRATEGY = P2P_POINT_TO_POINT
        const val LOG_TAG = "Look4"
        lateinit var deviceId: String

        private lateinit var connectionClient: ConnectionsClient
        private val discOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()

        private var user: User? = null
        private var timer: CountDownTimer? = null
    }

    private val lookViewModel: LookViewModel by viewModels()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        lookViewModel.handleNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectionClient = Nearby.getConnectionsClient(this)
        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        lookViewModel.state.observe(this, Observer { bindViewState(it) })
        lookViewModel.user.observe(this, Observer { user = it })

        searchBtn.setOnClickListener { startClient() }
        look_back.setOnClickListener { closeActivity() }

        no_button.setSafeOnClickListener {
            lookViewModel.endpointIdSaved?.let { connectionClient.disconnectFromEndpoint(it) }
            closeActivity()
        }

        yes_button.setSafeOnClickListener {
            requireNotNull(lookViewModel.endpointIdSaved) { "Endpoint is null" }
            when (prefs.getUserProfile().role) {
                ADVERTISER -> {
                    lookViewModel.savePhoneNumberToDB(
                        lookViewModel.discovererPhoneNumber,
                        ADVERTISER
                    )

                    lookViewModel.endpointIdSaved?.let { endpoint ->
                        connectionClient.sendPayload(
                            endpoint, Payload
                                .fromBytes(
                                    prefs.getUserProfile().phoneNumber?.toByteArray()
                                        ?: "".toByteArray()
                                )
                        )
                            ?.addOnFailureListener { e ->
                                showSnackbarError(getString(R.string.look_error_connection_is_lost))
                                Crashlytics.logException(e)
                                connectionClient.disconnectFromEndpoint(endpoint)
                                closeActivity()
                            }?.addOnSuccessListener {
                                connectionClient.disconnectFromEndpoint(endpoint)
                                contactAdvertiserUnseenState.set(newValue = ContactUnseenState.EnabledState)
                                closeActivity()
                            }
                    }
                }
                DISCOVERER -> {
                    connectionClient.stopDiscovery()
                    val pfd: ParcelFileDescriptor? = contentResolver.openFileDescriptor(
                        Uri.parse(prefs.getUserProfile().imagePath!!), "r"
                    )
                    val pFilePayload = Payload.fromFile(pfd!!)
                    connectionClient.sendPayload(
                        lookViewModel.endpointIdSaved!!,
                        Payload.fromBytes("${getUserProfile()?.name};${getUserProfile()?.phoneNumber};${getUserProfile()?.gender}".toByteArray())
                    )?.addOnFailureListener { e ->
                        Crashlytics.logException(e)
                        showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                        closeActivity()
                    }
                    connectionClient.sendPayload(
                        lookViewModel.endpointIdSaved!!, pFilePayload
                    )?.addOnFailureListener { e ->
                        showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                        Crashlytics.logException(e)
                    }

                    finish()
                }
            }
        }

        checkForPermissions()

//        disconnectButton.setOnClickListener {
//            connClient.apply {
//             stopAdvertising()
//               stopDiscovery()
//                stopAllEndpoints()
//           }
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer = null
        endpointDiscoveryCallback = null
        connectionLifecycleCallback = null
    }

    private fun bindViewState(viewState: LookState) {
        when (viewState) {
            is LookState.DefaultState -> {
                Crashlytics.log("State: Default")
                populateDefaultView()
            }
            is LookState.NoFoundState -> {
                Crashlytics.log("State: No Found")
                populateNoFoundView()
            }
            is LookState.SearchState -> {
                Crashlytics.log("State: Search")
                startClient()
            }
            is LookState.PhoneNumberReceived -> {
                Crashlytics.log("State: PhoneNumberReceived")
                lookViewModel.endpointIdSaved?.let { connectionClient.disconnectFromEndpoint(it) }
                showSnackbarWithAction()
            }
            is LookState.SucceededAdvertiserIsFoundState<*> -> {
                Crashlytics.log("State: SucceededAdvertiserIsFoundState")
                populateSucceedView()
            }
            is LookState.SucceededDiscoverIsFoundState<*> -> {
                Crashlytics.log("State: SucceededDiscoverIsFoundState")
                populateSucceedView()
                searchingInProgressText.visibility = VISIBLE
                searchingInProgressText.text = lookViewModel.discovererName
                profile_image.setImageDrawable(Drawable.createFromPath(lookViewModel.discovererFilePath))
            }
        }
    }

    private fun closeActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivity(
                Intent(this, MainActivity::class.java),
                ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
            )
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }

        finish()
    }

    private fun getUserProfile(): User? {
        return prefs.getUserProfile()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_REQUIRED_PERMISSIONS
            )
        }
    }

    private fun checkForPermissions() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Crashlytics.log("Permission is missing in checkForPermissions(): $ACCESS_COARSE_LOCATION")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_COARSE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION
            )
        } else {
            Crashlytics.log("Permission is given in checkForPermissions(): $ACCESS_COARSE_LOCATION")
            lookViewModel.startDiscovering()
            onNewIntent(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    lookViewModel.startDiscovering()
                    onNewIntent(intent)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish()
                    showSnackbarError(R.string.error_permissions_are_not_granted_for_discovering)
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    /** Returns true if the app was granted all the permissions. Otherwise, returns false.  */
    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Crashlytics.log("Permission is missing: $permission")
                return false
            }
        }
        return true
    }

    private fun startClient() {
        startTimer()
        populateScanningView()
        lookViewModel.endpointIdSaved?.let { connectionClient.disconnectFromEndpoint(it) }
        endpointDiscoveryCallback?.let {
            connectionClient.startDiscovery(packageName, it, discOptions)
                ?.addOnSuccessListener {
                    lookViewModel.updateRole(DISCOVERER)
                    Crashlytics.log("Discovery has been started")
                }?.addOnFailureListener { e ->
                    // We're unable to start discovering.
                    showSnackbarError(getString(R.string.look_error_scanning_can_not_be_started))
                    handleFailedResponse(e)
                    //  closeActivity()
                }
        }
    }

    @Synchronized
    private fun startTimer() {
        timer = object : CountDownTimer(25000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdown_view.text = ((25000 - millisUntilFinished) / 1000).toString()
                Crashlytics.log("onTick(): ${countdown_view.text}")
            }

            override fun onFinish() {
                connectionClient.stopAllEndpoints()
                countdown_view.visibility = GONE
                lookViewModel.setNoFoundState()
                Crashlytics.logException(Throwable("No found. Timer has finished"))
                FirebaseCrashlytics.getInstance().recordException(Throwable("No found. Timer has finished"))
                Crashlytics.log("timer onFinish()")
            }
        }
        timer?.start()
    }

    private var endpointDiscoveryCallback: EndpointDiscoveryCallback? =
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                if (user?.name != null && connectionLifecycleCallback != null) {
                    connectionClient.requestConnection(user?.name!!, endpointId, connectionLifecycleCallback!!)
                        ?.addOnFailureListener { e ->
                            handleFailedResponse(e)
                            showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                            // closeActivity()
                        }
                }
            }

            override fun onEndpointLost(endpointId: String) {
                showSnackbarError(getString(R.string.look_error_disconnected))
                Crashlytics.log("onEndpointLost: $endpointId")
            }
        }

    private var connectionLifecycleCallback: ConnectionLifecycleCallback? =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
                connectionClient.acceptConnection(p0, payloadCallback)?.addOnFailureListener { e ->
                    showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                    Crashlytics.logException(e)
                    closeActivity()
                }
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                lookViewModel.endpointIdSaved = endpointId
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        Crashlytics.log("endpointId = $endpointId")
                    }

                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        handleFailedResponse(Exception("ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED"))
                        showSnackbarError(getString(R.string.look_error_rejected))
                        //  closeActivity()
                    }
                    ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR -> {
                        handleFailedResponse(Exception("ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR"))
                        showSnackbarError(getString(R.string.look_error_failed))
                        //  closeActivity()
                    }

                    ConnectionsStatusCodes.STATUS_ERROR -> {
                        handleFailedResponse(Exception("ConnectionsStatusCodes.STATUS_ERROR"))
                        showSnackbarError(getString(R.string.look_error_failed))
                        //  closeActivity()
                    }
                    else -> {
                        handleFailedResponse(Exception("Unknown status code"))
                        showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                        //  closeActivity()
                    }
                }
            }

            override fun onDisconnected(p0: String) {
                showSnackbarError(getString(R.string.look_error_disconnected))
                lookViewModel.endpointIdSaved?.let { connectionClient.disconnectFromEndpoint(it) }
                Crashlytics.log("onDisconnected: $p0")
            }
        }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            lookViewModel.handlePayload(p1)
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            if (p1.status == PayloadTransferUpdate.Status.SUCCESS && p1.totalBytes > 1000) {
                Crashlytics.log("onPayloadTransferUpdate: ${p1.status} && ${p1.totalBytes}")
                if (profile_image.drawable == null && lookViewModel.isGenderValid) {
                    timer?.cancel()
                    lookViewModel.advertiserName?.let {
                        searchingInProgressText.text =
                            lookViewModel.advertiserName
                    }
                    populateSucceedView()
                    Glide.with(applicationContext)
                        .load(lookViewModel.newFile).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(profile_image)
                }
            }
        }
    }

    private fun handleFailedResponse(exception: Exception) {
        timer?.cancel()
        Crashlytics.logException(exception)
        lookViewModel.endpointIdSaved?.let { connectionClient.disconnectFromEndpoint(it) }
        connectionClient.stopAllEndpoints()
        lookViewModel.setNoFoundState()
    }
}
