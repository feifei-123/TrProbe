package com.sogou.iot.arch.anr.catcher

import android.os.FileObserver
import android.os.Handler
import android.os.Message
import android.util.Log
import com.sogou.iot.arch.anr.FileWriter
import com.sogou.iot.arch.anr.*
import com.sogou.iot.arch.anr.catcher.FileEventType.Companion.convertFromInt
import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.db.HandlerThreadFactory

import java.io.File


enum class FileEventType(val value:Int,val info:String){
    ACCESS(1,"从文件中读取数据"), //从文件中读取数据
    MODIFY(2,"从文件中编辑数据"), //从文件中编辑数据
    ATTRIB(4,"文件元数据（权限，拥有者，时间戳）被明确改变"), //文件元数据（权限，拥有者，时间戳）被明确改变
    CLOSE_WRITE(8,"有人打开文件或者目录进行书写，并且关闭它"),//有人打开文件或者目录进行书写，并且关闭它
    CLOSE_NOWEITTE(16,"有人打开文件或者目录没有编辑，并且关闭它"),//有人打开文件或者目录没有编辑，并且关闭它
    OPEN(32,"一个文件或者目录被打开"),//一个文件或者目录被打开
    MOVE_FROM(64,"一个文件或者子目录从被监控目录被移出"),//一个文件或者子目录从被监控目录被移出
    MOVE_TO(128,"一个文件或者子目录被移入到被监控的目录"),//一个文件或者子目录被移入到被监控的目录
    CREATE(256,"一个文件或者子目录在被监控的目录下被创建"),//一个文件或者子目录在被监控的目录下被创建
    DELETE(512,"文件从监控目录被删除"),//文件从监控目录被删除
    DELETE_SELF(1024,"监控的文件或者目录被删除，监控停止"),//监控的文件或者目录被删除，监控停止
    MOVE_SELF(2048,"监控的文件或者目录被移动，监控继续"),//监控的文件或者目录被移动，监控继续
    UNKOWN(-1,"未知");


    companion object{
        public fun convertFromInt(event:Int): FileEventType {
            return when(event){
                ACCESS.value -> ACCESS
                MODIFY.value -> MODIFY
                ATTRIB.value-> ATTRIB
                CLOSE_WRITE.value-> CLOSE_WRITE
                CLOSE_NOWEITTE.value-> CLOSE_NOWEITTE
                OPEN.value-> OPEN
                MOVE_FROM.value-> MOVE_FROM
                MOVE_TO.value-> MOVE_TO
                CREATE.value-> CREATE
                DELETE.value-> DELETE
                DELETE_SELF.value-> DELETE_SELF
                MOVE_SELF.value-> MOVE_SELF

                else -> UNKOWN
            }
        }
    }
}

class FileObserverCatcher(watchPath: String) :FileObserver(watchPath), AbstractBlockCatcher {

    companion object{
        val TAG:String = FileObserverCatcher::class.java.simpleName
        const val MSG_REPORT_ANR = 1
        const val DELAY = 1000*5L
    }


    var mHandler = object : Handler(HandlerThreadFactory.getTimerThreadHandler().looper){
        override fun handleMessage(msg: Message?) {
            if(msg?.what == MSG_REPORT_ANR){
                var anrFile = msg?.obj
                anrFile?.let {
                    doReportAnrHappened(it as String)
                }
            }
        }
    }
    override fun doStartWatch() {
        startWatching()
    }

    override fun doStopWatch() {

    }

    var anrPathFolder:String
    var watcherPrcoceseNames:MutableList<String> = mutableListOf()
    private var blocklistner: BlockListener? = null
    //private var lastAnrHappenedTime:Long = 0L
    var dealingAnrPaths:MutableList<String> = mutableListOf();

    init {
        anrPathFolder  = watchPath
    }

