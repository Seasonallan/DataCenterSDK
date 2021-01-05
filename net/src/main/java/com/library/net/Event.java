package com.library.net;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Event {

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Event START(boolean isHot) {
        Event event = new Event();
        event.eventType = 2;
        event.eventCode = isHot ? "hotBoot" : "coldBoot";
        event.startType = isHot ? 1 : 2;
        event.pageCode = "login";
        event.startFinishiTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        event.startElapsedTime = "50";

        return event;
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

    private String pageCode;

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
    private String usageTime;//
    private String loadType;//


    //图片视屏
    private String resourceSize;//
    private String loadTime;//
    private String completionTime;//
    private String networkSpeed;//
    private String originalAddress;//
    private String definition;//

    // 操作日志
    private String operationTime;//


}
