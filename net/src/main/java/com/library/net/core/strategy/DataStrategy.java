package com.library.net.core.strategy;

import com.library.net.BuildConfig;

/**
 * 报文上传策略
 */
public class DataStrategy {

    public static DataStrategy sDefault = new DataStrategy();


    public enum UploadStrategy {
        IMMEDIATELY, //直接发送
        FREE, //空闲发送
        DELAY; //延迟发送
    }

    /**
     * 线程配置
     * @param threadCount
     * @return
     */
    public DataStrategy threadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    /**
     * 重试次数
     * @param retryCount
     * @return
     */
    public DataStrategy retryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    /**
     * 策略
     * @param strategy
     * @return
     */
    public DataStrategy strategy(UploadStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    /**
     * 是否加密
     * @param encode
     * @return
     */
    public DataStrategy encryptData(boolean encode) {
        this.encode = encode;
        return this;
    }

    /**
     * 是否捕捉异常
     * @param catchException
     * @return
     */
    public DataStrategy catchException(boolean catchException) {
        this.catchException = catchException;
        return this;
    }

    /**
     * 是否打印日志
     * @param log
     * @return
     */
    public DataStrategy logcat(boolean log) {
        logcat = log;
        return this;
    }

    /**
     * 是否打印日志，默认debug模式下打印，release下关闭
     */
    public static boolean logcat = BuildConfig.DEBUG;

    /**
     * 同时启动的上报线程数量
     */
    public int threadCount = 1;

    /**
     * 失败重试次数
     */
    public int retryCount = -1;

    /**
     * 发送策略
     */
    public UploadStrategy strategy = UploadStrategy.IMMEDIATELY;


    /**
     * 是否加密
     */
    public boolean encode = false;

    /**
     * 是否捕捉异常
     */
    public boolean catchException = true;

}
