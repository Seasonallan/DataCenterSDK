/*
 * Created by chenru on 2020/06/22.
 * Copyright 2015－2020 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.library.net.core;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.library.net.BuildConfig;
import com.library.net.core.strategy.DataStrategy;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class HttpUtils {


    public static JSONObject common(Context context, String userId, String appId) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("osType", "1");
            jsonObject.put("uid", userId);
            jsonObject.put("appId", appId);
            jsonObject.put("machineId", getUID(context));
            jsonObject.put("netWork", getNetworkState(context));
            String ip = getIp(context);
            jsonObject.put("startIp", ip);
            jsonObject.put("installIp", ip);
            jsonObject.put("exceptionIp", ip);
            int[] cpuRate = getCpu();
            jsonObject.put("appCpuUsage", cpuRate[0]);
            jsonObject.put("cpuUsage", cpuRate[1]);

            Object[] memory = getMemory(context);
            jsonObject.put("memoryUsage", memory[0]);
            jsonObject.put("appMemoryUsage", memory[1]);
            jsonObject.put("appMemorySize", memory[2]);

            jsonObject.put("oSVersion", Build.VERSION.SDK_INT);
            jsonObject.put("phoneBrand", Build.BRAND);
            jsonObject.put("phoneVersion", Build.MODEL);

            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                File sdcardDir = Environment.getExternalStorageDirectory();
                StatFs sf = new StatFs(sdcardDir.getPath());
                long blockSize = sf.getBlockSize();
                long blockCount = sf.getBlockCount();
                jsonObject.put("memory", blockSize * blockCount / 1024);
            }
            jsonObject.put("cpuVersion", Build.CPU_ABI);
        } catch (Exception e) {
            if (DataStrategy.logcat)
                e.printStackTrace();
        }
        return jsonObject;
    }


    /**
     * 获取内存使用率
     *
     * @return
     */
    public static Object[] getMemory(Context context) {
        Object[] rate = new Object[3];
        try {
            long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            //当前分配的总内存
            long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            //剩余内存
            long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);

            rate[0] = (totalMemory - freeMemory) * 100 / totalMemory;
            rate[1] = (totalMemory - freeMemory) * 100 / totalMemory;
            rate[2] = totalMemory;

            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);

            rate[0] = mi.availMem * 100 / mi.totalMem;
            rate[1] = mi.availMem * 100 / mi.totalMem;
            rate[2] = mi.totalMem / (1024 * 1024);
        } catch (Exception e) {
            if (DataStrategy.logcat)
                e.printStackTrace();
        }
        return rate;
    }

    /**
     * 获取CPU使用率
     *
     * @return
     */
    @Deprecated
    public static int[] getCpu() {
        int[] rate = {0, 0};

        try {
            String Result;
            Process p;
            p = Runtime.getRuntime().exec("top -n 1");

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((Result = br.readLine()) != null) {
                if (Result.trim().length() < 1) {
                    continue;
                } else {
                    String[] cpuUser = Result.split("%");
                    if (cpuUser.length > 1) {
                        String[] cpuUsage = cpuUser[0].split("User");
                        String[] sysUsage = cpuUser[1].split("System");

                        rate[0] = Integer.parseInt(cpuUsage[1].trim());
                        rate[1] = Integer.parseInt(sysUsage[1].trim());
                    }
                    break;
                }
            }

        } catch (Exception e) {
            if (DataStrategy.logcat)
                e.printStackTrace();
        }
        return rate;
    }

    /**
     * 获取唯一码
     *
     * @return
     */
    public static String getUID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
//        if (android.os.Build.VERSION.SDK_INT < 23) {
//            return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
//        } else {
//            return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
//        }
    }

    /**
     * netWork		1	2G
     * netWork		2	3G
     * netWork		3	wifi
     * netWork		4	4G
     * netWork		5	5G
     */
    public static int getNetworkState(Context context) {
        //获取系统的网络服务
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //如果当前没有网络
        if (null == connManager)
            return -1;
        //获取当前网络类型，如果为空，返回无网络
        NetworkInfo activeNetInfo = connManager.getActiveNetworkInfo();
        if (activeNetInfo == null || !activeNetInfo.isAvailable()) {
            return -1;
        }
        // 判断是不是连接的是不是wifi
        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (null != wifiInfo) {
            NetworkInfo.State state = wifiInfo.getState();
            if (null != state)
                if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
                    return 3;
                }
        }
        // 如果不是wifi，则判断当前连接的是运营商的哪种网络2g、3g、4g等
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (null != networkInfo) {
            NetworkInfo.State state = networkInfo.getState();
            String strSubTypeName = networkInfo.getSubtypeName();
            if (null != state)
                if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
                    switch (activeNetInfo.getSubtype()) {
                        //如果是2g类型
                        case TelephonyManager.NETWORK_TYPE_GPRS: // 联通2g
                        case TelephonyManager.NETWORK_TYPE_CDMA: // 电信2g
                        case TelephonyManager.NETWORK_TYPE_EDGE: // 移动2g
                        case TelephonyManager.NETWORK_TYPE_1xRTT:
                        case TelephonyManager.NETWORK_TYPE_IDEN:
                            return 1;
                        //如果是3g类型
                        case TelephonyManager.NETWORK_TYPE_EVDO_A: // 电信3g
                        case TelephonyManager.NETWORK_TYPE_UMTS:
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        case TelephonyManager.NETWORK_TYPE_HSDPA:
                        case TelephonyManager.NETWORK_TYPE_HSUPA:
                        case TelephonyManager.NETWORK_TYPE_HSPA:
                        case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        case TelephonyManager.NETWORK_TYPE_EHRPD:
                        case TelephonyManager.NETWORK_TYPE_HSPAP:
                            return 2;
                        //如果是4g类型
                        case TelephonyManager.NETWORK_TYPE_LTE:
                            return 4;
                        case TelephonyManager.NETWORK_TYPE_NR: //对应的20 只有依赖为android 10.0才有此属性
                            return 5;
                        default:
                            //中国移动 联通 电信 三种3G制式
                            if (strSubTypeName.equalsIgnoreCase("TD-SCDMA") || strSubTypeName.equalsIgnoreCase("WCDMA") || strSubTypeName.equalsIgnoreCase("CDMA2000")) {
                                return 3;
                            } else {
                                return 3;
                            }
                    }
                }
        }
        return -1;
    }

    public static String getIp(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        int i = info.getIpAddress();  //获取ip地址
        return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
                + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
    }


    static String getRetString(InputStream is) {
        String buf;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            is.close();
            buf = sb.toString();
            if (DataStrategy.logcat) {
                Log.e("DataCenter", "response=" + buf);
            }
            return buf;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                    reader = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                    is = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }
}
