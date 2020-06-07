package com.artamonov.look4.look

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.CHANGE_WIFI_STATE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
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
import com.artamonov.look4.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
import com.artamonov.look4.R
import com.artamonov.look4.base.BaseActivity
import com.artamonov.look4.data.database.User
import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.utils.UserRole.Companion.ADVERTISER
import com.artamonov.look4.utils.UserRole.Companion.DISCOVERER
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
        private var timer: CountDownTimer? = null

        private var connectionClient: ConnectionsClient? = null

        private val discOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        private var lookViewModel = LookViewModel()
        private var user: User? = null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        lookViewModel.handleNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_look)
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
                    populateDefaultView()
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
        look_back.setOnClickListener { onBackPressed() }

        no_button.setOnClickListener {
            // TODO Replace with disconnectFromEndpoint()
            connectionClient?.stopAllEndpoints()
            populateDefaultView()
        }

        yes_button.setOnClickListener {
            when (PreferenceHelper.getUserProfile()?.role) {
                ADVERTISER -> {
                    lookViewModel.savePhoneNumberToDB(lookViewModel.discovererPhoneNumber, ADVERTISER)

                    showSnackbarWithAction()
                    lookViewModel.endpointIdSaved?.let {
                        connectionClient?.sendPayload(
                            lookViewModel.endpointIdSaved!!, Payload.fromBytes(PreferenceHelper.getUserProfile()?.phoneNumber!!.toByteArray()))?.addOnFailureListener { e ->
                            showSnackbarError(e.toString())
                            connectionClient?.disconnectFromEndpoint(
                                lookViewModel.endpointIdSaved!!) }?.addOnSuccessListener {
                            finish()
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
                            e -> showSnackbarError(e.toString())
                    }
                        connectionClient?.sendPayload(
                            lookViewModel.endpointIdSaved!!, pFilePayload)?.addOnFailureListener {
                                e -> showSnackbarError(e.toString())
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
        countdown_label.visibility = GONE
    }

    private fun populateScanningView() {
        searchingInProgressText.isAllCaps = true
        searchingInProgressText.text = resources.getString(R.string.look_searching_in_progress)
        talk_in_person_text.visibility = GONE
        look_divider.visibility = GONE
        searchBtn.visibility = GONE
        no_button.visibility = GONE
        yes_button.visibility = GONE
        profile_image.visibility = GONE
        found_view.visibility = GONE
        searchingInProgressText.visibility = VISIBLE
        countdown_view.visibility = VISIBLE
        countdown_label.visibility = VISIBLE
    }

    private fun populateNoFoundView() {
        searchingInProgressText.text = resources.getString(R.string.look_no_found)
        searchBtn.text = resources.getString(R.string.look_search_again)
        countdown_view.visibility = GONE
        countdown_label.visibility = GONE
        no_button.visibility = GONE
        yes_button.visibility = GONE
        profile_image.visibility = GONE
        found_view.visibility = GONE
        talk_in_person_text.visibility = VISIBLE
        look_divider.visibility = VISIBLE
        searchBtn.visibility = VISIBLE
    }

    private fun populateSucceedView() {
            countdown_view.visibility = GONE
            countdown_label.visibility = GONE
            talk_in_person_text.visibility = GONE
            look_divider.visibility = GONE
            searchBtn.visibility = GONE
            no_button.visibility = VISIBLE
            yes_button.visibility = VISIBLE
            profile_image.visibility = VISIBLE
            found_view.visibility = VISIBLE
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
        populateScanningView()
        lookViewModel.endpointIdSaved?.let { connectionClient?.disconnectFromEndpoint(lookViewModel.endpointIdSaved!!) }
        startTimer()
        connectionClient?.startDiscovery(packageName, endpointDiscoveryCallback, discOptions)
        ?.addOnSuccessListener {
                    lookViewModel.updateRole(DISCOVERER)
                }?.addOnFailureListener { e ->
                // We're unable to start discovering.
                showSnackbarError(e.toString())
                populateNoFoundView()
                }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(25000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdown_view.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                connectionClient?.stopAllEndpoints()
                populateNoFoundView()
            }
        }
        timer?.start()
    }

    private val endpointDiscoveryCallback: EndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            user?.let {
                connectionClient?.requestConnection(user?.name!!, endpointId, connectionLifecycleCallback)
                    ?.addOnSuccessListener {
                    }
                    ?.addOnFailureListener { e ->
                        handleFailedResponse()
                        showSnackbarError(e.toString())
                        connectionClient?.stopAllEndpoints()
                    }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            showSnackbarError("Endpoint: $endpointId lost!")
        }
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
            connectionClient?.acceptConnection(p0, payloadCallback)?.addOnFailureListener { e ->
                showSnackbarError(e.toString()) }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> { lookViewModel.endpointIdSaved = endpointId }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    handleFailedResponse()
                    showSnackbarError("Connection attempt to $endpointId was rejected")
                }
                ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR -> {
                    connectionClient?.disconnectFromEndpoint(endpointId)
                    handleFailedResponse()
                    showSnackbarError("Connection attempt to $endpointId was failed")
                }

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    handleFailedResponse()
                    showSnackbarError("Connected attempt to $endpointId failed")
                }
                else -> {
                    handleFailedResponse()
                    showSnackbarError("Unknown status code")
                }
            }
        }

        override fun onDisconnected(p0: String) {
            showSnackbarError("Disconnected")
        }
    }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            lookViewModel.handlePayload(p1)
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            if (p1.status == PayloadTransferUpdate.Status.SUCCESS && p1.totalBytes > 1000) {
                if (profile_image.drawable == null && lookViewModel.isGenderValid) {
                    timer?.cancel()
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

    private fun handleFailedResponse() {
        connectionClient?.stopAllEndpoints()
        timer?.cancel()
        populateNoFoundView()
    }
}
