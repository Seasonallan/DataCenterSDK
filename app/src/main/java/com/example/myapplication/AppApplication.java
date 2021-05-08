package com.example.myapplication;

import android.app.Application;

import com.library.net.DataCenterEngine;
import com.library.net.core.strategy.DataStrategy;

public class AppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //http://datacenter-push-log.aitdcoin.com/api/point/v1/report
        DataCenterEngine
                .strategy(DataStrategy.sDefault.threadCount(1).retryCount(0)
                        .strategy(DataStrategy.UploadStrategy.IMMEDIATELY).encryptData(false).catchException(true).logcat(true))
                .configure("5d0660c0-b67d-47e3-b0eb-10649d4c382f", "ff93de60-b4bd-4f98-8ae3-2d7aeda08ede")
                .configure("97692a32-0ac8-4570-84d4-ef0c1c951b5e", "e617a438-33b3-4206-9c25-52dbfc396809")
                .environment("http://172.93.1.253:9889/api/point/v1/report")
                .environment("http://point-upload-pre.aitdcoin.com/api/point/v1/report")
                .user("10086")
                .start(this);


        //登录后可以设置用户ID
        DataCenterEngine.user("10086");
    }
}