    fun addWatcherProcessName(processName:String?): FileObserverCatcher {
        processName?.let {
            if(!watcherPrcoceseNames.contains(processName)){
                watcherPrcoceseNames.add(processName)
            }
        }
        return this
    }

    fun addWatcherProcessNames(processNames:Array<String>): FileObserverCatcher {
        for( processName in processNames){
            Log.d(TAG,"addWatcherProcessNames:${processName}")
            addWatcherProcessName(processName.trim())
        }
        return this
    }

    fun setBlockListner(listener: BlockListener): FileObserverCatcher {
        blocklistner = listener
        return this;
    }


    override fun onEvent(event: Int, path: String?) {
       var eventType =  convertFromInt(event)
        var isAnrHappened = eventType == FileEventType.CLOSE_WRITE
        Log.d(TAG,"onEvent isAnrHappened:${isAnrHappened},eventType:${eventType},path:${path}")
        path?.let {
            if(isAnrHappened && !dealingWithTheAnr(it) &&anrBelong2Package(it)){
                putDealingWithTheAnr(it)
                reportAnrHappened(getAnrWholePath(it))
            }
        }
    }

    fun reportAnrHappened(anrFile:String){
        var msg = Message.obtain()
        msg.what = MSG_REPORT_ANR
        msg.obj = anrFile
        mHandler.removeMessages(MSG_REPORT_ANR)
        mHandler.sendMessageDelayed(msg,DELAY)
        Log.d(TAG,"reportAnrHappened send msg_report_anr delay ${DELAY}")
    }

    fun doReportAnrHappened(anrFile:String){
        var blockId = System.currentTimeMillis().toString()
        Log.d(TAG,"reportAnrHappened:${blockId},${anrFile}")

        //保存anr文件
        FileWriter.restoreAnrFile(anrFile, FileWriter.getAnrTraceFileNameById(blockId))
        var keyStack = FileWriter.readAnrStackFromTrace(anrFile)

        var cpuBean = TraceCollector.getInstance().sampleCpuInfo()
        var cpuInfo:String = if(cpuBean != null) cpuBean.msg else ""
        //anr上报
        var blockInfo = BlockInfo(
            blockId,
            BlockType.ANR,
            BlockWatcherType.AnrFileObverser,
            System.currentTimeMillis(),
            0L,
            keyStack,
            cpuInfo,
            System.currentTimeMillis()
        )

        blocklistner?.onBlock(blockInfo)
        //恢复anr状态
        resetDealingState()
    }


    fun dealingWithTheAnr(path: String):Boolean{
        var dealing = dealingAnrPaths.contains(path.trim())
        Log.d(TAG,"dealingWithTheAnr:"+dealing+",dealingAnrPaths:"+dealingAnrPaths.toString())
        return dealing
    }

    fun putDealingWithTheAnr(path: String){
        if(!dealingAnrPaths.contains(path)){
            dealingAnrPaths.add(path)
        }
        Log.d(TAG,"putDealingWithTheAnr:"+path+",dealingAnrPaths:"+dealingAnrPaths.toString())
    }

    fun resetDealingState(){
        Log.d(TAG,"resetDealingState,dealingAnrPaths:"+dealingAnrPaths.toString())
        dealingAnrPaths.clear()
    }



    fun getAnrWholePath(path: String):String{
        var fullpath = if(anrPathFolder.endsWith(File.separator)) anrPathFolder+path else anrPathFolder+File.separator+path
        return fullpath
    }

    fun anrBelong2Package(pathName: String?):Boolean{
        var filePath =getAnrWholePath(pathName!!)
        var anrProcess = FileWriter.readAnrProcessName(filePath)

        var result = false
        if(watcherPrcoceseNames.contains(anrProcess.trim())){
            result = true;
        }

        Log.d(TAG,"anrProcess:"+anrProcess+",result:"+result+",watcherPrcoceseNames:${watcherPrcoceseNames.toString()}")
        return result;
    }
}

