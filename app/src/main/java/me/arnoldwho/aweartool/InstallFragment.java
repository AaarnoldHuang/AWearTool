package me.arnoldwho.aweartool;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import static android.app.Activity.RESULT_OK;

public class InstallFragment extends Fragment {

    private static final int FILE_SELECT_CODE = 0;
    private static final String TAG = "ChooseFile";
    private String ADBPATH;
    private String sd_name;
    private String FILE_PATH;
    private TextView showText;
    private View view;
    MyHandler myHandler;

    RunCmd runCmd = new RunCmd();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_install, container, false);
        Button installBtn = (Button) view.findViewById(R.id.installBtn);
        final Button runBtm = (Button) view.findViewById(R.id.runBtn);
        showText = (TextView) view.findViewById(R.id.showText);
        ADBPATH = view.getContext().getFilesDir().getAbsolutePath() + File.separator;
        sd_name = getExtendedMemoryPath(view.getContext());
        runBtm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runCmd.runCMD("su -c setprop service.adb.tcp.port 5555 && stop adbd && start adbd");
                runCmd.runCMD("su -c cd " + ADBPATH + " && ./arnold connect 127.0.0.1:5555");
                showText.setText(runCmd.runCMD("su -c cd " + ADBPATH + " && ./arnold devices"));
            }
        });

        installBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showText.setText(Connect());
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
            }
        });

       myHandler = new MyHandler();

        return view;
    }

    class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    showText.setText("Success!");
                    break;
                default:
                    break;
            }

        }
    }

    public String Connect() {
        return runCmd.runCMD("su -c cd " + ADBPATH +
                " && ./arnold forward tcp:4444 localabstract:/adb-hub && ./arnold connect 127.0.0.1:4444 && ./arnold devices");
    }

    public String installApk(String FILE_PATH) {
        return runCmd.runCMD("su -c cd " + ADBPATH + " && ./arnold -s 127.0.0.1:4444 install -r \"" + FILE_PATH + "\"");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    FILE_PATH = getPath(view.getContext(), uri, sd_name);
                    showText.setText("Installing....");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            installApk(FILE_PATH);
                            Message msg = new Message();
                            msg.what = 0;
                            myHandler.sendMessage(msg);
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
