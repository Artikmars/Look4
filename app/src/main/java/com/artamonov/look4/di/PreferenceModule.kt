package com.artamonov.look4.di

import com.artamonov.look4.data.prefs.PreferenceHelper
import com.artamonov.look4.data.prefs.PreferenceHelperImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class PreferenceModule {

    @Singleton
    @Binds
    abstract fun bindPreferenceHelper(preferenceHelperImpl: PreferenceHelperImpl): PreferenceHelper
}
