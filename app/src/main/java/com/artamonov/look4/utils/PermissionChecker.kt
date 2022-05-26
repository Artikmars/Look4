package com.artamonov.look4.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

class PermissionChecker @Inject constructor(
    private val context: Context,
    private val firebaseCrashlytics: FirebaseCrashlytics
) {

    /** Returns true if the app was granted all the permissions. Otherwise, returns false.  */
    fun hasPermissionsGranted(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                firebaseCrashlytics.log("Permission is missing: $permission")
                return false
            }
        }
        return true
    }
}