# 第一步
在根目录的 build.gradle中 allprojects的repositories里添加jitpack依赖
maven { url 'https://jitpack.io' }
# 第二步
在app项目的build.gradle下的dependencies中添加DataCenter库依赖
    implementation 'com.github.Seasonallan:DataCenterSDK:1.3'
# 第三步
使用DataCenterEngine调用库API

1、 在Application中启动引擎，自动埋点装机日志和启动冷热日志和异常日志：
```
        DataCenterEngine
                .strategy(DataStrategy.sDefault.threadCount(1).retryCount(0)
                        .strategy(DataStrategy.UploadStrategy.IMMEDIATELY).encryptData(false).catchException(true).logcat(true))
                .configure("5d0660c0-b67d-47e3-b0eb-10649d4c382f", "ff93de60-b4bd-4f98-8ae3-2d7aeda08ede")
                .environment("http://172.93.1.253:9889/api/point/v1/report")
                .user("10086")
                .start(this);
```
2、登录后可以设置用户ID
```
        DataCenterEngine.user("10086");
```
3、原生开发：在BaseActivity继承IPageCode，在需要统计页面启动的Activity实现getPageCode方法，自动埋点页面加载日志

   Flutter开发：在需要统计页面启动的地方添加 `DataCenterEngine.report(DataEvent.PAGELOAD("login"));`
   
4、操作日志上报：`DataCenterEngine.report(DataEvent.OPERATION("changeLanguage",
                        "切换语言", "之前【英语】修改成【中文】"));`
 

# appId appSecret申请手册
1、开发环境申请地址：http://datacenter-developer-dev.aitdcoin.com/  
开发环境上报地址：http://172.93.1.253:9889/api/point/v1/report

2、正式环境申请地址：https://datacenter-developer.aitdcoin.com/  
开发环境上报地址：http://datacenter-push-log.aitdcoin.com/api/point/v1/report

# 注意事项
1、使用demo时，gradle版本不一致方案：
中断更新gradle
修改根目录下的build.gradle中的classpath "com.android.tools.build:gradle:4.0.1" 为你的版本
修改根目录下的gradle/wrapper/gradle-wrapper.properties中的distributionUrl为你的版本
关闭重启项目
2、混淆配置，保持序列号模型
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
 