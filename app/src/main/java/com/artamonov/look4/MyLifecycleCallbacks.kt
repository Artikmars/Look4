package com.artamonov.look4

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.crashlytics.android.Crashlytics

class MyLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Crashlytics.log(activity::class.simpleName)
    }

    override fun onActivityResumed(activity: Activity) {
    }
}
