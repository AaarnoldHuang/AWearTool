package me.arnoldwho.aweartool;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 0;
    private static final String TAG = "ChooseFile";
    private String ADBPATH;
    private String sd_name;
    private String FILE_PATH;
    private TextView showText;

    MyIO myIO = new MyIO();
    RunCmd runCmd = new RunCmd();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button installBtn = (Button) findViewById(R.id.installBtn);
        final Button runBtm = (Button) findViewById(R.id.runBtn);
        showText = (TextView) findViewById(R.id.showText);
        ADBPATH = MainActivity.this.getFilesDir().getAbsolutePath() + File.separator;
        sd_name = getExtendedMemoryPath(this);
        firstStart();

        installBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showText.setText(Connect(MainActivity.this));
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
            }
        });

        runBtm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String openNetworkDebug = "su -c setprop service.adb.tcp.port 5555 && stop adbd && start adbd";
                String myCMD = "su -c cd " + ADBPATH + " && ./arnold connect 127.0.0.1:5555 && ./arnold devices";
                runCmd.runCMD(openNetworkDebug);
                showText.setText(runCmd.runCMD(myCMD));
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (!runCmd.runCMD("getprop service.adb.tcp.port").equals("-1")) {
            runCmd.runCMD("su -c setprop service.adb.tcp.port -1 && stop adbd && start adbd && "
                    + ADBPATH + "/arnold kill-server");
        }
        super.onDestroy();
    }

    public void firstStart() {
        SharedPreferences pref = getSharedPreferences("share", MODE_PRIVATE);
        boolean isFirstRun = pref.getBoolean("isFirstRun", true);
        SharedPreferences.Editor editor = getSharedPreferences("share", MODE_PRIVATE).edit();
        if (isFirstRun) {
            editor.putBoolean("isFirstRun", false);
            editor.apply();
            myIO.copy(MainActivity.this, "arnold");
            myIO.copy(MainActivity.this, "arnold.bin");
        }
    }

    public String Connect(Activity activity) {
        return runCmd.runCMD("su -c cd " + ADBPATH +
                " && ./arnold forward tcp:4444 localabstract:/adb-hub && ./arnold connect 127.0.0.1:4444 && ./arnold devices");
    }

    public String installApk(Activity activity, String FILE_PATH) {
        return runCmd.runCMD("su -c cd " + ADBPATH + " && ./arnold -s 127.0.0.1:4444 install -r \"" + FILE_PATH + "\"");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    FILE_PATH = getPath(MainActivity.this, uri, sd_name);
                    showText.setText("Installing....");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            installApk(MainActivity.this, FILE_PATH);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showText.setText("Success!");
                                }
                            });
                        }
                    }).start();
                }
                break;
        }
    }

    public static String getPath(final Context context, final Uri uri, String sd_name) {

        String pathHead = "/storage/";
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else if (sd_name.equalsIgnoreCase(type)) {
                    return pathHead + sd_name + "/" + split[1];
                }
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return pathHead + uri.getPath();
        }
        return null;
    }
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static String getExtendedMemoryPath(Context mContext) {
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (removable) {
                    final String[] split = path.split("/");
                    final String SDname = split[2];
                    return SDname;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }  catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        return null;
    }

}
