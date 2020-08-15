package com.sogou.iot.arch.db


import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "block")
data class BlockWrapper(
    @PrimaryKey
        var id:String,
    var type: Int,
    var watcherType: Int,
    var happentime:Long,
    var blockTime:Long,
    var info:String,
    var cpuInfo:String,
    var timeStamp:Long,
    var fileList:List<String>,
    var isReported:Boolean//是否已经上报
){

}


