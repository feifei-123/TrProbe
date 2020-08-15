package com.sogou.iot.arch.upload

import android.os.Handler
import android.os.Message
import android.util.Log
import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.anr.TrBlockMonitor

import com.sogou.iot.arch.db.*
import com.sogou.tm.commonlib.log.Logu


object BlockUploadStrategy {

    var TAG:String = BlockUploadStrategy::class.java.simpleName

    var handler:Handler = object : Handler(HandlerThreadFactory.getTimerThreadHandler().looper){
        override fun handleMessage(msg: Message?) {
            var blockInfo = msg?.obj as BlockInfo
            blockInfo?.let {
                tryUploadAnr2Server(blockInfo = blockInfo)
            }
        }
    }

    fun tryUploadAnr2ServerByDelay(delay:Long,blockInfo: BlockInfo){
        Logu.d(TAG,"tryUploadAnr2ServerByDelay:${blockInfo.id},msg.what:${blockInfo.id.toLong().toInt()},delayTime:${delay}")
        var msg =  handler.obtainMessage()
        msg.what = blockInfo.id.toLong().toInt()
        msg.obj = blockInfo
        handler.sendMessageDelayed(msg,delay)
    }

    fun cancelLastSendByID(blockId:Long){
        Logu.d(TAG,"cancelLastSendByID:${blockId},msgID:${blockId.toLong().toInt()}")
        handler.removeMessages(blockId.toLong().toInt())
    }

    fun tryUploadAnr2Server(blockInfo: BlockInfo){

        TrBlockMonitor.monitorContext.zipAndUpload(blockInfo,{ success, info ->
            Log.d(TAG,"tryUploadAnr2Server sucess:${blockInfo.id},success :${success}")
            if(success){
                blockInfo.isReported = true
                var rowId = BlockSource.updateBlockInfo(blockInfo)
                Logu.d(TAG,"updateBlockInfo reported:"+blockInfo.id+",rowId:${rowId}")

            }
        })
    }










}