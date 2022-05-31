package com.artamonov.look4.di

import android.content.Context
import com.artamonov.look4.utils.PermissionChecker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class PermissionsCheckerModule {

    @Singleton
    @Provides
    fun bindPermissionsCheckerInstance(
        @ApplicationContext context: Context,
        firebaseCrashlytics: FirebaseCrashlytics
    ): PermissionChecker {
        return PermissionChecker(context, firebaseCrashlytics)
    }
}