package me.arnoldwho.aweartool;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.File;

public class ScreenFragment extends Fragment {

    RunCmd runCmd = new RunCmd();
    private String ADBPATH;

    public ScreenFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screen, container, false);
        ADBPATH = view.getContext().getFilesDir().getAbsolutePath() + File.separator;
        Button screencapBtn = (Button) view.findViewById(R.id.screenscpBtn);
        new Thread(new Runnable() {
            @Override
            public void run() {
                //runCmd.runCMD("su -c cd " + ADBPATH +
                        //" && ./arnold forward tcp:4444 localabstract:/adb-hub && ./arnold connect 127.0.0.1:4444");
            }
        }).start();
        screencapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeScreenShoot();
            }
        });
        return view;
    }

    public void takeScreenShoot() {
        String CMD = "su -c cd " + ADBPATH + " && ./arnold -s 127.0.0.1:4444 shell /system/bin/screencap -p /sdcard/screenshot.png";
        //runCmd.runCMD(CMD);
    }
}
