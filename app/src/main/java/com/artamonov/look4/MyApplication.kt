package com.artamonov.look4
import android.app.Application
import com.artamonov.look4.data.prefs.PreferenceHelper

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PreferenceHelper.init(this)
    }
}
