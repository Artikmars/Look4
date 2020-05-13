package com.artamonov.look4.utils

import androidx.annotation.StringDef

class UserRole {

    companion object {
        const val ADVERTISER = "ADVERTISER"
        const val DISCOVERER = "DISCOVERER"
    }

    @Target(AnnotationTarget.TYPE)
    @StringDef(ADVERTISER, DISCOVERER)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class AnnotationUserRole
}
