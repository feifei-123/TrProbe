package com.sogou.tm.commonlib.log.utils;

import android.util.Log;

/**
 * 文件名:ALog
 * 创建者:baixuefei
 * 创建日期:2020/5/18 4:25 PM
 * 职责描述: 管理系统的Log操作
 */


public class ALog {
    public static final String TAG = "feifei_log ";

    public static void i( String msg){
        Log.i(TAG,msg);
    }

    public static void i(String tag, String msg){
        Log.i(TAG+tag,msg);
    }

    public static void d(String msg){
        Log.d(TAG,msg);
    }

    public static void d(String tag, String msg){
        Log.d(TAG+tag,msg);
    }

    public static void e(String msg){
        Log.e(TAG,msg);
    }

    public static void e(String tag, String msg){
        Log.e(TAG+tag,msg);
    }

    public static void e(String msg, Throwable e){
        Log.e(TAG,msg,e);
    }

    public static void e(String tag, String msg, Throwable e){
        Log.e(TAG+tag,msg,e);
    }
}
