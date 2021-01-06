package com.library.net.core;

import com.library.net.BuildConfig;
import com.library.net.DataEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 本地序列化数据存储方案
 */
public class CacheFileUtil {



    public static DataEvent getSerialData(String key, File dir){
        return getSerialData(key, dir.getAbsolutePath());
    }
    /**
     * 获取Serializable类型数据结构
     * @param key
     * @return
     */
    public static DataEvent getSerialData(String key, String filePath){

        FileInputStream fileInputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            File file = new File(filePath, key);
            if (file.isFile() && file.length() > 0){
                fileInputStream = new FileInputStream(file.toString());
                objectInputStream = new ObjectInputStream(fileInputStream);

                return (DataEvent) objectInputStream.readObject();
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG){
                e.printStackTrace();
            }
        }finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static boolean saveSerialData(String key, DataEvent value, File dir) {
        return saveSerialData(key, value, dir.getAbsolutePath());
    }

    /**
     * 存储Serializable类型数据结构到文件
     * @param key
     * @param value
     */
    public static boolean saveSerialData(String key, DataEvent value, String filePath){

        FileOutputStream fileOutputStream=null;
        ObjectOutputStream objectOutputStream =null;
        try {
            File file = new File(filePath, key);
            if (file.getParentFile() != null && !file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            fileOutputStream= new FileOutputStream(file.toString());
            objectOutputStream= new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(value);
            return true;
        } catch (Exception e) {
            if (BuildConfig.DEBUG){
                e.printStackTrace();
            }
        }finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

}
