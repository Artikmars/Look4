package com.artamonov.look4.di

import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
class FirebaseCrashlyticsInstanceModule {

    @Singleton
    @Provides
    fun bindFirebaseCrashlyticsInstance(): FirebaseCrashlytics {
        return FirebaseCrashlytics.getInstance()
    }
}
