package com.library.net;

import android.content.Context;

public class Configure {

    public static String url_auth = "http://datacenter-developer-dev.aitdcoin.com/api/developer/v1/services/auth";
    public static String url_report = "http://172.31.17.22:9889/api/point/v1/report";

    public static String appId = "5d0660c0-b67d-47e3-b0eb-10649d4c382f";
    public static String appSecret = "ff93de60-b4bd-4f98-8ae3-2d7aeda08ede";


    public static int threadCount = 1;
    public static int retry = -1;

    public static Context context;

    public static String token;
}
