package com.library.net;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.library.net.core.HttpCallback;
import com.library.net.core.HttpMethod;
import com.library.net.core.HttpUtils;
import com.library.net.core.RequestHelper;
import com.library.net.core.strategy.CacheStrategy;
import com.library.net.core.strategy.DataStrategy;

import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 数据中台 埋点引擎
 */
public class DataCenterEngine {

    private String url_report = "http://172.31.17.22:9889/api/point/v1/report";

    private String appId;
    private String appSecret;

    private DataStrategy dataStrategy;

    private Context context;

    private static volatile DataCenterEngine instance;
    private String userId;

    private DataCenterEngine() {
        dataEventList = new CopyOnWriteArrayList<>();
        dataStrategy = new DataStrategy();
    }

    private static DataCenterEngine instance() {
        if (instance == null) {
            synchronized (DataCenterEngine.class) {
                if (instance == null) {
                    instance = new DataCenterEngine();
                }
            }
        }
        return instance;
    }

    private CopyOnWriteArrayList<String> dataEventList;

    private String pageCode;


    /**
     * 获取当前页面的编码
     *
     * @return
     */
    public static String getCurrentPageCode() {
        return instance().pageCode;
    }

    /**
     * 设置当前页面的编码，原生可以不调用，自动设置
     *
     * @return
     */
    public static void setCurrentPageCode(String pageCode) {
        instance().pageCode = pageCode;
    }

    /**
     * 策略配置
     *
     * @param dataStrategy
     * @return
     */
    public static DataCenterEngine strategy(DataStrategy dataStrategy) {
        instance().dataStrategy = dataStrategy;
        return instance();
    }


    /**
     * 配置appId
     *
     * @param appId
     * @param appSecret
     * @return
     */
    public static DataCenterEngine configure(String appId, String appSecret) {
        instance().appId = appId;
        instance().appSecret = appSecret;
        return instance();
    }

    /**
     * 配置用户ID
     *
     * @param userId
     * @return
     */
    public static DataCenterEngine user(String userId) {
        instance().userId = userId;
        return instance();
    }

    /**
     * 配置环境
     *
     * @param reportUrl 上报地址
     * @return
     */
    public static DataCenterEngine environment(String reportUrl) {
        instance().url_report = reportUrl;
        return instance();
    }

    private Thread.UncaughtExceptionHandler mOriginalHandler;

