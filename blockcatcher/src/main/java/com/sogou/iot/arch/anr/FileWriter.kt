package com.sogou.iot.arch.anr

import android.os.Environment
import android.util.Log
import com.sogou.iot.arch.anr.catcher.FileObserverCatcher
import com.sogou.iot.arch.db.ex.folderEndPath
import com.sogou.iot.arch.db.utils.FileUtils
import com.sogou.iot.arch.db.utils.ShellUtils
import com.sogou.iot.arch.db.utils.TimeUtils

import java.io.File

object FileWriter {

    //anr文件后缀
    val ANR_FILE_SUFFIX = "_traces.txt"
    var LOGCAT_SUFFIX = "_logcat.txt"
    var ALLTHREADSTATE_SUFFIX = "_allthreadstate.txt"
    var MAIN_HISTORY_SUFFIX = "_mainhistory.txt"
    var BLOCK_INFO_SUFFIX = "_blockinfo.txt"
    var TMPZIP_SUFFIX = "_tmp.zip"
    //anr文件前缀
    val BLOCK_PREFIX = "block_"
    //单行分隔标识符
    val LINESEPERATOR = "\t\n"

    //更具blockId 获得系统anr trace文件名
    fun getAnrTraceFileNameById(blockId:String):String{
        return BLOCK_PREFIX +blockId+""+ ANR_FILE_SUFFIX
    }

    fun getLogcatFileNamebyId(blockId: String):String{
        return BLOCK_PREFIX +blockId+""+ LOGCAT_SUFFIX
    }


    fun getAllThreadStateFileNameById(blockId: String):String{
        return BLOCK_PREFIX +blockId+""+ ALLTHREADSTATE_SUFFIX
    }

    fun getMainThreadHistoryFileNameById(blockId: String):String{
        return BLOCK_PREFIX +blockId+""+ MAIN_HISTORY_SUFFIX
    }

    fun getBlockInfoFileName(blockId: String):String{
        return BLOCK_PREFIX +blockId+""+ BLOCK_INFO_SUFFIX
    }

    fun getAnrTraceFile(blockId: String):File{
        return File(
            getPathFolder(),
            getAnrTraceFileNameById(blockId)
        )
    }

    fun getTmpZipFileName(blockId: String):String{
        return "block"+ TMPZIP_SUFFIX
    }

    fun getPathFolder(): File {
        val state = Environment.getExternalStorageState()
        var path = TrBlockMonitor.monitorContext.getLogSavePath().folderEndPath()
         val theFile = File(path)
        if(!theFile.exists()){
            theFile.mkdirs()
        }
        return theFile
    }

    fun restoreAnrFile(anrFilePath:String, targetFileName:String){

        val targetFile = File(getPathFolder(),targetFileName)
        if(targetFile.exists()) targetFile.delete()
        var anrFile = File(anrFilePath)

        if(anrFile.exists()){
            Log.d("feifei","restoreAnrFile ${anrFilePath},targetFileName:,${targetFile.absolutePath}")
            FileUtils.copyFile(anrFile,targetFile)
        }else{
            Log.d("feifei","restoreAnrFile ${anrFile} was not exist")
        }
    }

    /**
     * 读取/data/anr文件中发生anr的进程名
     */
    fun readAnrProcessName(filePath:String):String {
        var anrFile = File(filePath)
        var procesName = ""
        Log.d("feifei","readAnrProcessName:${filePath}")
        anrFile.useLines { lines ->
            Log.d("feifei", "lines---:" + lines)
            for (line in lines) {
                Log.d("feifei", "line---:" + line)
                if (line.startsWith("Cmd line:", true)) {
                    var content = line.split(":")
                    Log.d(FileObserverCatcher.TAG, "anrBelong2Package - content:${content}")
                    if (content.size > 1) {
                        procesName = content[1]
                    }
                    break
                }
            }
        }
        return procesName
    }

