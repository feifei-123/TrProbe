package com.sogou.iot.arch.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crash")
data class CrashWrapper(
    @PrimaryKey
    var id:String,
    var crashType: Int,
    var info:String,
    var timeStamp:Long,
    var deviceInfo:String,
    var fileList:MutableList<String>,
    var isReported:Boolean
)