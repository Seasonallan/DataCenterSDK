package com.library.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.library.net.core.HttpCallback;
import com.library.net.core.HttpMethod;
import com.library.net.core.RequestHelper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EasyRequestApi {

    private static volatile EasyRequestApi instance;

    private EasyRequestApi() {
        eventList = new CopyOnWriteArrayList<>();
    }

    private static EasyRequestApi instance() {
        if (instance == null) {
            synchronized (EasyRequestApi.class) {
                if (instance == null) {
                    instance = new EasyRequestApi();
                }
            }
        }
        return instance;
    }

    private CopyOnWriteArrayList<Event> eventList;

    public void eventReport(Event event) {
        eventList.add(event);
        if (consumeThreadCount < Configure.threadCount) {
            consume();
        }
    }

    private void eventError(Event... event) {
        for (Event ev : event) {
            eventList.add(0, ev);
        }
        //本地缓存
    }

    private void consume() {
        if (eventList.size() > 0) {
            Event event = eventList.remove(0);
            request(event);
        }
    }

    private int consumeThreadCount = 0;

    private void request(final Event event) {
        JSONObject jsonObject = common();
        event.parseJson(jsonObject);
        new RequestHelper.Builder(HttpMethod.POST, Configure.url_report)
                .header(headerMap())
                .jsonData(jsonObject.toString())
                .callback(new ResponseParse() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        eventError(event);
                    }

                    @Override
                    public void onResponse(Response response) {
                        if (response.code == 1) {//no authoritative, reAuth
                            eventError(event);
                            auth();
                        } else {
                            consume();//继续下一个
                        }
                    }
                }).retryCount(Configure.retry).execute();
    }

    /**
     * {
     * "eventCode":"STARTLOG",
     * "eventType":2,
     * "uid":"123246549846",
     * "pageCode": "login",
     * "startType":1,
     * "startFinishiTime":"2020-11-19 10:10:10",
     * <p>
     * "startElapsedTime":"50"
     * }
     */
    public static void report(Event event) {
        instance().eventReport(event);
    }

    private void auth() {
        new RequestHelper.Builder(HttpMethod.POST, Configure.url_auth)
                .params(paramsMap())
                .callback(new ResponseParse() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        eventError();
                    }

                    @Override
                    public void onResponse(Response response) {
                        if (response.code == -1) {//没有对应的服务
                            eventError();
                        } else {
                            consume();//继续下一个
                        }

                    }
                }).retryCount(Configure.retry).execute();
    }

    private static Map<String, String> paramsMap() {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("appId", Configure.appId);
        headerMap.put("appSecret", Configure.appSecret);
        return headerMap;
    }


    private abstract static class ResponseParse extends HttpCallback<Response> {

        @Override
        public Response onParseResponse(String result) {
            Response response = new Response();
            try {
                JSONObject jsonObject = new JSONObject(result);
                if (jsonObject.has("msg"))
                    response.msg = jsonObject.getString("msg");
                if (jsonObject.has("code"))
                    response.code = jsonObject.getInt("code");
                if (jsonObject.has("data"))
                    response.data = jsonObject.getString("data");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

    }


    private static class Response {
        public int code = -1;
        public String msg;
        public String data;
    }

    private static Map<String, String> headerMap() {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json;charset=UTF-8");
        headerMap.put("X-Auth-Token", Configure.token);
        return headerMap;
    }

    private static JSONObject common() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("osType", "1");
            jsonObject.put("appId", Configure.appId);
            jsonObject.put("machineId", getUID());
            jsonObject.put("netWork", getNetworkState());
            jsonObject.put("startIp", getIp());
            int[] cpuRate = getCpu();
            jsonObject.put("appCpuUsage", cpuRate[0]);
            jsonObject.put("cpuUsage", cpuRate[1]);

            Object[] memory = getMemory();
            jsonObject.put("memoryUsage", memory[0]);
            jsonObject.put("appMemoryUsage", memory[1]);
            jsonObject.put("appMemorySize", memory[2]);

            //jsonObject.put("", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }


    /**
     * 获取内存使用率
     *
     * @return
     */
    private static Object[] getMemory() {
        Object[] rate = new Object[3];
        try {
            long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            //当前分配的总内存
            long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            //剩余内存
            long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);

            rate[0] = maxMemory;
            rate[1] = (totalMemory - freeMemory) * 100 / totalMemory;
            rate[2] = totalMemory;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rate;
    }

    /**
     * 获取CPU使用率
     *
     * @return
     */
    private static int[] getCpu() {
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
                    String[] cpuUsage = cpuUser[0].split("User");
                    String[] sysUsage = cpuUser[1].split("System");

                    rate[0] = Integer.parseInt(cpuUsage[1].trim());
                    rate[1] = Integer.parseInt(sysUsage[1].trim());
                    break;
                }
            }

        } catch (Exception e) {
            //e.printStackTrace();
        }
        return rate;
    }

    /**
     * 获取唯一码
     *
     * @return
     */
    private static String getUID() {
        return Settings.Secure.getString(Configure.context.getContentResolver(), Settings.Secure.ANDROID_ID);
//        if (android.os.Build.VERSION.SDK_INT < 23) {
//            return ((TelephonyManager) Configure.context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
//        } else {
//            return Settings.Secure.getString(Configure.context.getContentResolver(), Settings.Secure.ANDROID_ID);
//        }
    }

    /**
     * netWork		1	2G
     * netWork		2	3G
     * netWork		3	wifi
     * netWork		4	4G
     * netWork		5	5G
     */
    private static int getNetworkState() {
        Context context = Configure.context;
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

    private static String getIp() {
        WifiManager wifi = (WifiManager) Configure.context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        int i = info.getIpAddress();  //获取ip地址
        return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
                + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
    }

}
