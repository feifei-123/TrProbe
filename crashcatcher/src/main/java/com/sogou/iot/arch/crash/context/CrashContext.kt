package com.sogou.iot.arch.crash.context

import android.content.Context
import com.sogou.iot.arch.crash.CrashCollectType
import com.sogou.iot.arch.crash.CrashInfo
import com.sogou.tm.commonlib.log.Logu

abstract class CrashContext: ICrashContext {

    /**Context上下文信息，必须传递*/
    var mContext:Context? = null

    override fun setAppContext(context:Context?){
        mContext = context
    }

    override fun getAppContext(): Context?{
        return mContext
    }

//    /**
//     * crash日志保存路径
//     */
//    open fun getLogSavePath():String{
//        return "/sdcard/sogou/log/crash"
//    }

    /**
     * natviecrash捕获后,是否丢弃,不上报给系统
     */
    override fun interruptSystemNatvieCrash():Boolean{
        return false
    }

    /**
     * javacrash捕获后,是否丢弃，不上报给系统。
     */
    override fun interrupteSystemJavaCrash():Boolean{
        return false
    }

    /**
     * crash发生时,是否收集logcat信息
     */
    override fun doCollectLogcat():Boolean{
        return true
    }


    /**
     * crash发生后,延时若干秒收集logcat信息,等待系统输出crash堆栈到logcat。
     */
    override fun collectLogcatDelay():Long{
        return 3000;
    }

    /**
     * crash发生时,收集logcat日志后，是否将系统logcat缓存清空。保证一份logcat日志仅包含一个crash
     */
    override fun clearLogcatOnCrash():Boolean{
        return true
    }

    /**
     * 收集crash日志(crash堆栈日志和logcat日志)方式,本进程处理，还是移交其他进程处理
     * 1、OTHERPROCESS 其他进程处理:crash发生时,app已经处于一个不稳定状态,不能可靠的执行过多的任务,所以推荐选择OTHERPROCESS。此处默认是使用logService进程,同时预留了接口供开发者指定其他进程,见collectLogcatByOtherProcess()和saveLogWithPathByOtherProcess()
     * 2、REBOOT crash发生后,app重启时收集crash信息。适用于launcher类的奔溃后会自动重启的应用。
     */
    override fun getCrashCollectType(): CrashCollectType {
        return CrashCollectType.OTHERPROCESS
    }


    /**
     * 通过其他进程收集logcat信息,用户可以自己指定进程完成此任务。
     */
    override fun collectLogcatByOtherProcess(savepath: String, delaytime: Long, clearlogcat: Boolean) {
        Logu.collectLogcat(savepath, delaytime, clearlogcat)
    }

    /**
     * 通过其他进程保存logcat信息，用户可以自己指定进程完成此任务。
     */
    override fun saveLogWithPathByOtherProcess(log: String, path: String){
        Logu.saveLogWithPath(log,path)
    }

    /**
     * crash 记录过期时间,单位:天
     * 过期后，记录会从数据库删除,对应的日志文件也会删除。
     */
    override fun getCashOverDueDay():Int{
        return 7
    }

    /**
     * 回调Crash事件 给宿主应用
     */
    override fun onCrashEvent(crashInfo: CrashInfo){

    }


//    /**
//     * 将相关的文件压缩成zip文件,等待上传
//     * 上传成功后,需要回调uploadSuccess 告知sdk 上传成功的状态;否则该crash 继续定期的尝试回调zipAndUpload
//     */
//    open fun zipAndUpload(crashInfo: CrashInfo, uploadSuccess:((success:Boolean, info:String)->Unit)?){
//        //此处完成 文件压缩和block上报
//        Log.d("feifei","zipAndUpload:${crashInfo.fileList.toString()}")
//        uploadSuccess?.invoke(true,"success")
//    }


}