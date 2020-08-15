package com.sogou.iot.arch.upload

import android.util.Log
import com.sogou.iot.arch.crash.CrashInfo
import com.sogou.iot.arch.crash.TRCrashMonitor
import com.sogou.iot.arch.db.CrashSource
import com.sogou.iot.arch.db.LocalStore

object CrashUploadStrategy {
    val TAG = CrashUploadStrategy::class.java.simpleName
    fun try2UploadCrash2Server(crashInfo: CrashInfo){
        TRCrashMonitor.mCrashContext.zipAndUpload(crashInfo,{ success, info ->
            Log.d(TAG,"try2UploadCrash2Server sucess:${crashInfo.id},success :${success}")
            if(success){
                crashInfo.isReported = true
                var rowId = CrashSource.updateCrashInfo(crashInfo)

            }
        })
    }
}