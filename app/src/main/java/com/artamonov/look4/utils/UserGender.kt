package com.artamonov.look4.utils

import androidx.annotation.StringDef

class UserGender {

    companion object {
        const val MALE = "MALE"
        const val FEMALE = "FEMALE"
        const val ALL = "ALL"
    }

    @Target(AnnotationTarget.TYPE)
    @StringDef(MALE, FEMALE, ALL)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class AnnotationUserGender
}
