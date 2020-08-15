package com.sogou.iot.arch.crash.catcher

import android.content.Context
import android.util.Log
import com.sogou.iot.arch.crash.*
import com.sogou.iot.arch.crash.context.ICrashContext
import com.sogou.iot.arch.db.LINESEPERATOR


import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date

/**
 */

class JavaCrashCatcher
/**
 * 保证只有一个 JavaCrashCatcher 实例
 */
private constructor() : AbstractCrashCatcher, Thread.UncaughtExceptionHandler {

    // 程序的 Context 对象
    private var mContext: Context? = null

    // 系统默认的 UncaughtException 处理类
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null

    private var mCrashListener: CrashListener? = null

    //是否阻断 系统的crashHandler
    var interruptDefaultCrashHandler = false


    override fun install(crashContext: ICrashContext) {
        mContext = crashContext.getAppContext()
        interruptDefaultCrashHandler = crashContext.interrupteSystemJavaCrash()

        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        // 设置该 JavaCrashCatcher 为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * 当 UncaughtException 发生时会转入该函数来处理
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理

            sleepByTime(killSelfDelay.toLong())

            if (!interruptDefaultCrashHandler) {
                Log.d(TAG, "uncaughtException send to DefaultHandler")
                mDefaultHandler!!.uncaughtException(thread, ex)
            } else {
                Log.d(TAG, "uncaughtException kill self")
                try {
                    Thread.sleep(killSelfDelay.toLong())
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } finally {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    System.exit(1)
                }
            }

        }
    }

    fun sleepByTime(delay:Long){
        try {
            Thread.sleep(delay)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {

        }
    }

    /**
     * 自定义错误处理，收集错误信息，发送错误报告等操作均在此完成
     *
     * @param ex
     * @return true：如果处理了该异常信息；否则返回 false
     */
    private fun handleException(ex: Throwable?): Boolean {
        var ex = ex

        if (ex == null) {
            ex = Exception("异常信息为空")
        }

        val crashId = System.currentTimeMillis().toString() + ""

        val crashInfo = CrashInfo(
            crashId,
            CrashType.JAVACRASH,
            getCrashInfo(ex),
            System.currentTimeMillis()
        )
        Log.d("feifei","onJavaCrash happened")
        try {
            if (mCrashListener != null) {
                mCrashListener!!.onCrashEvent(crashInfo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }


    /**
     * 获得异常日志信息
     *
     * @param ex
     * @return 返回设备信息和异常信息, 便于将文件传送到服务器
     */
    private fun getCrashInfo(ex: Throwable): String {
        val sb = StringBuffer()
        sb.append("crashTime:"+S_D_FORMAT.format(Date()) + LINESEPERATOR)
                sb.append("packageName:"+mContext?.getPackageName()+",");
                sb.append("processName:"+CrashCollector.getProcessName(mContext!!,android.os.Process.myPid())+LINESEPERATOR);
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause: Throwable? = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()

        val result = writer.toString()
        sb.append(result)


        return sb.toString()
    }


    fun setCrashListener(listener: CrashListener): JavaCrashCatcher {
        mCrashListener = listener
        return this
    }

    companion object {

        val TAG = "JavaCrashCatcher"
        /**
         * 获取 JavaCrashCatcher 实例 ,单例模式
         */
        val instance = JavaCrashCatcher()


        private val S_D_FORMAT = SimpleDateFormat("MM-dd HH:mm:ss.SSS ")

        //检测到java奔溃后,自杀延时时间
        var killSelfDelay = 1000
    }


}
