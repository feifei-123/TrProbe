package com.sogou.iot.arch.anr.catcher

import android.os.*
import android.util.Log
import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.anr.BlockType
import com.sogou.iot.arch.anr.TrBlockMonitor.ANR_TAG
import com.sogou.iot.arch.anr.TraceCollector



class WatchDogCatcher :Thread(), AbstractBlockCatcher {
    override fun doStartWatch() {

        setWork(true)
        this.start()
    }

    override fun doStopWatch() {
        setWork(false)
    }

    private var blocklistner: BlockListener? = null
    private var isWorking = false
    private var ignoreDebugger:Boolean = true
    private var block_time_long:Long = BLOCK_TIMEOUT



    private var lastTimeTick = -1
    private var timeTick = 0L
    private val ZERO = 0L
    private val RESET_TICK_MSG = 1



    private val watchDogHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if(RESET_TICK_MSG == msg.what){
                setTimeTick(ZERO)
            }
        }
    }


    fun setBlockListner(listener: BlockListener): WatchDogCatcher {
        blocklistner = listener
        return this
    }

    fun setIgnoreDebugger(ignore:Boolean): WatchDogCatcher {
        ignoreDebugger = ignore
        return this
    }

    fun setWork(work: Boolean): WatchDogCatcher {
        isWorking = work
        Log.d(TAG + ANR_TAG, "setWork:$work")
        return this;
    }

    fun setAnrTimeLong(anrTime:Long): WatchDogCatcher {
        block_time_long = anrTime
        return this
    }

    @Synchronized
    fun setTimeTick(tick: Long) {
        timeTick = tick
        //Log.d(TAG,"setTimeTick:"+timeTick);
    }




    fun sendTickMessage() {
        synchronized(this) {
            watchDogHandler.removeMessages(RESET_TICK_MSG)
            watchDogHandler.sendEmptyMessage(RESET_TICK_MSG)
        }
    }



    override fun run() {

        while (isWorking && !isInterrupted) {
//            Log.d_no_print(TAG + ANR_TAG, "SendMessage :" + timeTick + ",last:" + lastTimeTick);

            setTimeTick(timeTick+block_time_long)
            sendTickMessage()//此处必须是原子操作

            try {
                sleep(block_time_long)
            } catch (e: InterruptedException) {
                e.printStackTrace()
                Log.d(TAG + ANR_TAG, "ANRWatchDog_ exception:" + e.stackTrace)
            }

            if (ZERO != timeTick) { //anr happened
                anr_do_happened()
            }
        }
    }

    fun anr_do_happened() {

        if (ignoreDebugger && (Debug.isDebuggerConnected() || Debug.waitingForDebugger())) {
//            Logu.w(
//                    TAG, "An ANR was detected but ignored because the debugger is connected "
//            )
            return
        }

        var blockID = System.currentTimeMillis()
        var keyStack = TraceCollector.generateStackByThread(Looper.getMainLooper().thread)
        var cpuBean = TraceCollector.getInstance().sampleCpuInfo()
        var cpuInfo:String = if(cpuBean != null) cpuBean.msg else "暂无"
        var blockInfo = BlockInfo(
            blockID.toString(),
            BlockType.UIBLOCK,
            BlockWatcherType.WatchDog
            , blockID
            , timeTick
            , keyStack
            , cpuInfo
            , System.currentTimeMillis()
        )
        Log.d(TAG, "anr_happened timeTick:$timeTick,Debug.isDebuggerConnected():${Debug.isDebuggerConnected()}, Debug.waitingForDebugger():${ Debug.waitingForDebugger()},blocklistner was null?:${blocklistner==null}")
        blocklistner?.onBlock(blockInfo)

    }


    companion object {

        val TAG = WatchDogCatcher::class.java.simpleName

        //判定主线程是否出现了卡顿
        val BLOCK_TIMEOUT = 3 * 1000L
    }
}
