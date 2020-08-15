package com.sogou.iot.arch.anr.interceptor

import android.util.Log
import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.anr.TrBlockMonitor
import com.sogou.iot.arch.db.BlockSource
import com.sogou.iot.arch.upload.BlockUploadStrategy


class UploadIntercepter : IBlockInterceptor {

    var sendDelay: Long = (TrBlockMonitor.monitorContext.getBlockTimeLong() * 1.2).toLong()
    override fun onBlock(info: BlockInfo) {
        dealWithBydereplication(info)
    }

    /**
     * 去重处理
     */
    fun dealWithBydereplication(blockInfo: BlockInfo) {
        var lastblock = BlockSource.getLastBlock()
        var theSameBlock: Boolean = false
        lastblock?.let {
            //当前block 与lastBlock的info相同，并且lastBlock 距现在不超过1分钟，则视为当前clock和lastBlock为相同的block,lastBlock作为去重项,需删除.
            if (it.info.isNotEmpty() && blockInfo.info.isNotEmpty() && it.info.equals(blockInfo.info) && (System.currentTimeMillis() - it.timeStamp < 60 * 1000)) {
                theSameBlock = true
            }
        }
        Log.d(TAG, "dealWithBydereplication theSameBlock:" + theSameBlock)
        if (!theSameBlock) { //判定为新的block
            doSend(blockInfo)
        } else { //判定为相同的block
            lastblock?.let {
                doCancelSendBlock(it)
            }
            doSend(blockInfo)
        }
    }

    fun doSend(blockInfo: BlockInfo) {
        var id = BlockSource.insertBlockInfo(blockInfo)
        BlockUploadStrategy.tryUploadAnr2ServerByDelay(sendDelay, blockInfo)
        Log.d(TAG, "doSend:${blockInfo.id},rowId:${id},blockId:${blockInfo.id}")
    }

    fun doCancelSendBlock(blockInfo: BlockInfo) {
        Log.d(TAG, "doCancelSendBlock:${blockInfo.id},msg.what:${blockInfo.id.toLong().toInt()}")
        BlockSource.deleteSingleBlockWithFile(blockInfo)
        BlockUploadStrategy.cancelLastSendByID(blockInfo.id.toLong())
    }

    companion object {
        var TAG: String = UploadIntercepter::class.java.simpleName
    }
}