    /**
     * 启动中台服务
     *
     * @param application
     */
    public static void start(Application application) {
        instance().init(application.getApplicationContext());

        if (!instance().context.getSharedPreferences("dc_status",
                Context.MODE_PRIVATE).getBoolean("install2.0", false)) {
            //上传装机日志
            report(DataEvent.INSTALL());
        }
        //上传冷启动日志
        report(DataEvent.START(false));

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            long pauseTime = System.currentTimeMillis();

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                pauseTime = System.currentTimeMillis();
                if (activity instanceof IPageCode) {
                    setCurrentPageCode(((IPageCode) activity).getPageCode());
                    if (!TextUtils.isEmpty(getCurrentPageCode())) {
                        //上传页面打开日志
                        DataCenterEngine.report(DataEvent.PAGELOAD(getCurrentPageCode()));
                    }
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                String pageCode = null;
                if (activity instanceof IPageCode) {
                    pageCode = ((IPageCode) activity).getPageCode();
                }
                if (TextUtils.isEmpty(pageCode)) {
                    pageCode = "Base_Page";
                }
                setCurrentPageCode(pageCode);
                if (System.currentTimeMillis() - pauseTime > 2 * 1000) {
                    //发送热启动
                    report(DataEvent.START(true));
                }

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                pauseTime = System.currentTimeMillis();
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });
    }

    File dir;

    private void init(Context context) {
        this.context = context;
        dir = new File(context.getCacheDir(), "event");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File[] listFile = dir.listFiles();
        if (listFile != null) {
            for (File file : listFile) {
                dataEventList.add(file.getName());
            }
        }
        //TODO:事件排序 dataEventList


        //捕捉异常
        if (dataStrategy.catchException) {
            mOriginalHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
                    report(DataEvent.EXCEPTION(ex));
                    mOriginalHandler.uncaughtException(thread, ex);
                }
            });
        }
    }

    public void eventReport(DataEvent dataEvent) {
        if (context == null) {
            Log.e("DataCenter", "Please start the engine before reporting the event!!!");
            return;
        }
        String key = dataEvent.getCacheString();
        CacheStrategy.saveSerialData(key, dataEvent, dir);
        if (DataStrategy.logcat) {
            Log.e("DataCenter", "eventReport(key:" + key + ", length:" + new File(dir, key).length() + ")");
        }
        dataEventList.add(0, key);
        if (consumeThreadCount < dataStrategy.threadCount) {
            continueTask();
        }
    }

    //事件消费 异常
    private void eventError(DataEvent... dataEvent) {
        consumeThreadCount--;
        for (DataEvent ev : dataEvent) {
            dataEventList.add(0, ev.getCacheString());
            if (DataStrategy.logcat) {
                Log.e("DataCenter", ev.getCacheString() + ">> error");
            }
        }
    }


    //事件消费 完成
    private void eventConsume(DataEvent... dataEvent) {
        for (DataEvent ev : dataEvent) {
            String id = ev.getCacheString();
            new File(dir, id).delete();
            if (DataStrategy.logcat) {
                Log.e("DataCenter", "delete>>" + id);
            }
        }
        continueTask();
    }

    private void continueTask() {
        if (dataEventList.size() > 0) {
            String key = dataEventList.remove(0);
            if (DataStrategy.logcat) {
                Log.e("DataCenter", key);
            }
            DataEvent dataEvent = CacheStrategy.getSerialData(key, dir);
            if (dataEvent == null) {
                if (DataStrategy.logcat) {
                    Log.e("DataCenter", key + " empty");
                }
                new File(dir, key).delete();
                continueTask();
                return;
            }
            if (dataEvent.getEventType() == 3) {
                if (DataStrategy.logcat) {
                    Log.e("DataCenter", "delete>>" + key + " exception report: delete file first, for no callback to run");
                }
                new File(dir, key).delete();
            }
            request(dataEvent);
        }
    }


    private int consumeThreadCount = 0;

    private void request(final DataEvent dataEvent) {
        consumeThreadCount++;
        JSONObject jsonObject = HttpUtils.common(context, userId, appId);
        dataEvent.parseJson(jsonObject);
        new RequestHelper.Builder(HttpMethod.POST, url_report)
                .header(headerMap(jsonObject))
                .jsonData(jsonObject.toString())
                .callback(new ResponseParse() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        eventError(dataEvent);
                    }

                    @Override
                    public void onResponse(Response response) {
                        if (DataStrategy.logcat) {
                            Log.e("DataCenter", "response=" + response.code);
                        }
                        if (response.code == 1) {//no authoritative, reAuth
                            eventError(dataEvent);
                        } else {
                            consumeThreadCount--;
                            if (dataEvent.getEventType() == 1) {
                                instance().context.getSharedPreferences("dc_status",
                                        Context.MODE_PRIVATE).edit().putBoolean("install2.0", true).commit();
                            }
                            eventConsume(dataEvent);//继续下一个
                        }
                    }
                }).retryCount(dataStrategy.retryCount).execute();
    }


    /**
     * 上报日志
     *
     * @param dataEvent
     */
    public static void report(DataEvent dataEvent) {
        instance().eventReport(dataEvent);
    }


    private abstract class ResponseParse extends HttpCallback<Response> {

        @Override
        public Response onParseResponse(String result) {
            Response response = new Response();
            if (TextUtils.isEmpty(result)) {
                return response;
            }
            try {
                JSONObject jsonObject = new JSONObject(result);
                if (jsonObject.has("msg"))
                    response.msg = jsonObject.getString("msg");
                if (jsonObject.has("code"))
                    response.code = jsonObject.getInt("code");
                if (jsonObject.has("data"))
                    response.data = jsonObject.getString("data");
            } catch (Exception e) {
                if (DataStrategy.logcat)
                    e.printStackTrace();
            }
            return response;
        }

    }


    private class Response {
        public int code = -1;
        public String msg;
        public String data;
    }

    private Map<String, String> headerMap(JSONObject jsonObject) {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json;charset=UTF-8");
        long timestamp = System.currentTimeMillis();
        headerMap.put("timestamp", timestamp + "");
        headerMap.put("sign", sign(jsonObject, timestamp));
        return headerMap;
    }


    public String sign(JSONObject jsonObject, long timestamp) {
        //1、取body中所有的参数按参数名进行排序（升序），依次取取参数名，参数值， 连接在一个字符串中得到 secretStr
        Iterator<String> iterator = jsonObject.keys();
        List<String> keys = new ArrayList<>();
        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }
        Collections.sort(keys);
        String secretStr = "";
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = null;
            try {
                value = jsonObject.get(key) == null ? "" : jsonObject.get(key).toString();
            } catch (Exception e) {
            }
            secretStr = secretStr + key + value;
        }
        //2、用 appId + secretStr + appSecret + timestamp 连接得到一个signStr
        String signStr = appId + secretStr + appSecret + timestamp;
        //3、sign 的值为 Md5Utils.getMD5(sing.toString())
        return getMD5(signStr.getBytes());
    }

    public static String getMD5(byte[] bytes) {
        char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] str = new char[32];
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bytes);
            byte[] tmp = md.digest();
            int k = 0;
            for (int i = 0; i < 16; ++i) {
                byte byte0 = tmp[i];
                str[k++] = hexDigits[byte0 >>> 4 & 15];
                str[k++] = hexDigits[byte0 & 15];
            }
        } catch (Exception var8) {
            var8.printStackTrace();
        }
        return new String(str);
    }
}
