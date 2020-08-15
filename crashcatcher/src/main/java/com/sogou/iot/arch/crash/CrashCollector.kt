package com.sogou.iot.arch.crash

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.sogou.iot.arch.db.CrashSource

import com.sogou.iot.arch.db.HandlerThreadFactory
import com.sogou.iot.arch.db.LINESEPERATOR
import com.sogou.iot.arch.db.ex.folderEndPath
import com.sogou.iot.arch.db.ex.getSystemMemoryInfo
import com.sogou.iot.arch.db.utils.ShellUtils

import java.io.File
import java.util.HashMap

object CrashCollector {
    // 用来存储设备信息和异常信息
    private val infos = HashMap<String, String>()

    private val TAG = CrashCollector::class.java.simpleName


    fun doCollectExtraInfo(crashInfo: CrashInfo) {
        collectDeviceInfo(TRCrashMonitor.mCrashContext.getAppContext()!!)
        crashInfo.deviceInfo = getDeviceInfo()

        crashInfo.fileList.clear()
        crashInfo.fileList.add(getTmLogFileName(crashInfo))
        if(TRCrashMonitor.mCrashContext.doCollectLogcat()){
            crashInfo.fileList.add(getLogcatFileName(crashInfo))
        }

        Log.d(TAG,"getCrashCollectType:${TRCrashMonitor.mCrashContext.getCrashCollectType()}")
        if (TRCrashMonitor.mCrashContext.getCrashCollectType() == CrashCollectType.OTHERPROCESS) {

            //保存tmlog 文件
            saveLogWithPathByOtherProcess(
                crashInfo.generateCrashMsg(),
                TRCrashMonitor.mCrashContext.getLogSavePath().folderEndPath() + getTmLogFileName(crashInfo)
            )
            //保存logcat文件
            if (TRCrashMonitor.mCrashContext.doCollectLogcat()) checkCrashLogcatInfoByOtherProcess(crashInfo)
        } else {
            saveLogWithPathBySelf(
                crashInfo.generateCrashMsg(),
                TRCrashMonitor.mCrashContext.getLogSavePath().folderEndPath()+ getTmLogFileName(crashInfo)
            )
        }

        var rowid = CrashSource.insertCrashInfo(crashInfo)
        Log.d(TAG, "doCollectExtraInfo - insertCrash:${rowid},fileList:${crashInfo.fileList}")
    }


    fun checkCrashLogcatInfoByOtherProcess(crashInfo: CrashInfo) {
        var logcatSaveName = getLogcatFileName(crashInfo)
        var logcatSavePath = TRCrashMonitor.mCrashContext.getLogSavePath().folderEndPath() + logcatSaveName;
        TRCrashMonitor.mCrashContext.collectLogcatByOtherProcess(
            logcatSavePath,
            TRCrashMonitor.mCrashContext.collectLogcatDelay(),
            TRCrashMonitor.mCrashContext.clearLogcatOnCrash()
        )
    }

    fun getLogcatFileName(crashInfo: CrashInfo):String{
        var logcatSaveName = if (crashInfo.crashType == CrashType.NATIVECRASH) {
            crashInfo.id + "_nativecrash.logcat"
        } else {
            crashInfo.id + "_javacrash.logcat"
        }
        return logcatSaveName
    }

    fun getTmLogFileName(crashInfo: CrashInfo):String{
        var logInfoName = if (crashInfo.crashType == CrashType.NATIVECRASH) {
            crashInfo.id + "_nativecrash.tmlog"
        } else {
            crashInfo.id + "_javacrash.tmlog"
        }
        return logInfoName
    }

    fun saveLogWithPathByOtherProcess(log: String, path: String) {
        TRCrashMonitor.mCrashContext.saveLogWithPathByOtherProcess(log, path)
    }

    /**
     * app重启时 收集logcat 信息
     */
    fun saveCrashLogcatInfoByReboot(crashInfo: CrashInfo) {
        HandlerThreadFactory.getTimerThreadHandler().postDelayed({
            saveCrashLogcatInfoBySelf(crashInfo)
            CrashSource.updateCrashInfo(crashInfo)
        }, TRCrashMonitor.mCrashContext.collectLogcatDelay())
    }



    /**
     * 在当前进程 收集logcat信息 保存到本地文件
     */
    fun saveCrashLogcatInfoBySelf(crashInfo: CrashInfo){
        var logcatSaveName = getLogcatFileName(crashInfo)

        var logcatSavePath = TRCrashMonitor.mCrashContext.getLogSavePath().folderEndPath() + logcatSaveName
        val file = File(logcatSavePath)
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs()
            Log.d("feifei", "doCollectLogCat parent not exist,create it~")
        }
        val cmd = "logcat -d > $logcatSavePath"
        Log.d("doCollectLogCat", cmd)
        ShellUtils.execCommand(cmd, false)
        if (TRCrashMonitor.mCrashContext.clearLogcatOnCrash()) {
            val cmd_clear = "logcat -c"
            ShellUtils.execCommand(cmd_clear, false)
            Log.d("doCollectLogCat", "clear logcat  :$cmd_clear")
        }
    }

    /**
     * 保存文件到本地
     */
    fun saveLogWithPathBySelf(log: String, path: String) {
        Log.d(TAG, "saveLogWithPathBySelf - path: ${path}")
       saveLogWithPath(log, path)
    }


    fun saveLogWithPath(log:String,path:String){
        var file = File(path)
        if(!file.parentFile.exists())file.parentFile.mkdirs()
        if(file.exists()) file.delete()
        file.createNewFile()
        file.appendText(log)
    }
    /**
     * 收集设备参数信息
     *
     * @param ctx
     */
    fun collectDeviceInfo(ctx: Context) {
        try {
            infos.clear()
            infos["pkgName:"] = ctx.packageName
            var procceName = getProcessName(ctx, android.os.Process.myPid())
            procceName?.let {
                infos["processName:"] = it
            }

            val pm = ctx.packageManager
            val pi = pm.getPackageInfo(ctx.packageName, PackageManager.GET_ACTIVITIES)

            if (pi != null) {
                val versionName = if (pi.versionName == null) "null" else pi.versionName
                val versionCode = pi.versionCode.toString() + ""
                infos["versionName"] = versionName
                infos["versionCode"] = versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "an error occured when collect package info", e)
        }

        val fields = Build::class.java.declaredFields
        for (field in fields) {
            try {
                field.isAccessible = true
                infos[field.name] = field.get(null).toString()
            } catch (e: Exception) {
                Log.e(TAG, "an error occured when collect crash info", e)
            }

        }
    }

    fun getDeviceInfo(): String {
        val sb = StringBuilder()
        for (theKey in infos.keys) {
            val key = theKey
            val value = infos.get(theKey)
            sb.append(key + "=" + value + LINESEPERATOR)
        }

        sb.append("==========================="+LINESEPERATOR)
        sb.append(sb.getSystemMemoryInfo(TRCrashMonitor.mCrashContext.getAppContext()!!)+LINESEPERATOR)

        return sb.toString()
    }


    fun printDeviceInfo() {
        val sb = StringBuilder()
        sb.append("====================init=========================\n")
        sb.append(getDeviceInfo())
        Log.d(TAG, sb.toString())
    }

    fun getProcessName(cxt: Context, pid: Int): String? {
        val am = cxt.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses ?: return null
        for (procInfo in runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName
            }
        }
        return null
    }

}