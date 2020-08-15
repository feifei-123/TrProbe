package com.sogou.iot.arch.anr.interceptor

import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.anr.TrBlockMonitor

class ReportInterceptor:IBlockInterceptor {
    override fun onBlock(info: BlockInfo) {
        repportBydereplication(info)
    }

    var mLastBlock:BlockInfo? = null
    /**
     * 将Block事件 回调给上层App
     */
    fun repportBydereplication(blockInfo: BlockInfo) {

        var theSameBlock = if(
                mLastBlock !=null && mLastBlock!!.info.isNotEmpty() && blockInfo.info.isNotEmpty() &&
                mLastBlock!!.info.equals(blockInfo.info) &&
                (System.currentTimeMillis() - mLastBlock!!.timeStamp < 60 * 1000)
            ) true else false

        if (!theSameBlock) { //新的block事件,回调给上层
           TrBlockMonitor.monitorContext.onBlockEvent(blockInfo)
            mLastBlock = blockInfo
        }
    }

}