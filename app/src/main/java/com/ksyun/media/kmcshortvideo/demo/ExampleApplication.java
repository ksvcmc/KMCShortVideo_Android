package com.ksyun.media.kmcshortvideo.demo;

import android.app.Application;

import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by sujia on 2017/7/14.
 */

public class ExampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        /*
         * 初始化Bugly，需要传入注册时申请的APPID，第三个参数为SDK调试模式开关；
         * 建议在测试阶段建议设置成true，发布时设置为false。
         * Bugly为应用崩溃日志收集工具，开发者可根据实际情况选择不集成或依赖其它Bug收集工具
         */
        CrashReport.initCrashReport(getApplicationContext(), "4e98881bde", false);
    }
}
