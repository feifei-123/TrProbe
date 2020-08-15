package com.sogou.iot.arch.db

import android.util.Log
import com.sogou.iot.arch.crash.CrashInfo
import com.sogou.iot.arch.crash.TRCrashMonitor
import com.sogou.iot.arch.db.ex.folderEndPath
import java.io.File

object CrashSource {

    val TAG = CrashSource::class.java.simpleName

    fun insertCrashInfo(crashInfo: CrashInfo):Long{
        return LocalStore.insertCrashWrapper(crashInfo.toCrashWrapper())
    }

    fun deleteCrashInfo(crashInfo: CrashInfo):Int{
        return LocalStore.deleteCrashWrapper(crashInfo.toCrashWrapper())
    }

    fun getAllCrashInfos():List<CrashInfo>{
        var crashWrappers =  LocalStore.getAllCrashWrappers()
        var result:MutableList<CrashInfo> = mutableListOf()
        crashWrappers.forEach {
            result.add(CrashInfo().fromCrashWrapper(it))
        }
        return result
    }

    fun getAllCrashesUpReported():List<CrashInfo>{
        var crashWrappers =  LocalStore.getAllCrashWrappersUpReported()
        var result:MutableList<CrashInfo> = mutableListOf()
        crashWrappers.forEach {
            result.add(CrashInfo().fromCrashWrapper(it))
        }
        return result
    }

    fun updateCrashInfo(crash: CrashInfo):Int{
        return LocalStore.updateCrashWrapper(crash.toCrashWrapper())
    }

    fun getLastCrash(): CrashInfo?{
        var crashWraper = LocalStore.getLastCrash()
        if(crashWraper != null){
            return CrashInfo().fromCrashWrapper(crashWraper)
        }
        return null
    }

//    fun getAllCrashOrAnrToUpload():List<AbstractInfo>{
//        var cashesWrappers = LocalStore.getAllCrashWrappersUpReported()
//        var blockWrappers = LocalStore.getAllBlockWrappersUnReported()
//
//        var result = mutableListOf<AbstractInfo>()
//        cashesWrappers.forEach {
//            result.add(CrashInfo().fromCrashWrapper(it))
//        }
//        blockWrappers.forEach {
//            result.add(BlockInfo().fromBlockWrapper(it))
//        }
//        return result
//    }

    fun getAllCrashByTime(day:Int):List<CrashInfo>{
        var crashWrappers = LocalStore.getAllCrashWrappersByTime(day)
        var result:MutableList<CrashInfo> = mutableListOf()
        crashWrappers.forEach {
            result.add(CrashInfo().fromCrashWrapper(it))
        }
        return result
    }

    /**
     * 检查 过期的crash记录
     */
    fun checkDeleteCrashOverDue(day:Int){
        var crashes = getAllCrashByTime(day)
        Log.d(TAG,"checkDeleteOverDue crashes :"+crashes.size)
        crashes.forEach {
            deleteSingleCrashWithFile(it)
        }
    }

    fun deleteSingleCrashWithFile(crash: CrashInfo){
        Log.d(TAG,"deleteSingleCrashWithFile:${crash.id},filelist.size:"+crash.fileList.size)

        deleteCrashInfo(crash)
        crash.fileList.forEach {
            var filePath = TRCrashMonitor.mCrashContext.getLogSavePath().folderEndPath()+it
            var file = File(filePath)
            if(file.exists()) file.delete()
        }
    }
}