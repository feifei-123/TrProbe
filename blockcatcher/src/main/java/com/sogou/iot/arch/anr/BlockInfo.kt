package com.sogou.iot.arch.anr


import android.util.Log
import com.sogou.iot.arch.anr.FileWriter.LINESEPERATOR
import com.sogou.iot.arch.anr.catcher.BlockWatcherType
import com.sogou.iot.arch.db.AbstractInfo
import com.sogou.iot.arch.db.BlockWrapper
import com.sogou.iot.arch.db.ex.folderEndPath
import java.text.SimpleDateFormat
import java.util.*

data class BlockInfo(
    var id:String = "",
    var type: BlockType = BlockType.UIBLOCK,
    var watcherType: BlockWatcherType = BlockWatcherType.AnrFileObverser,
    var happentime:Long = 0,
    var blockTime:Long = 0,
    var info:String = "",
    var cpuInfo:String = "",
    var timeStamp:Long = 0
): AbstractInfo{

    @Transient
    val FILE_NAME_FORMATTER = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS", Locale.US)
    var fileList:List<String> = mutableListOf()
    var isReported = false //是否已经上报

    override fun toString(): String {
        return generateShowMsg()
    }


    /**
     * 获取标识Block的关键信息,用于block去重操作。
     */
    override fun getKeyInfo():String{
        var strBuffer = StringBuffer()
        strBuffer.append("blockType:${type}${LINESEPERATOR}")
        strBuffer.append("UI阻塞点:${getConcernBlockInfo()}${LINESEPERATOR}")
        return strBuffer.toString()
    }

    fun generateShowMsg():String{
        var stringBuilder = StringBuffer()
        stringBuilder.append("发生了UI卡顿${LINESEPERATOR}")
        stringBuilder.append("blockId:${id}${LINESEPERATOR}")
        stringBuilder.append("blockType:${type}${LINESEPERATOR}")
        stringBuilder.append("blockCatcherType:${watcherType}${LINESEPERATOR}")
        stringBuilder.append("UI阻塞点:${getConcernBlockInfo()}${LINESEPERATOR}")
        stringBuilder.append("cpu信息:${cpuInfo}${LINESEPERATOR}")
        stringBuilder.append("日志文件:${fileList.toString()}${LINESEPERATOR}")
        return stringBuilder.toString()
    }

    fun getConcernBlockInfo():String{
        var lines = info.split(LINESEPERATOR)
        if(lines == null || lines.size == 0) return ""
        var result = lines[0];
        for(line in lines){
            for (concen in TrBlockMonitor.monitorContext.getConcernPackageNames()){
                Log.d("feifei","line:${line},package:${concen}")
                if(line.contains(concen)){
                    result = line
                    break
                }
            }
        }
        return result
    }

    fun toBlockWrapper(): BlockWrapper {
        return BlockWrapper(
            id = id,
            type = type.value,
            watcherType = watcherType.value,
            happentime = happentime,
            blockTime = blockTime,
            info = info,
            cpuInfo = cpuInfo,

            timeStamp = timeStamp,
            fileList = fileList,
            isReported = isReported
        )
    }

    fun fromBlockWrapper(blockWrapper: BlockWrapper): BlockInfo {
        id = blockWrapper.id
        type = BlockType.toBlockType(blockWrapper.type)
        watcherType = BlockWatcherType.toBlockWatcherType(blockWrapper.watcherType)
        happentime = blockWrapper.happentime
        blockTime = blockWrapper.blockTime
        info = blockWrapper.info
        cpuInfo = blockWrapper.cpuInfo
        timeStamp = blockWrapper.timeStamp
        fileList = blockWrapper.fileList
        isReported = blockWrapper.isReported
        return this
    }



    fun convert2FilePath(fileName:String):String{
        return TrBlockMonitor.monitorContext.getLogSavePath().folderEndPath()+fileName
    }

    fun conver2FilePathList():MutableList<String>{
        var filepathlist = mutableListOf<String>()
        fileList.forEach{
            filepathlist.add(convert2FilePath(it))
        }
        return filepathlist
    }

    companion object{

    }

}


