package com.sogou.translate.example

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.util.Log
import com.sogou.iot.arch.probe.ProbeEngine


class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        mContext = this

        //初始化log
        ProbeEngine.setContext(this).setupLog()

        if(isMainProgress()){
            //初始化crash 和block catcher
            ProbeEngine.setContext(this)
                .setLogContext(MyLogContext())
                .setBlockContext(MyComBlockContext())
                .setCrashContext(MyCrashContext())
                .install()
        }
    }

    fun isMainProgress(): Boolean {
        var myprocessName = getProcessName(this, android.os.Process.myPid())
        Log.d("feifei","myprocessName:${myprocessName}")
        return "com.sogou.translate.example".equals(myprocessName)
    }


    fun getProcessName(context: Context, pid:Int):String?{
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses ?: return null
        for (procInfo in runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName
            }
        }
        return null
    }

    companion object{

        lateinit var mContext:MyApplication
        fun getAppContext():Application{
            return mContext;
        }
    }

}
