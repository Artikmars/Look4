package com.artamonov.look4.utils;

import android.content.Context;
import java.io.File;
import java.io.IOException;

public class LogHandler {

    public static File saveLogsToFile(Context context) {
        String fileName = "log_" + System.currentTimeMillis() + ".txt";
        File outputFile = new File(context.getExternalCacheDir(), fileName);
        try {
            @SuppressWarnings("unused")
            Process process =
                    Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFile;
    }
}
