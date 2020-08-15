package com.sogou.iot.arch.crash


import com.sogou.iot.arch.db.AbstractInfo
import com.sogou.iot.arch.db.CrashWrapper
import com.sogou.iot.arch.db.LINESEPERATOR
import com.sogou.iot.arch.db.ex.folderEndPath
import java.io.File


data class CrashInfo(
    var id:String = "",
    var crashType: CrashType = CrashType.JAVACRASH,
    var info:String = "",
    var timeStamp:Long = 0
): AbstractInfo{

    var deviceInfo:String = ""
    var fileList:MutableList<String> = mutableListOf()
    var isReported:Boolean = false


    /**
     * 提取出标识此次cash的关键信息，用于crash的去重操作。
     */
    override fun getKeyInfo():String{
        var strBuffer = StringBuffer();
        if(crashType == CrashType.NATIVECRASH){ //natvie crash 提取singalinfo 行和 crash point行
            var infos = info.split(LINESEPERATOR);
            infos.forEach {
                if(it.startsWith("singalinfo")||it.startsWith("crash point")){
                    var regexpc = "pc:0x[0-9a-f]{16}".toRegex()
                    var regexpackage = "-[^/]+/".toRegex()
                    var nopc = regexpc.replace(it,"") //去掉类似pc:0x00000070ba22e9f8的信息，因为每次crash的pc值都不一样
                    var nopackageRandom =  regexpackage.replace(nopc," /")//去掉包名后面的随机字符 com.sogou.translate.example-FaKScOYmj4-QrX-tdn1dBw==
                    strBuffer.append(nopackageRandom+ LINESEPERATOR)
                }
            }
        }else if(crashType == CrashType.JAVACRASH){
            var infos = info.split(LINESEPERATOR);
            infos.forEach{
                if(!it.startsWith("crashTime")){
                    strBuffer.append(it+ LINESEPERATOR)
                }
            }
        }
        return strBuffer.toString()
    }

    fun generateCrashMsg():String{
        var strBuffer = StringBuffer()
        strBuffer.append("crash happened"+ LINESEPERATOR)
        strBuffer.append("crash id: "+id+ LINESEPERATOR)
        strBuffer.append("crash type: "+crashType+ LINESEPERATOR)
        strBuffer.append("crash info: "+info+ LINESEPERATOR)
        strBuffer.append("crash filelist:"+fileList+LINESEPERATOR)

        strBuffer.append("==========================="+ LINESEPERATOR)
        strBuffer.append(deviceInfo+ LINESEPERATOR)
        return strBuffer.toString()
    }



    fun toCrashWrapper(): CrashWrapper {
        return CrashWrapper(
            id = id,
            crashType = crashType.value,
            info = info,
            timeStamp = timeStamp,
            deviceInfo = deviceInfo,
            fileList = fileList,
            isReported = isReported
        )
    }

    fun fromCrashWrapper(crashWrapper: CrashWrapper): CrashInfo {
        id = crashWrapper.id
        crashType = CrashType.toCrashType(crashWrapper.crashType)
        info = crashWrapper.info
        timeStamp = crashWrapper.timeStamp
        deviceInfo = crashWrapper.deviceInfo
        fileList = crashWrapper.fileList
        isReported = crashWrapper.isReported
        return this
    }


    fun convert2FilePath(fileName:String):String{
        return TRCrashMonitor.mCrashContext.getLogSavePath().folderEndPath()+ fileName
    }

    fun conver2FilePathList():MutableList<String>{
        var filepathlist = mutableListOf<String>()
        fileList.forEach{
            filepathlist.add(convert2FilePath(it))
        }
        return filepathlist
    }

    override fun toString(): String {

        return generateCrashMsg()
    }

}