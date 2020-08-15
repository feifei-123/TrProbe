package com.sogou.iot.arch.anr

import android.os.Handler
import android.os.Process
import android.util.Log
import com.sogou.iot.arch.anr.catcher.BlockWatcherType
import com.sogou.iot.arch.anr.catcher.BlockListener
import com.sogou.iot.arch.anr.catcher.FileObserverCatcher
import com.sogou.iot.arch.anr.catcher.WatchDogCatcher
import com.sogou.iot.arch.anr.context.IBlockContext
import com.sogou.iot.arch.anr.interceptor.IBlockInterceptor
import com.sogou.iot.arch.anr.interceptor.DisplayIntercepor
import com.sogou.iot.arch.anr.interceptor.ReportInterceptor
import com.sogou.iot.arch.anr.interceptor.UploadIntercepter
import com.sogou.iot.arch.db.HandlerThreadFactory
import com.sogou.iot.arch.db.BlockSource
import com.sogou.iot.arch.db.LocalStore
import com.sogou.iot.arch.db.ex.getProcessName


object TrBlockMonitor {

    const val ANR_TAG = "_ANR_"
    val TAG = TrBlockMonitor::class.java.simpleName

    lateinit var monitorContext: IBlockContext
    //watchdog 方式捕捉anr
    lateinit var anrWatchDog: WatchDogCatcher
    lateinit var fileObserver: FileObserverCatcher
    lateinit var traceCollecter: TraceCollector

    var mInterceptor:MutableList<IBlockInterceptor> = mutableListOf()

    var isWorking:Boolean = false
    var anrWatchType: BlockWatcherType = BlockWatcherType.AnrFileObverser

    lateinit var mSubHandler: Handler

    var anrHappenedAction:((anrWatchType: BlockWatcherType, anrId:String)->Unit)?=null

    var blockListner =object : BlockListener {
        override fun onBlock(blockInfo: BlockInfo) {

            if(abandonIgnoredUIBlock(blockInfo)){
                return
            }

            traceCollecter.collectAnrInfo(blockInfo)
            anrHapped(blockInfo)
        }
    }


    fun install(moniContext: IBlockContext):TrBlockMonitor{

        monitorContext = moniContext

        LocalStore.setAppContext(monitorContext.getAppContext()!!)

        setAnrWatchType(monitorContext.getMonitorType())

        anrWatchDog = WatchDogCatcher()
            .setIgnoreDebugger(monitorContext.getIgnoreDebugger())
                .setAnrTimeLong(monitorContext.getBlockTimeLong())
            .setBlockListner(blockListner)

        fileObserver = FileObserverCatcher(monitorContext.getObserverPath())
            .addWatcherProcessName(getProcessName(moniContext.getAppContext()!!,Process.myPid()))
            .addWatcherProcessNames(moniContext.getConcernProcesses4Anr())
            .setBlockListner(blockListner)

        traceCollecter = TraceCollector.getInstance().setTraceInterval(monitorContext.getTraceSampleIntervel())
            .setTraceMaxSize(monitorContext.getTraceMaxSize())
            .setEnableCollectSystemTrace(monitorContext.collectSystemTrace())
            .setEnableAllThreadState(monitorContext.collectAllThreadState())
            .setEnableMainThreadHistory(monitorContext.collectMainThreadHistory())
            .setEnableCollectLogcat(monitorContext.collectLogcatInfo())
            .setDoCollectCpuInfo(moniContext.doCollectCpuInfo())

        mSubHandler = HandlerThreadFactory.getTimerThreadHandler()


        mInterceptor.add(UploadIntercepter())
        mInterceptor.add(ReportInterceptor())
        if(monitorContext.enableDisplayToast()){
            mInterceptor.add(DisplayIntercepor())
        }


        return this

    }

    fun setAnrHappedAction(action:((anrWatchType: BlockWatcherType, anrId:String)->Unit)):TrBlockMonitor{
        anrHappenedAction = action
        return this;
    }

    fun anrHapped(blockInfo: BlockInfo){
        Log.e(TAG,"anrHapped onBlockEvent:"+blockInfo.toString())
        for(blockinterceptor in mInterceptor){
            blockinterceptor.onBlock(blockInfo)
        }
    }

    /**
     * 针对一些 不关心的UIBlock 进行滤除操作。
     * 比如,crash 时导致的block
     * true - 代表blockInfo 不关心,丢弃。
     */
    fun abandonIgnoredUIBlock(blockInfo: BlockInfo):Boolean{
        var shouldabandon = false
        blockInfo.info
        var ignores = monitorContext.getIgnoreBlockCase()
        for (theignore in ignores){
            if(blockInfo.info.contains(theignore)){
                shouldabandon = true
                break
            }
        }
        Log.d(TAG,"abandonIgnoredUIBlock:${shouldabandon}   ,   info:${blockInfo.getConcernBlockInfo()}")
        return shouldabandon
    }

    fun setAnrWatchType(type: BlockWatcherType):TrBlockMonitor{
        anrWatchType  = type
        return this
    }

    fun isAnrWatcherWorking():Boolean{
        return isWorking
    }

    fun start() {
        Log.d(TAG, "start")

        mSubHandler.postDelayed(Runnable {

            isWorking = true
            if(anrWatchType == BlockWatcherType.WatchDog){
                anrWatchDog.doStartWatch()
            }else if(anrWatchType == BlockWatcherType.AnrFileObverser){
                fileObserver.doStartWatch()
            }else {
                anrWatchDog.doStartWatch()
                fileObserver.doStartWatch()
            }
            //trace信息收集器
            traceCollecter.setWork(true).start()

            checkOverDueCrashRecord()
        },monitorContext.startMonitorDelay());
    }

    fun checkOverDueCrashRecord(){
        HandlerThreadFactory.getTimerThreadHandler().post({
            BlockSource.checkDeleteBlockOverDue(TrBlockMonitor.monitorContext.getBlockOverDueDay())
        })
    }

    fun stop() {
        Log.d(TAG, "stop")
        isWorking = false
        anrWatchDog.setWork(false)
        traceCollecter.setWork(false)
    }




}
