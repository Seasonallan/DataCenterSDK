package com.library.net;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.library.net.core.CacheFileUtil;
import com.library.net.core.HttpCallback;
import com.library.net.core.HttpMethod;
import com.library.net.core.HttpUtils;
import com.library.net.core.RequestHelper;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 数据中台 埋点引擎
 */
public class DataCenterEngine {

    private String url_auth = "http://datacenter-developer-dev.aitdcoin.com/api/developer/v1/services/auth";
    private String url_report = "http://172.31.17.22:9889/api/point/v1/report";

    private String appId;
    private String appSecret;

    private DataStrategy dataStrategy;

    private Context context;
    private String token;

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

    private Thread.UncaughtExceptionHandler mOriginalHandler;

    /**
     * 启动中台服务
     *
     * @param application
     */
    public static void start(Application application) {
        instance().init(application.getApplicationContext());
        instance().mOriginalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
                report(DataEvent.EXCEPTION(ex));
                if (instance().mOriginalHandler != null) {
                    instance().mOriginalHandler.uncaughtException(thread, ex);
                }
            }
        });

        instance().token = application.getSharedPreferences("dc_status",
                Context.MODE_PRIVATE).getString("token", null);
        if (!instance().context.getSharedPreferences("dc_status",
                Context.MODE_PRIVATE).getBoolean("install", false)) {
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
                if (activity instanceof IPageCode) {
                    setCurrentPageCode(((IPageCode) activity).getPageCode());
                }
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
        //事件排序 dataEventList
    }

    public void eventReport(DataEvent dataEvent) {
        String key = dataEvent.getCacheString();
        CacheFileUtil.saveSerialData(key, dataEvent, dir);
        if (BuildConfig.DEBUG) {
            Log.e("DataCenter", key + ">> " + new File(dir, key).isFile() + ", " + new File(dir, key).length());
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
            if (BuildConfig.DEBUG) {
                Log.e("DataCenter", ev.getCacheString() + ">> error");
            }
        }
    }


    //事件消费 完成
    private void eventConsume(DataEvent... dataEvent) {
        for (DataEvent ev : dataEvent) {
            String id = ev.getCacheString();
            new File(dir, id).delete();
            if (BuildConfig.DEBUG){
                Log.e("DataCenter", "delete>>" + id);
            }
        }
        continueTask();
    }

    private void continueTask() {
        if (dataEventList.size() > 0) {
            if (TextUtils.isEmpty(token)) {
                auth();
            } else {
                String key = dataEventList.remove(0);
                if (BuildConfig.DEBUG) {
                    Log.e("DataCenter", key);
                }
                DataEvent dataEvent = CacheFileUtil.getSerialData(key, dir);
                if (dataEvent == null) {
                    if (BuildConfig.DEBUG) {
                        Log.e("DataCenter", "empty");
                        new File(dir, key).delete();
                        continueTask();
                        return;
                    }

                }
                request(dataEvent);
            }
        }
    }

    private int consumeThreadCount = 0;

    private void request(final DataEvent dataEvent) {
        consumeThreadCount++;
        JSONObject jsonObject = HttpUtils.common(context, userId, appId);
        dataEvent.parseJson(jsonObject);
        new RequestHelper.Builder(HttpMethod.POST, url_report)
                .header(headerMap())
                .jsonData(jsonObject.toString())
                .callback(new ResponseParse() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        eventError(dataEvent);
                    }

                    @Override
                    public void onResponse(Response response) {
                        if (response.code == 1) {//no authoritative, reAuth
                            eventError(dataEvent);
                            auth();
                        } else {
                            consumeThreadCount--;
                            if (dataEvent.getEventType() == 1) {
                                instance().context.getSharedPreferences("dc_status",
                                        Context.MODE_PRIVATE).edit().putBoolean("install", true).commit();
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

    private void auth() {
        consumeThreadCount++;
        new RequestHelper.Builder(HttpMethod.POST, url_auth)
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
                            token = response.data;
                            instance().context.getSharedPreferences("dc_status",
                                    Context.MODE_PRIVATE).edit().putString("token", token).commit();
                            consumeThreadCount--;
                            continueTask();//继续下一个
                        }

                    }
                }).retryCount(dataStrategy.retryCount).execute();
    }

    private Map<String, String> paramsMap() {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("appId", appId);
        headerMap.put("appSecret", appSecret);
        return headerMap;
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
                if (BuildConfig.DEBUG)
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

    private Map<String, String> headerMap() {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json;charset=UTF-8");
        headerMap.put("X-Auth-Token", token);
        return headerMap;
    }

}
