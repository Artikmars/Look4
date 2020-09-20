package com.artamonov.look4.utils

import android.content.Context
import android.view.animation.Animation
import android.view.animation.RotateAnimation

class LookRotateAnimation(val context: Context) : RotateAnimation(0F, 360F,
    Animation.RELATIVE_TO_SELF, 0.5f,
    Animation.RELATIVE_TO_SELF, 0.5f) {

    fun init(): LookRotateAnimation {
        val anim = LookRotateAnimation(context)
        anim.duration = 3000
        anim.repeatCount = INFINITE
        anim.fillAfter = true
        anim.setInterpolator(context, android.R.anim.linear_interpolator)
        return anim
    }
}
