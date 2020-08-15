package com.sogou.iot.arch.upload

import android.util.Log
import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.crash.CrashInfo
import com.sogou.iot.arch.db.AbstractInfo
import com.sogou.iot.arch.db.BlockSource
import com.sogou.iot.arch.db.CrashSource
import com.sogou.iot.arch.db.HandlerThreadFactory

object UploadStrategy {

    fun checkUpload(){
        HandlerThreadFactory.getTimerThreadHandler().post {
            var crash2Upload = CrashSource.getAllCrashesUpReported()
            var block2Upload = BlockSource.getAllBlocksUnReported()
            var crashAndBlocks = mutableListOf<AbstractInfo>()
            Log.d("checkUpload","crash2Upload:${crash2Upload.size},block2Upload:${block2Upload.size}")
            crashAndBlocks.addAll(crash2Upload)
            crashAndBlocks.addAll(block2Upload)

            for(index in 0..crashAndBlocks.size-1){
                HandlerThreadFactory.getTimerThreadHandler().postDelayed( {
                    var info  = crashAndBlocks.get(index)
                    if(info is BlockInfo){
                        BlockUploadStrategy.tryUploadAnr2Server(info)
                    }else if(info is CrashInfo){
                        CrashUploadStrategy.try2UploadCrash2Server(info)
                    }
                },4000L*index)
            }
        }
    }

}