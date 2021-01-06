package com.example.myapplication;

import android.app.Application;

import com.library.net.DataCenterEngine;
import com.library.net.DataStrategy;

public class AppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        DataCenterEngine
                .strategy(DataStrategy.sDefault.threadCount(1).retryCount(0).strategy(DataStrategy.UploadStrategy.IMMEDIATELY).encode(false))
                .configure("5d0660c0-b67d-47e3-b0eb-10649d4c382f", "ff93de60-b4bd-4f98-8ae3-2d7aeda08ede")
                .user("10086")
                .start(this);

        //登录后设置用户ID
        DataCenterEngine.user("10086");
    }
}
