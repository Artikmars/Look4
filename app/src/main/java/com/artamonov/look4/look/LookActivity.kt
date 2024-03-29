package com.artamonov.look4.look

import android.Manifest.permission.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.database.User
import com.artamonov.look4.databinding.ActivityLookBinding
import com.artamonov.look4.utils.*
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactAdvertiserUnseenState
import com.artamonov.look4.utils.UserRole.Companion.ADVERTISER
import com.artamonov.look4.utils.UserRole.Companion.DISCOVERER
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.awesomedialog.*
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.Strategy.P2P_STAR
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class LookActivity : BaseActivity() {

    companion object {
        const val LOG_TAG = "Look4"
        val requiredPermissions = arrayOf(
            ACCESS_COARSE_LOCATION,
            ACCESS_FINE_LOCATION,
            READ_EXTERNAL_STORAGE
        )
    }

    private lateinit var binding: ActivityLookBinding
    private val strategy = P2P_STAR
    private val discOptions by lazy { DiscoveryOptions.Builder().setStrategy(strategy).build() }
    private var user: User? = null
    private val lookViewModel: LookViewModel by viewModels()
    var job: Job? = null

    private var requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value == true }
            if (granted) {
                lookViewModel.handleNewIntent(intent)
                if (!lookViewModel.isIntentValid()) {
                    lookViewModel.startDiscovering()
                }
            } else {
                finish()
                showSnackbarError(R.string.error_permissions_are_not_granted_for_discovering)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lookViewModel._state.observe(this) { bindViewState(it) }
        lookViewModel._action.observe(this) { bindViewAction(it) }
        lookViewModel._user.observe(this) { user = it }

        binding.searchBtn.setOnClickListener { startClient() }
        binding.lookBack.setOnClickListener { onBackPressed() }

        binding.noButton.setSafeOnClickListener {
            lookViewModel.endpointIdSaved?.disconnectFromEndpoint(connectionClient)
            closeActivity()
        }

        binding.yesButton.setSafeOnClickListener {
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
                        closeActivity()
                    }
                    lookViewModel.setPendingState()
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

    private fun bindViewAction(it: LookAction?) {
        when (it) {
            is LookAction.ShowDialog -> {
                AwesomeDialog.build(this)
                    .title(getString(R.string.look_disconnect_warning_title))
                    .body(getString(R.string.look_disconnect_warning))
                    .onPositive(getString(R.string.look_yes)) { closeActivity() }
                    .onNegative(getString(R.string.look_no))
            }
            else -> {
                // null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        endpointDiscoveryCallback = null
        connectionLifecycleCallback = null
    }

    override fun onBackPressed() {
        if (lookViewModel.showWarningIfPending()) {
            return
        }
        super.onBackPressed()
    }

    private fun bindViewState(viewState: LookState) {
        when (viewState) {
            is LookState.DefaultState -> {
                firebaseCrashlytics.log("State: Default")
                binding.populateDefaultView()
            }
            is LookState.NoFoundState -> {
                firebaseCrashlytics.log("State: No Found")
                binding.populateNoFoundView()
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
                binding.populateSucceedView()
            }
            is LookState.SucceededDiscoverIsFoundState<*> -> {
                firebaseCrashlytics.log("State: SucceededDiscoverIsFoundState")
                binding.populateSucceedView()
                binding.searchingInProgressText.isVisible = true
                binding.searchingInProgressText.text = lookViewModel.discovererName
                binding.profileImage.setImageDrawable(Drawable.createFromPath(lookViewModel.discovererFilePath))
            }
            is LookState.PendingState -> {
                firebaseCrashlytics.log("State: PendingState")
                binding.populatePendingView()
                binding.searchingInProgressText.isVisible = true
                binding.searchingInProgressText.text = getString(R.string.look_pending)
            }
            else -> {
                // null
            }
        }
    }

    private fun closeActivity() {
        startMainActivity()
        finish()
    }

    private fun getUserProfile(): User {
        return prefs.getUserProfile()
    }

    private fun checkForPermissions() {
        if (lookViewModel.hasPermissionsGranted(requiredPermissions)) {
            firebaseCrashlytics.log("Permission is given in checkForPermissions(): $ACCESS_COARSE_LOCATION")
            lookViewModel.handleNewIntent(intent)
            if (!lookViewModel.isIntentValid()) {
                lookViewModel.startDiscovering()
            }
        } else {
            firebaseCrashlytics.log("Permission is missing in checkForPermissions(): $ACCESS_COARSE_LOCATION")
            requestPermissions.launch(requiredPermissions)
        }
    }

    private fun startClient() {
        startTimer()
        binding.populateScanningView()
        lookViewModel.endpointIdSaved?.disconnectFromEndpoint(connectionClient)
        connectionClient.stopAllEndpoints()
        endpointDiscoveryCallback?.let {
            connectionClient.startDiscovery(packageName, it, discOptions)
                .addOnSuccessListener {
                    lookViewModel.updateRole(DISCOVERER)
                    firebaseCrashlytics.log("Discovery has been started")
                }.addOnFailureListener { e ->
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
            keepScreenOn()
            for (second in 0..totalSeconds) {
                binding.countdownView.text = second.toString()
                firebaseCrashlytics.log("onTick(): ${binding.countdownView.text}")
                delay(1000)
            }
            connectionClient.stopDiscovery()
            connectionClient.stopAllEndpoints()
            unKeepScreenOn()

            // Make count down view gone when on no found state
            binding.countdownView.text = ""

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
                    connectionClient.requestConnection(
                        user?.name!!,
                        endpointId,
                        connectionLifecycleCallback!!
                    )
                        .addOnFailureListener { e ->
                            handleFailedResponse(e)
                            showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
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
                        connectionClient.stopDiscovery()
                    }

                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                        handleFailedResponse(Exception("ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED"))
                        showSnackbarError(getString(R.string.look_error_rejected))
                    }
                    ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR -> {
                        handleFailedResponse(Exception("ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR"))
                        showSnackbarError(getString(R.string.look_error_failed))
                    }

                    ConnectionsStatusCodes.STATUS_ERROR -> {
                        handleFailedResponse(Exception("ConnectionsStatusCodes.STATUS_ERROR"))
                        showSnackbarError(getString(R.string.look_error_failed))
                    }
                    else -> {
                        handleFailedResponse(Exception("Unknown status code"))
                        showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                    }
                }
            }

            override fun onDisconnected(p0: String) {
                job?.cancel()
                unKeepScreenOn()

                // Make count down view gone when on no found state
                binding.countdownView.text = ""

                lookViewModel.endpointIdSaved?.disconnectFromEndpoint(connectionClient)
                connectionClient.stopAllEndpoints()
                showSnackbarError(getString(R.string.look_error_disconnected))
                closeActivity()
            }
        }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            lookViewModel.handlePayload(p1)
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            if (p1.status == PayloadTransferUpdate.Status.SUCCESS && p1.totalBytes > 1000) {
                firebaseCrashlytics.log("onPayloadTransferUpdate: ${p1.status} && ${p1.totalBytes}")
                if (binding.profileImage.drawable == null && lookViewModel.isGenderValid) {
                    job?.cancel()
                    lookViewModel.advertiserName?.let {
                        binding.searchingInProgressText.text = lookViewModel.advertiserName
                    }
                    binding.populateSucceedView()
                    Glide.with(applicationContext)
                        .load(lookViewModel.newFile).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(binding.profileImage)
                }
            }
        }
    }

    private fun handleFailedResponse(exception: Exception) {
        lookViewModel.endpointIdSaved?.disconnectFromEndpoint(connectionClient)
        connectionClient.stopAllEndpoints()
        job?.cancel()
        unKeepScreenOn()

        // Make count down view gone when on no found state
        binding.countdownView.text = ""

        firebaseCrashlytics.recordException(exception)
        lookViewModel.setNoFoundState()
    }
}
