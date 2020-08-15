package com.sogou.iot.arch.db.ex

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import java.io.File

fun String.folderEndPath():String{
    if(!this.endsWith(File.separator)){
        return this+File.separator
    }
    return this
}


/**获取系统内存状态*/
fun Any.getSystemMemoryInfo(context: Context):String{
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(info)
    var  sb = StringBuilder()
    sb.append("系统总内存:" + info.totalMem / 1024 / 1024+"M\n")
    sb.append("系统是否处于低内存运行：" + info.lowMemory+"\n")
    sb.append("当系统剩余内存低于" + info.threshold / 1024 / 1024 + "M时就看成低内存运行\n")
    sb.append("系统可用内存:" + info.availMem / 1024 / 1024+"M\n")
    return sb.toString()
}

fun getProcessName(context: Context,pid:Int):String?{
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val runningApps = am.runningAppProcesses ?: return null
    for (procInfo in runningApps) {
        if (procInfo.pid == pid) {
            return procInfo.processName
        }
    }
    return null
}