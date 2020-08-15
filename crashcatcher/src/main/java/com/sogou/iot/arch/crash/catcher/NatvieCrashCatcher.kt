package com.sogou.iot.arch.crash.catcher

import android.util.Log
import com.sogou.iot.arch.crash.*
import com.sogou.iot.arch.crash.CrashInfo
import com.sogou.iot.arch.crash.context.ICrashContext
import com.sogou.translate.breakpad.BreakPadCore
import com.sogou.iot.arch.db.ex.folderEndPath
import java.io.File

class NatvieCrashCatcher : AbstractCrashCatcher {

    private var mCrashListener: CrashListener? = null

    override fun install(crashContext: ICrashContext) {
        val externalReportPath = File(crashContext.getLogSavePath().folderEndPath())
        if (!externalReportPath.exists()) {
            externalReportPath.mkdirs()
        }

        Log.d("feifei", "initBreakPad")
        BreakPadCore.initBreakpad(externalReportPath.absolutePath, InfoHelperImpl() {
            var crashId = System.currentTimeMillis().toString()
            var crashInfo = CrashInfo(
                id = crashId,
                crashType = CrashType.NATIVECRASH,
                info = it,
                timeStamp = System.currentTimeMillis()
            )
            mCrashListener?.onCrashEvent(crashInfo)
        })
        BreakPadCore.setInterrupteSysNativeCrash(crashContext.interruptSystemNatvieCrash())
    }

    fun setCrashListener(listener: CrashListener): NatvieCrashCatcher {
        mCrashListener = listener
        return this;
    }

    companion object{
        private var mInstance: NatvieCrashCatcher? = null
        fun getInstance(): NatvieCrashCatcher = mInstance
            ?: NatvieCrashCatcher().also { mInstance = it }
    }
}