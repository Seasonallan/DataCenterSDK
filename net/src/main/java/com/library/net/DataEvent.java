package com.library.net;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 事件
 */
public class DataEvent implements Serializable {

    public void parseJson(JSONObject jsonObject) {
        try {
            jsonObject.put("eventType", eventType);
            jsonObject.put("eventCode", eventCode);

            jsonObject.put("pageCode", pageCode);
            jsonObject.put("startType", startType);
            jsonObject.put("exceptionTime", exceptionTime);
            jsonObject.put("exceptionIp", exceptionIp);
            jsonObject.put("operatingItems", operatingItems);
            jsonObject.put("eventDescription", eventDescription);

            jsonObject.put("usageTime", usageTime);
            jsonObject.put("loadType", loadType);
            jsonObject.put("resourceSize", resourceSize);
            jsonObject.put("loadTime", loadTime);
            jsonObject.put("completionTime", completionTime);

            jsonObject.put("networkSpeed", networkSpeed);
            jsonObject.put("originalAddress", originalAddress);
            jsonObject.put("definition", definition);
            jsonObject.put("operationTime", operationTime);

            jsonObject.put("startFinishiTime", startFinishiTime);
            jsonObject.put("startElapsedTime", startElapsedTime);
            jsonObject.put("pageCode", pageCode);

            jsonObject.put("installTime", installTime);
            jsonObject.put("operationItems", operationItems);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    long time;

    /**
     * 装机事件
     *
     * @return
     */
    public static DataEvent INSTALL() {
        DataEvent dataEvent = new DataEvent();
        dataEvent.eventType = 1;
        dataEvent.time = System.nanoTime();
        dataEvent.eventCode = "install";
        dataEvent.pageCode = "App_Global";
        dataEvent.installTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        return dataEvent;
    }

    /**
     * 冷热启动
     * 冷：进程销毁
     * 热：切换进程
     *
     * @param isHot 冷热
     * @return
     */
    public static DataEvent START(boolean isHot) {
        DataEvent dataEvent = new DataEvent();
        dataEvent.eventType = 2;
        dataEvent.time = System.nanoTime();
        dataEvent.eventCode = isHot ? "hotBoot" : "coldBoot";
        dataEvent.startType = isHot ? 2 : 1;
        dataEvent.pageCode = "App_Global";
        dataEvent.startFinishiTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        dataEvent.startElapsedTime = "0";

        return dataEvent;
    }


    /**
     * 异常日志
     *
     * @return
     */
    public static DataEvent EXCEPTION(Throwable e) {
        DataEvent dataEvent = new DataEvent();
        dataEvent.eventType = 3;
        dataEvent.time = System.nanoTime();
        dataEvent.eventCode = "exception";
        dataEvent.pageCode = DataCenterEngine.getCurrentPageCode();
        dataEvent.exceptionTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        if (e != null) {
            dataEvent.operatingItems = e.getMessage();
            dataEvent.eventDescription = collectExceptionInfo(e);
        }
        return dataEvent;
    }

    /**
     * 获取捕获异常的信息
     */
    private static String collectExceptionInfo(Throwable ex) {
        Writer mWriter = new StringWriter();
        PrintWriter mPrintWriter = new PrintWriter(mWriter);
        ex.printStackTrace(mPrintWriter);
        ex.printStackTrace();
        Throwable mThrowable = ex.getCause();
        // 迭代栈队列把所有的异常信息写入writer中
        while (mThrowable != null) {
            mThrowable.printStackTrace(mPrintWriter);
            // 换行 每个个异常栈之间换行
            mPrintWriter.append("\r\n");
            mThrowable = mThrowable.getCause();
        }
        // 记得关闭
        mPrintWriter.close();
        return mWriter.toString();
    }


    /**
     * 页面加载
     *
     * @param pageCode 页面代码
     * @return
     */
    public static DataEvent PAGELOAD(String pageCode) {
        DataEvent dataEvent = new DataEvent();
        dataEvent.eventType = 4;
        dataEvent.time = System.nanoTime();
        dataEvent.eventCode = "PAGELOAD";
        dataEvent.pageCode = pageCode;
        dataEvent.operationTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        return dataEvent;
    }


    /**
     * 图片视频
     *
     * @return
     */
    public static DataEvent IMAGEVEDIO(String originalAddress) {
        DataEvent dataEvent = new DataEvent();
        dataEvent.eventType = 5;
        dataEvent.time = System.nanoTime();
        dataEvent.eventCode = "IMAGEVEDIO";
        dataEvent.pageCode = DataCenterEngine.getCurrentPageCode();
        dataEvent.originalAddress = originalAddress;
        dataEvent.loadTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        return dataEvent;
    }

    /**
     * 操作日志
     *
     * @param eventCode 事件编码
     * @param operation 前置场景
     * @param note      备注
     * @return
     */
    public static DataEvent OPERATION(String eventCode,
                                      String operation, String note) {
        DataEvent dataEvent = new DataEvent();
        dataEvent.eventType = 6;
        dataEvent.time = System.nanoTime();
        dataEvent.eventCode = eventCode;
        dataEvent.pageCode = DataCenterEngine.getCurrentPageCode();
        dataEvent.operationItems = operation;
        dataEvent.eventDescription = note;
        dataEvent.operationTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        return dataEvent;
    }

    /**
     * 操作日志
     *
     * @param eventCode 事件编码
     * @return
     */
    public static DataEvent OPERATION(String eventCode) {
        return OPERATION(eventCode, null, null);
    }

    public int getEventType() {
        return eventType;
    }

    /**
     * eventType	1	装机事件
     * eventType	2	启动事件
     * eventType	3	异常事件
     * eventType	4	页面加载事件
     * eventType	5	图片视频加载事件
     * eventType	6	操作事件
     */
    private int eventType;
    private String eventCode;

    private String startFinishiTime;
    private String startElapsedTime;

    private String operationItems;

    private String pageCode;

    private String installTime;

    /**
     * startType	1	冷启动
     * startType	2	热启动
     */
    private int startType;


    //异常日志
    private String exceptionTime;//"操作事项",
    private String exceptionIp;//"事件描述",


    private String operatingItems;//"操作事项",
    private String eventDescription;//"事件描述",

    //页面加载
    private int usageTime = 0;//
    private String loadType = "接口调用";//


    //图片视屏
    private String resourceSize;//
    private String loadTime;//
    private String completionTime;//
    private String networkSpeed;//
    private String originalAddress;//
    private String definition;//

    // 操作日志
    private String operationTime;//

    private int level = 1;//

    public String getCacheString() {
        return time + "_" + eventType + "_" + level;
    }

}