    /**
     * 从/data/anr 系统traces.txt文件中 读取主线程堆栈
     */
    fun readAnrStackFromTrace(filePath: String):String{
        var anrFile = File(filePath)
        Log.d("feifei","readAnrStackFromTrace:${filePath}")
        var stackBuffer:StringBuffer = StringBuffer()

        var startFetch = false
        anrFile.useLines { lines ->
            for (line in lines) {

                if (line.startsWith("\"main\"", true)) {
                    startFetch = true //开始提取主线程堆栈
                }

                if(startFetch){
                    Log.d("feifei", "read line :" + line+",startFetch:${startFetch}")
                    stackBuffer.append(line+ LINESEPERATOR)
                    if(line.equals("")){
                        startFetch = false
                        Log.d("feifei", "read line :" + line+",startFetch:${startFetch}")
                        break
                    }
                }
            }
        }
        return stackBuffer.toString()
    }

    /**
     * 截取logcat日志到 anr目录
     */
    fun cutLogCat2Folder(blockId:String):String{
        var fileName = getLogcatFileNamebyId(blockId)
        var targetpath = File(getPathFolder(),fileName).absoluteFile
        val cmd = "logcat -d > $targetpath"
        Log.d("cutLogCat2Folder","collectLogcatInfo :$cmd")
        ShellUtils.execCommand(cmd, false)
        val cmd_clear = "logcat -c"
        ShellUtils.execCommand(cmd_clear, false)
        Log.d("","clear logcat  :$cmd_clear")
        return fileName
    }

    /**
     * 收集当前所有线程的堆栈信息 写到指定文件
     */
    fun collectAllThreadState2File(blockId: String):String{
        var fileName = getAllThreadStateFileNameById(blockId)
        var allthreadInfoFile = File(getPathFolder(),fileName)
        checkFileExist(allthreadInfoFile)
        allthreadInfoFile.appendText("发生ANR时所有线程的堆栈信息如下:\n")
        allthreadInfoFile.appendText("线程总个数:${Thread.getAllStackTraces().keys.size}\n")
        for(thread in Thread.getAllStackTraces().keys){
            var  stack = TraceCollector.generateStackByThread(thread)
            allthreadInfoFile.appendText(stack+ LINESEPERATOR)
        }
        return fileName
    }


    /**
     * 收集主进程的调用堆栈历史 到指定文件
     */
    fun collectMainThreadHistory2File(blockId: String,traceBeans:List<TraceCollector.TraceBean>):String{
        var fileName = getMainThreadHistoryFileNameById(blockId)
        val mainThreadHisFile = File(getPathFolder(),fileName)
        checkFileExist(mainThreadHisFile)
        var historySize = traceBeans.size-1
        mainThreadHisFile.appendText("打印主线程最近${historySize+1}次的进程状态\n")
        for (index in historySize downTo 0) {
            val stringBuffer = StringBuffer()
            val traceBean = traceBeans[index] as TraceCollector.TraceBean
            stringBuffer.append(( historySize-index).toString() + "⭐" + TimeUtils.getWholeTimeString(traceBean.stamp) + ",Stack:")
            stringBuffer.append(traceBean.msg)
            stringBuffer.append("\n")
            mainThreadHisFile.appendText(stringBuffer.toString())
        }
        return fileName
    }

    fun collectBlockInfo2File(blockInfo: BlockInfo):String{
        var fileName = getBlockInfoFileName(blockInfo.id)
        var file = File(getPathFolder(),fileName)
        checkFileExist(file)
        file.appendText("=======================")
        file.appendText(LINESEPERATOR)
        file.appendText(LINESEPERATOR)
        file.appendText("blockId:"+blockInfo.id+ LINESEPERATOR)
        file.appendText(LINESEPERATOR)
        file.appendText("发生时间:"+blockInfo.FILE_NAME_FORMATTER.format(blockInfo.happentime)+ LINESEPERATOR)
        file.appendText("阻塞时长:"+blockInfo.blockTime+ LINESEPERATOR)
        file.appendText("阻塞类型:"+blockInfo.type+ LINESEPERATOR)
        file.appendText("阻塞捕捉器类型:"+blockInfo.watcherType+ LINESEPERATOR)

        file.appendText("cpu 状态:"+ LINESEPERATOR)
        file.appendText(blockInfo.cpuInfo)
        file.appendText(LINESEPERATOR)
        file.appendText("主线程堆栈:"+ LINESEPERATOR)
        file.appendText(blockInfo.info+ LINESEPERATOR)
        file.appendText(LINESEPERATOR)
        return fileName
    }

    fun checkFileExist(file:File){
        if(!file.parentFile.exists())file.parentFile.mkdirs()
        if(file.exists()) file.delete()
        file.createNewFile()
    }



}