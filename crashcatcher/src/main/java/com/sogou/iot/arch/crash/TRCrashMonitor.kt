package com.sogou.iot.arch.crash

import android.util.Log
import com.sogou.iot.arch.crash.catcher.JavaCrashCatcher
import com.sogou.iot.arch.crash.catcher.NatvieCrashCatcher
import com.sogou.iot.arch.crash.context.ICrashContext
import com.sogou.iot.arch.db.CrashSource
import com.sogou.iot.arch.db.HandlerThreadFactory
import com.sogou.iot.arch.db.LocalStore


object TRCrashMonitor: CrashListener {

    val TAG:String = TRCrashMonitor::class.java.simpleName

    override fun onCrashEvent(crashInfo: CrashInfo) {

        HandlerThreadFactory.getTimerThreadHandler().post({
            CrashCollector.doCollectExtraInfo(crashInfo)
            mCrashContext.onCrashEvent(crashInfo)
            Log.e("feifei","onCrashEvent:${crashInfo.id},crashInfo:${crashInfo.info}")
        })

    }

    var javaCrashCatcher: JavaCrashCatcher =
        JavaCrashCatcher.instance
    var nativeCrashCatcher: NatvieCrashCatcher =
        NatvieCrashCatcher.getInstance()
    lateinit var mCrashContext: ICrashContext



    fun install(crashContext: ICrashContext){
        mCrashContext = crashContext
        LocalStore.setAppContext(mCrashContext.getAppContext()!!)
        javaCrashCatcher.setCrashListener(this).install(crashContext)
        nativeCrashCatcher.setCrashListener(this).install(crashContext)

        checkOverDueCrashRecord()

        checkCrashByReboot()

    }

    fun checkCrashByReboot(){
        if(mCrashContext.getCrashCollectType() == CrashCollectType.REBOOT){
            HandlerThreadFactory.getTimerThreadHandler().post{
                var lastcash = CrashSource.getLastCrash()
                Log.d(TAG,"checkCrashByReboot：${lastcash?.id}")
                lastcash?.let {
                    CrashCollector.saveCrashLogcatInfoByReboot(crashInfo = lastcash)
                }
            }

        }
    }

    fun checkOverDueCrashRecord(){
        HandlerThreadFactory.getTimerThreadHandler().post({
            CrashSource.checkDeleteCrashOverDue(TRCrashMonitor.mCrashContext.getCashOverDueDay())
        })
    }
}