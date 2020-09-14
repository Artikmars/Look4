package com.artamonov.look4.look

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.database.User
import com.artamonov.look4.main.MainActivity
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactAdvertiserUnseenState
import com.artamonov.look4.utils.UserRole.Companion.ADVERTISER
import com.artamonov.look4.utils.UserRole.Companion.DISCOVERER
import com.artamonov.look4.utils.disconnectFromEndpoint
import com.artamonov.look4.utils.set
import com.artamonov.look4.utils.setSafeOnClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy.P2P_POINT_TO_POINT
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.activity_look.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LookActivity : BaseActivity(R.layout.activity_look) {

    companion object {
        const val LOG_TAG = "Look4"
        private const val REQUEST_CODE_REQUIRED_PERMISSIONS = 1
    }

    private val requiredPermissions = arrayOf(
        ACCESS_COARSE_LOCATION,
        ACCESS_FINE_LOCATION,
        READ_EXTERNAL_STORAGE
    )
    private val strategy = P2P_POINT_TO_POINT
    private val discOptions by lazy { DiscoveryOptions.Builder().setStrategy(strategy).build() }
    private var user: User? = null
    private val lookViewModel: LookViewModel by viewModels()
    var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lookViewModel.state.observe(this, { bindViewState(it) })
        lookViewModel.user.observe(this, { user = it })

        searchBtn.setOnClickListener { startClient() }
        look_back.setOnClickListener { closeActivity() }

        no_button.setSafeOnClickListener {
            lookViewModel.endpointIdSaved?.disconnectFromEndpoint(connectionClient)
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
                                firebaseCrashlytics.recordException(e)
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
                        firebaseCrashlytics.recordException(e)
                        showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                        closeActivity()
                    }
                    connectionClient.sendPayload(
                        lookViewModel.endpointIdSaved!!, pFilePayload
                    )?.addOnFailureListener { e ->
                        showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                        firebaseCrashlytics.recordException(e)
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
        job?.cancel()
        endpointDiscoveryCallback = null
        connectionLifecycleCallback = null
    }

    private fun bindViewState(viewState: LookState) {
        when (viewState) {
            is LookState.DefaultState -> {
                firebaseCrashlytics.log("State: Default")
                populateDefaultView()
            }
            is LookState.NoFoundState -> {
                firebaseCrashlytics.log("State: No Found")
                populateNoFoundView()
            }
            is LookState.SearchState -> {
                firebaseCrashlytics.log("State: Search")
                startClient()
            }
            is LookState.PhoneNumberReceived -> {
                firebaseCrashlytics.log("State: PhoneNumberReceived")
                lookViewModel.endpointIdSaved?.disconnectFromEndpoint(connectionClient)
                showSnackbarWithAction()
            }
            is LookState.SucceededAdvertiserIsFoundState<*> -> {
                firebaseCrashlytics.log("State: SucceededAdvertiserIsFoundState")
                populateSucceedView()
            }
            is LookState.SucceededDiscoverIsFoundState<*> -> {
                firebaseCrashlytics.log("State: SucceededDiscoverIsFoundState")
                populateSucceedView()
                searchingInProgressText.visibility = VISIBLE
                searchingInProgressText.text = lookViewModel.discovererName
                profile_image.setImageDrawable(Drawable.createFromPath(lookViewModel.discovererFilePath))
            }
        }
    }

    private fun closeActivity() {
        startActivity(
            Intent(this, MainActivity::class.java),
            ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
        )

        finish()
    }

    private fun getUserProfile(): User? {
        return prefs.getUserProfile()
    }

    private fun checkForPermissions() {
        if (!hasPermissions(this, requiredPermissions)) {
            firebaseCrashlytics.log("Permission is missing in checkForPermissions(): $ACCESS_COARSE_LOCATION")
            ActivityCompat.requestPermissions(this, requiredPermissions,
                REQUEST_CODE_REQUIRED_PERMISSIONS)
        } else {
            firebaseCrashlytics.log("Permission is given in checkForPermissions(): $ACCESS_COARSE_LOCATION")
            lookViewModel.handleNewIntent(intent)
            if (!lookViewModel.isIntentValid()) { lookViewModel.startDiscovering() }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_REQUIRED_PERMISSIONS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) &&
                    grantResults[1] == PERMISSION_GRANTED && grantResults[2] == PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    lookViewModel.handleNewIntent(intent)
                    if (!lookViewModel.isIntentValid()) { lookViewModel.startDiscovering() }
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
            if (ContextCompat.checkSelfPermission(context, permission) != PERMISSION_GRANTED
            ) {
                firebaseCrashlytics.log("Permission is missing: $permission")
                return false
            }
        }
        return true
    }

    private fun startClient() {
        startTimer()
        populateScanningView()
        lookViewModel.endpointIdSaved?.disconnectFromEndpoint(connectionClient)
        endpointDiscoveryCallback?.let {
            connectionClient.startDiscovery(packageName, it, discOptions)
                ?.addOnSuccessListener {
                    lookViewModel.updateRole(DISCOVERER)
                    firebaseCrashlytics.log("Discovery has been started")
                }?.addOnFailureListener { e ->
                    // We're unable to start discovering.
                    showSnackbarError(getString(R.string.look_error_scanning_can_not_be_started))
                    handleFailedResponse(e)
                    //  closeActivity()
                }
        }
    }

    private fun startTimer() {
        job = CoroutineScope(Dispatchers.Main).launch {
            val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(25000)
            for (second in 0..totalSeconds) {
                countdown_view.text = second.toString()
                firebaseCrashlytics.log("onTick(): ${countdown_view.text}")
                delay(1000)
            }
            connectionClient.stopAllEndpoints()
            countdown_view.text = ""
            lookViewModel.setNoFoundState()
            firebaseCrashlytics.recordException(Throwable("No found. Timer has finished"))
            firebaseCrashlytics.log(prefs.getUserProfile().toString())
            firebaseCrashlytics.log("timer onFinish()")
        }
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
                firebaseCrashlytics.log("onEndpointLost: $endpointId")
            }
        }

    private var connectionLifecycleCallback: ConnectionLifecycleCallback? =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
                connectionClient.acceptConnection(p0, payloadCallback)?.addOnFailureListener { e ->
                    showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                    firebaseCrashlytics.recordException(e)
                    closeActivity()
                }
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                lookViewModel.endpointIdSaved = endpointId
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        firebaseCrashlytics.log("endpointId = $endpointId")
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
                lookViewModel.endpointIdSaved?.disconnectFromEndpoint(connectionClient)
                firebaseCrashlytics.log("onDisconnected: $p0")
            }
        }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            lookViewModel.handlePayload(p1)
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            if (p1.status == PayloadTransferUpdate.Status.SUCCESS && p1.totalBytes > 1000) {
                firebaseCrashlytics.log("onPayloadTransferUpdate: ${p1.status} && ${p1.totalBytes}")
                if (profile_image.drawable == null && lookViewModel.isGenderValid) {
                    job?.cancel()
                    lookViewModel.advertiserName?.let {
                        searchingInProgressText.text = lookViewModel.advertiserName
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
        job?.cancel()
        firebaseCrashlytics.recordException(exception)
        lookViewModel.endpointIdSaved?.disconnectFromEndpoint(connectionClient)
        connectionClient.stopAllEndpoints()
        lookViewModel.setNoFoundState()
    }
}
