package com.sogou.iot.arch

import android.app.ActivityManager
import android.app.Application
import android.content.Context.ACTIVITY_SERVICE
import android.util.Log


object MemoryWatcher {

    val TAG = MemoryWatcher::class.java.simpleName

    @Volatile var isWatching = false
    var mCheckInterval = 1000L //ms
    lateinit var mContext:Application

    fun startWatch(context:Application){
        mContext = context
        tryWatch()
    }

    fun stopWatch(){
        Log.d(TAG,"stopWatch isWatching:$isWatching")

        isWatching = false
    }

    fun tryWatch(){
        Log.d(TAG,"tryWatch isWatching:$isWatching")
        if(isWatching)return
        isWatching = true
        doWatch()
    }

    fun doWatch(){
        Thread{
            while(isWatching){
                val activityManager = mContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val info = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(info)
                Log.i(TAG, "zhl-系统总内存:" + info.totalMem / 1024 / 1024)
                Log.i(TAG, "zhl-系统是否处于低内存运行：" + info.lowMemory)
                Log.i(TAG, "zhl-当系统剩余内存低于" + info.threshold / 1024 / 1024 + "时就看成低内存运行")
                Log.i(TAG, "zhl-系统可用内存:" + info.availMem / 1024 / 1024)

                Thread.sleep(mCheckInterval)
            }
        }.start()
    }
}

