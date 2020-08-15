package com.sogou.iot.arch.db

import android.content.Context

object LocalStore {

    lateinit var mContext:Context
    fun setAppContext(context:Context){
        mContext = context
    }

    val TAG = LocalStore::class.java.simpleName

    val blockDao:BlockDao
    get() = ProbeDatabase.getInstance(mContext).blockDao()

    val crashDao
    get() = ProbeDatabase.getInstance(mContext).crashDao()

    fun insertBlockWrapper(blockinfo: BlockWrapper):Long{
        return blockDao.insertBlockInfo(blockinfo)
    }

    fun deleteBlockWrapper(blockinfo: BlockWrapper):Int{
        return blockDao.deleteBlockInfo(blockinfo)
    }

    fun getAllBlockWrappers():List<BlockWrapper>{
        return blockDao.getAllBlocks()
    }

    fun getAllBlockWrappersUnReported():List<BlockWrapper>{
        return blockDao.getAllBlocksUnReported()
    }

    fun updateBlockWrapper(block: BlockWrapper):Int{
        return blockDao.updateBlockInfo(block)
    }


    fun insertCrashWrapper(crashInfo: CrashWrapper):Long{
        return crashDao.insertCrashInfo(crashInfo)
    }

    fun deleteCrashWrapper(crashInfo: CrashWrapper):Int{
        return crashDao.deleteCrashInfo(crashInfo)
    }

    fun getAllCrashWrappers():List<CrashWrapper>{
        return crashDao.getAllCrash()
    }

    fun getAllCrashWrappersUpReported():List<CrashWrapper>{
        return crashDao.getAllCrashsUnReported()
    }

    fun updateCrashWrapper(crash: CrashWrapper):Int{
        return crashDao.updateCrashInfo(crash)
    }


    fun getAllCrashWrappersByTime(day:Int):List<CrashWrapper>{
        var stamp = System.currentTimeMillis() - day*24*60*60*1000
        var casheWrappers = crashDao.getCrashOverDue(stamp)
        return casheWrappers
    }

    fun getAllBlockWrappersByTime(day:Int):List<BlockWrapper>{
        var stamp = System.currentTimeMillis() - day*24*60*60*1000
        var blockWrappers = blockDao.getBlockOverDue(stamp)
        return blockWrappers
    }



    /**
     * 获取最近一条crash的信息
     */
    fun getLastCrash():CrashWrapper?{
       return crashDao.getLastCrash()
    }

    /**
     * 获取最近一条block
     */
    fun getLastBlock():BlockWrapper?{
        return blockDao.getLastBlock()
    }


}