package com.sogou.iot.arch.db

import android.util.Log
import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.anr.TrBlockMonitor
import com.sogou.iot.arch.db.ex.folderEndPath
import java.io.File

object BlockSource {

    val TAG = BlockSource::class.java.simpleName

    fun insertBlockInfo(blockinfo: BlockInfo):Long{
        return LocalStore.insertBlockWrapper(blockinfo.toBlockWrapper())
    }

    fun deleteBlockInfo(blockinfo: BlockInfo):Int{
        return LocalStore.deleteBlockWrapper(blockinfo.toBlockWrapper())
    }

    fun getAllBlockInfos():List<BlockInfo>{
        var blockWrappers =  LocalStore.getAllBlockWrappers()
        var result:MutableList<BlockInfo> = mutableListOf()
        blockWrappers?.forEach {
            result.add(BlockInfo().fromBlockWrapper(it))
        }
        return result
    }

    fun getAllBlocksUnReported():List<BlockInfo>{
        var blockWrappers =  LocalStore.getAllBlockWrappersUnReported()
        var result:MutableList<BlockInfo> = mutableListOf()
        blockWrappers.forEach {
            result.add(BlockInfo().fromBlockWrapper(it))
        }
        return result
    }

    fun updateBlockInfo(block: BlockInfo):Int{
        return LocalStore.updateBlockWrapper(block.toBlockWrapper())
    }

    fun getLastBlock():BlockInfo?{
        var lastBlock:BlockInfo? = null
         LocalStore.getLastBlock()?.let {
             lastBlock = BlockInfo().fromBlockWrapper(it)
         }
        return lastBlock
    }


    fun getAllBlockByTime(day:Int):List<BlockInfo>{
        var blockWrappers = LocalStore.getAllBlockWrappersByTime(day)
        var result:MutableList<BlockInfo> = mutableListOf()
        blockWrappers.forEach {
            result.add(BlockInfo().fromBlockWrapper(it))
        }
        return result
    }

    /**
     * 检查 过期的Block记录
     */
    fun checkDeleteBlockOverDue(day: Int){
        var blocks = getAllBlockByTime(day)
        blocks.forEach {
            deleteSingleBlockWithFile(it)
        }
    }


    fun deleteSingleBlockWithFile(blockinfo: BlockInfo){
        Log.d(TAG,"deleteSingleBlockWithFile:${blockinfo.id}:filelist.size:"+blockinfo.fileList.size)
        deleteBlockInfo(blockinfo)
        blockinfo.fileList.forEach {
            var filePath = TrBlockMonitor.monitorContext.getLogSavePath().folderEndPath()+it
            var file = File(filePath)
            if(file.exists()){
                file.delete()
            }
        }
    }




}