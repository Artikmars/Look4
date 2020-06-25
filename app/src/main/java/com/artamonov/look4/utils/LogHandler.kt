package com.artamonov.look4.utils

import android.content.Context
import java.io.File
import java.io.IOException

object LogHandler {
    fun saveLogsToFile(context: Context): File {
        val fileName = "log_" + System.currentTimeMillis() + ".txt"
        val outputFile = File(context.externalCacheDir, fileName)
        try {
            Runtime.getRuntime().exec("logcat -f " + outputFile.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return outputFile
    }
}
