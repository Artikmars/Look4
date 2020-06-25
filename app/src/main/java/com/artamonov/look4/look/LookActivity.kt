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
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.artamonov.look4.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.main.MainActivity
import com.artamonov.look4.utils.ContactUnseenState
import com.artamonov.look4.utils.CountDownTimer.timer
import com.artamonov.look4.utils.LiveDataContactUnseenState.contactUnseenState
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
import kotlinx.android.synthetic.main.activity_look.*

class LookActivity : BaseActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            BLUETOOTH,
            BLUETOOTH_ADMIN,
            ACCESS_WIFI_STATE,
            CHANGE_WIFI_STATE,
            ACCESS_COARSE_LOCATION,
            ACCESS_FINE_LOCATION)

        private const val REQUEST_CODE_REQUIRED_PERMISSIONS = 1
        private val STRATEGY = P2P_POINT_TO_POINT
        const val LOG_TAG = "Look4"
        lateinit var deviceId: String

        private var connectionClient: ConnectionsClient? = null

        private val discOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        private lateinit var lookViewModel: LookViewModel
        private var user: User? = null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        lookViewModel.handleNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_look)
        lookViewModel = ViewModelProvider(this).get(LookViewModel::class.java)
        checkForPermissions()
        connectionClient = Nearby.getConnectionsClient(this)
        deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        lookViewModel.state.observe(this, Observer { state ->
            when (state) {
                is LookState.DefaultState -> {
                    populateDefaultView()
                }
                is LookState.NoFoundState -> {
                    populateNoFoundView()
                }
                is LookState.SearchState -> {
                    startClient()
                }
                is LookState.PhoneNumberReceived -> {
                    lookViewModel.endpointIdSaved
                    ?.let { connectionClient?.disconnectFromEndpoint(lookViewModel.endpointIdSaved!!) }
                    showSnackbarWithAction()
                }
                is LookState.SucceededAdvertiserIsFoundState<*> -> {
                    populateSucceedView()
                }
                is LookState.SucceededDiscoverIsFoundState<*> -> {
                    populateSucceedView()
                    searchingInProgressText.visibility = VISIBLE
                    searchingInProgressText.text =
                        lookViewModel.discovererName
                    profile_image.setImageDrawable(Drawable.createFromPath(lookViewModel.discovererFilePath))
                }
        }
        })

        lookViewModel.user.observe(this, Observer { user = it })

        searchBtn.setOnClickListener { startClient() }
        look_back.setOnClickListener { closeActivity() }

        no_button.setSafeOnClickListener {
            lookViewModel.endpointIdSaved?.let { connectionClient?.disconnectFromEndpoint(lookViewModel.endpointIdSaved!!) }
            closeActivity()
        }

        yes_button.setSafeOnClickListener {
            when (PreferenceHelper.getUserProfile()?.role) {
                ADVERTISER -> {
                    lookViewModel.savePhoneNumberToDB(lookViewModel.discovererPhoneNumber, ADVERTISER)

                    lookViewModel.endpointIdSaved?.let {
                        connectionClient?.sendPayload(
                            lookViewModel.endpointIdSaved!!, Payload.fromBytes(PreferenceHelper.getUserProfile()?.phoneNumber!!.toByteArray()))?.addOnFailureListener { e ->
                            showSnackbarError(getString(R.string.look_error_connection_is_lost))
                            Crashlytics.logException(e)
                            lookViewModel.endpointIdSaved?.let { connectionClient?.disconnectFromEndpoint(lookViewModel.endpointIdSaved!!) }
                            closeActivity()
                        }?.addOnSuccessListener {
                            lookViewModel.endpointIdSaved?.let { connectionClient?.disconnectFromEndpoint(lookViewModel.endpointIdSaved!!) }
                            contactUnseenState.set(newValue = ContactUnseenState.EnabledState)
                            closeActivity()
                        }
                    }
                }
                DISCOVERER -> {
                    connectionClient?.stopDiscovery()
                    val pfd: ParcelFileDescriptor? = contentResolver.openFileDescriptor(Uri.parse(PreferenceHelper.getUserProfile()?.imagePath!!), "r")
                    val pFilePayload = Payload.fromFile(pfd!!)
                    lookViewModel.endpointIdSaved?.let {
                        connectionClient?.sendPayload(
                            lookViewModel.endpointIdSaved!!,
                            Payload.fromBytes("${getUserProfile()?.name};${getUserProfile()?.phoneNumber};${getUserProfile()?.gender}".toByteArray()))?.addOnFailureListener {
                            e ->
                            Crashlytics.logException(e)
                            showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                            closeActivity()
                    }
                        connectionClient?.sendPayload(
                            lookViewModel.endpointIdSaved!!, pFilePayload)?.addOnFailureListener {
                                e ->
                            showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                            Crashlytics.logException(e)
                        }
                    }
                    finish()
                }
            }
        }

        lookViewModel.startDiscovering()
        onNewIntent(intent)

