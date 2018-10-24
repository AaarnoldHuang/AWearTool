package me.arnoldwho.aweartool;

import android.app.Activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MyIO {
    public void copy(Activity activity, String filename) {
        try {
            InputStream inputStream = activity.getAssets().open(filename);
            File file = new File(activity.getFilesDir().getAbsolutePath() + File.separator + filename);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            if (!file.exists() || file.length() == 0) {
                int len = -1;
                byte[] buffer = new byte[1024];
                while((len = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, len);
                }
            }
            fileOutputStream.flush();
            inputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("/system/bin/chmod 777 " + activity.getFilesDir().getAbsolutePath() + File.separator + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
