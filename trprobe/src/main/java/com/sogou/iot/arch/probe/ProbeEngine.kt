package com.sogou.iot.arch.probe

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log
import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.anr.TrBlockMonitor
import com.sogou.iot.arch.anr.context.IBlockContext
import com.sogou.iot.arch.anr.context.SpecialBlockContext
import com.sogou.iot.arch.crash.CrashInfo
import com.sogou.iot.arch.crash.TRCrashMonitor
import com.sogou.iot.arch.crash.context.CrashContext
import com.sogou.iot.arch.crash.context.ICrashContext
import com.sogou.iot.arch.db.ex.folderEndPath
import com.sogou.iot.arch.receiver.TRReceiver
import com.sogou.tm.commonlib.log.LogContext
import com.sogou.tm.commonlib.log.Logu

object ProbeEngine {
    var TAG: String = ProbeEngine::class.java.simpleName
    private var mLogContext: LogContext? = null
    private var mCrashContext: ICrashContext? = null
    private var mBlockContext: IBlockContext? = null
    private var mContext: Context? = null

    private var receiver: TRReceiver = TRReceiver()

    fun setContext(context: Context): ProbeEngine {
        mContext = context
        return this
    }

    fun setCrashContext(crashContext: ICrashContext): ProbeEngine {
        mCrashContext = crashContext
        return this
    }

    fun setBlockContext(blockContext: IBlockContext): ProbeEngine {
        mBlockContext = blockContext
        return this
    }

    fun setLogContext(logContext: LogContext): ProbeEngine {
        mLogContext = logContext
        return this
    }


    fun setupLog(): ProbeEngine {
        checkLogReady()
        Log.d(TAG, "setupLog:${mLogContext?.getLogSavePath()?.folderEndPath()}")
        Logu.initLogEngine(mLogContext!!.getAppContext())
        Logu.setLogSaveFoloder(mLogContext?.getLogSavePath()?.folderEndPath())
        Logu.setShowLog(mLogContext!!.getShowLog())
        Logu.enableTooLargeAutoDevide(mLogContext!!.enableToolargeAutoDevice())
        Logu.setAutoDivideRatio(mLogContext!!.autoDiviceRatio())
        Logu.setBaseStackIndex(mLogContext!!.getBaseStackIndex())
        Logu.setLogPre(mLogContext?.getLogPre())
        return this
    }


    fun install() {

        checkReady()

        setupLog()

        TrBlockMonitor.install(mBlockContext!!).start()
        TRCrashMonitor.install(mCrashContext!!)
        registerReceiver(mContext!!)


    }

    fun checkReady() {
        checkLogReady()
        checkCrashReady()
        checkBlockReady()
    }

    fun checkLogReady() {
        if (mContext == null) {
            throw Exception("mContext must be set !")
        }
        if (mLogContext == null) {
            mLogContext = LogContext()
        }
        mLogContext?.setAppContext(mContext)
    }

    fun checkCrashReady() {
        if (mContext == null) {
            throw Exception("mContext must be set !")
        }

        if (mCrashContext == null) {
            mCrashContext = createDefaultCrashContext()
        }
        mCrashContext?.setAppContext(mContext)
    }

    fun checkBlockReady() {
        if (mContext == null) {
            throw Exception("mContext must be set !")
        }

        if (mBlockContext == null) {
            mBlockContext = createDefaultBlockContext()
        }
        mBlockContext?.setAppContext(mContext)
    }

    fun registerReceiver(context: Context) {
        var filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(receiver, filter)

    }

    fun unregisterReceiver(context: Context) {
        context.unregisterReceiver(receiver)
    }


    fun flushLog() {
        Logu.flush()
    }

    /**
     * 创建一个默认的BlockContext
     */
    fun createDefaultBlockContext(): IBlockContext {
        return object : SpecialBlockContext() {
            override fun getLogSavePath(): String {
                return "/sdcard/sogou/log/anr"
            }

            override fun getConcernProcesses4Anr(): Array<String> {
                var processNames = arrayOf("com.sogou.iot.b1pro.launcher", "com.sogou.translate.example")
                return processNames
            }

            override fun getConcernPackageNames(): Array<String> {
                var concerns: Array<String> = arrayOf("com.sogou.translate.example");
                return concerns;
            }

            /**
             * 用于 上报block信息和block文件,block信息上传成功后，需要回调uploadSuccess() 告知blockCacher已经上报成功.
             */
            override fun zipAndUpload(
                blockInfo: BlockInfo,
                uploadSuccess: ((success: Boolean, info: String) -> Unit)?
            ) {
                uploadSuccess?.invoke(true, "success")
            }

        }
    }

    /**
     * 创建一个默认的CrashContext
     */
    fun createDefaultCrashContext(): ICrashContext {
        return object : CrashContext() {
            //设置crash日志文件保存路径
            override fun getLogSavePath(): String {
                return "/sdcard/sogou/log/crash"
            }

            //crash日志文件压缩和上报的接口,日志成功上报到服务端后,需要调用uploadSuccess,通知crashCacher
            override fun zipAndUpload(
                crashInfo: CrashInfo,
                uploadSuccess: ((success: Boolean, info: String) -> Unit)?
            ) {
                uploadSuccess?.invoke(true, "success")
            }
        }
    }

    fun isShowLog(): Boolean {
        return Logu.isShowLog()
    }

    fun setShowLog(isShowLog: Boolean) {
        Logu.setShowLog(isShowLog)
    }
}