//        disconnectButton.setOnClickListener {
//            connClient.apply {
//             stopAdvertising()
//               stopDiscovery()
//                stopAllEndpoints()
//           }
//        }
    }

    private fun closeActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivity(Intent(this, MainActivity::class.java),
                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            } else { startActivity(Intent(this, MainActivity::class.java)) }

        finish()
    }

    private fun getUserProfile(): User? {
        return PreferenceHelper.getUserProfile()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        if (!hasPermissions(this, REQUIRED_PERMISSIONS.toString())) {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_REQUIRED_PERMISSIONS
            )
        }
    }

    private fun populateDefaultView() {
        searchingInProgressText.isAllCaps = true
        talk_in_person_text.visibility = GONE
        look_divider.visibility = GONE
        searchBtn.visibility = GONE
        no_button.visibility = GONE
        yes_button.visibility = GONE
        profile_image.visibility = GONE
        found_view.visibility = GONE
        searchingInProgressText.visibility = GONE
        countdown_view.visibility = GONE
        Crashlytics.log("Default view is populated")
    }

    private fun populateScanningView() {
        talk_in_person_text.visibility = GONE
        look_divider.visibility = GONE
        searchBtn.visibility = GONE
        no_button.visibility = GONE
        yes_button.visibility = GONE
        profile_image.visibility = GONE
        found_view.visibility = GONE
        searchingInProgressText.visibility = GONE
        countdown_view.visibility = VISIBLE
        Crashlytics.log("Scanning view is populated")
    }

    private fun populateNoFoundView() {
        searchingInProgressText.text = resources.getString(R.string.look_no_found)
        searchBtn.text = resources.getString(R.string.look_search_again)
        countdown_view.visibility = GONE
        no_button.visibility = GONE
        yes_button.visibility = GONE
        profile_image.visibility = GONE
        found_view.visibility = GONE
        talk_in_person_text.visibility = VISIBLE
        look_divider.visibility = VISIBLE
        searchBtn.visibility = VISIBLE
        searchingInProgressText.visibility = VISIBLE
        Crashlytics.log("No found view is populated")
    }

    private fun populateSucceedView() {
        countdown_view.visibility = GONE
        talk_in_person_text.visibility = GONE
        look_divider.visibility = GONE
        searchBtn.visibility = GONE
        no_button.visibility = VISIBLE
        yes_button.visibility = VISIBLE
        profile_image.visibility = VISIBLE
        found_view.visibility = VISIBLE
        Crashlytics.log("Succeed view is populated")
    }

    private fun checkForPermissions() {
        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(READ_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            )
        }
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
        look_progress_bar.visibility = VISIBLE
        populateScanningView()
        lookViewModel.endpointIdSaved?.let { connectionClient?.disconnectFromEndpoint(lookViewModel.endpointIdSaved!!) }
        connectionClient?.startDiscovery(packageName, endpointDiscoveryCallback, discOptions)
        ?.addOnSuccessListener { lookViewModel.updateRole(DISCOVERER)
            look_progress_bar.visibility = GONE
            startTimer()
            Crashlytics.log("Discovery has been started")
        }?.addOnFailureListener { e ->
                // We're unable to start discovering.
                look_progress_bar.visibility = GONE
                showSnackbarError(getString(R.string.look_error_scanning_can_not_be_started))
                handleFailedResponse(e)
                closeActivity()
                }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(25000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdown_view.text = (millisUntilFinished / 1000).toString()
                Crashlytics.log("onTick(): ${countdown_view.text}")
            }

            override fun onFinish() {
                connectionClient?.stopAllEndpoints()
                countdown_view.visibility = GONE
                lookViewModel.setNoFoundState()
                Crashlytics.log("timer onFinish()")
            }
        }
        timer.start()
    }

    private val endpointDiscoveryCallback: EndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            user?.let {
                connectionClient?.requestConnection(user?.name!!, endpointId, connectionLifecycleCallback)
                    ?.addOnFailureListener { e ->
                        handleFailedResponse(e)
                        showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                        closeActivity()
                    }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            showSnackbarError(getString(R.string.look_error_disconnected))
            Crashlytics.log("onEndpointLost: $endpointId")
        }
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
            connectionClient?.acceptConnection(p0, payloadCallback)?.addOnFailureListener { e ->
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
                    closeActivity()
                }
                ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR -> {
                    handleFailedResponse(Exception("ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR"))
                    showSnackbarError(getString(R.string.look_error_failed))
                    closeActivity()
                }

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    handleFailedResponse(Exception("ConnectionsStatusCodes.STATUS_ERROR"))
                    showSnackbarError(getString(R.string.look_error_failed))
                    closeActivity()
                }
                else -> {
                    handleFailedResponse(Exception("Unknown status code"))
                    showSnackbarError(getString(R.string.look_error_connection_is_lost_try_again))
                    closeActivity()
                }
            }
        }

        override fun onDisconnected(p0: String) {
            showSnackbarError(getString(R.string.look_error_disconnected))
            lookViewModel.endpointIdSaved?.let { connectionClient?.disconnectFromEndpoint(lookViewModel.endpointIdSaved!!) }
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
                    timer.cancel()
                    lookViewModel.advertiserName?.let { searchingInProgressText.text =
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
        timer.cancel()
        Crashlytics.logException(exception)
        lookViewModel.endpointIdSaved?.let { connectionClient?.disconnectFromEndpoint(lookViewModel.endpointIdSaved!!) }
        connectionClient?.stopAllEndpoints()
        lookViewModel.setNoFoundState()
    }
}
