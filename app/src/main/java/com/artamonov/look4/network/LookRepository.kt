package com.artamonov.look4.network

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionsClient

class LookRepository(context: Context) {

    private var connectionClient: ConnectionsClient = Nearby.getConnectionsClient(context)

    private fun getClient(): ConnectionsClient {
        return connectionClient
    }
}
