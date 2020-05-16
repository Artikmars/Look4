package com.artamonov.look4.di

import com.artamonov.look4.data.prefs.PreferenceHelper
import org.koin.dsl.module

val appModule = module {
    single { PreferenceHelper(get()) }
}