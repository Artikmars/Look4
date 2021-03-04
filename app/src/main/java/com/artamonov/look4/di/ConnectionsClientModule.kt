package com.artamonov.look4.di

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionsClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ConnectionsClientModule {

    @Singleton
    @Provides
    fun bindConnectionsClient(@ApplicationContext context: Context): ConnectionsClient {
        return Nearby.getConnectionsClient(context)
    }
}
