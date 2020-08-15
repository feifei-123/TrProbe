package com.sogou.iot.arch.anr

import android.os.Looper
import android.util.Log
import com.sogou.iot.arch.anr.FileWriter.LINESEPERATOR
import com.sogou.iot.arch.anr.TrBlockMonitor.ANR_TAG

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader


class TraceCollector : Thread() {

    var working = false
    private var mTraceArray: MutableList<TraceBean> = mutableListOf() //堆栈信息
    var mCpuStateList:MutableList<TraceBean> = mutableListOf() //cpu信息

    var mlastCpuInfo:CpuInfo = CpuInfo(0,0,0,0,0,0)

    var trace_interval:Long = TRACE_INTERVAL.toLong()
    var trace_max_size:Long = MAX_TRACE_SIZE.toLong()

    var enableCollectSystemTrace:Boolean = false //采集/data/anr/anr_*信息
    var enableCollectAllThreadState:Boolean = false//采集anr发生时所有线程的状态信息
    var enableCollectMainThreadHis:Boolean = false//采集anr发生时 主线程近几秒内的状态信息系
    var enableCollectLogcat:Boolean = false //采集anr发生时的 logcat 信息

    var doCollectCpuInfo:Boolean = false //是否采集cpu信息




    fun setWork(work: Boolean?): TraceCollector {
        working = work!!
        Log.d(TAG + ANR_TAG, "setWork:$work")
        return this
    }

    fun setTraceInterval(intravel:Long): TraceCollector {
        trace_interval = intravel
        return this;
    }

    fun setTraceMaxSize(maxSize:Long): TraceCollector {
        trace_max_size = maxSize
        return this
    }

    fun setEnableCollectSystemTrace(enable:Boolean):TraceCollector{
        enableCollectSystemTrace = enable
        return this
    }

    fun setEnableAllThreadState(enable: Boolean):TraceCollector{
        enableCollectAllThreadState = enable
        return this;
    }

    fun setEnableMainThreadHistory(enable: Boolean):TraceCollector{
        enableCollectMainThreadHis = enable
        return this;
    }

    fun setEnableCollectLogcat(enable: Boolean):TraceCollector{
        enableCollectLogcat = enable
        return this
    }

    fun setDoCollectCpuInfo(collect:Boolean):TraceCollector{
        doCollectCpuInfo = collect
        return this
    }

    override fun run() {
        super.run()
        while (working) {
            putTraceInfo()
            putCpuInfo()
            try {
                sleep(TRACE_INTERVAL.toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }

    /**
     * 收集主线程堆栈信息
     */
    @Synchronized
    fun putTraceInfo() {
        if (mTraceArray.size >= trace_max_size) {
            mTraceArray.removeAt(0)
        }

        val traceBean = generateThreadTraceBean(Looper.getMainLooper().thread)
        mTraceArray.add(traceBean)
    }

    @Synchronized
    fun getTraceArrayCopy(): MutableList<TraceBean> {
        var tmpTraceArray: MutableList<TraceBean> = mutableListOf()
        tmpTraceArray.addAll(mTraceArray)
        return tmpTraceArray
    }



    fun generateThreadTraceBean(thread: Thread): TraceBean {
        val trace = thread.stackTrace
        val buffer = StringBuffer()
        buffer.append(",thread:" + thread.name + ",id:" + thread.id + ",prio:" + thread.priority + ",state:" + thread.state + "\n")
        var length = 0
        if (trace != null && trace.size > 0) {
            length = trace.size
            for (i in 0 until length) {
                val info = convertStatck2String(trace[i])
                buffer.append(info)
                buffer.append("\n")
            }
        }
        val traceBean = TraceBean()
        traceBean.stamp = System.currentTimeMillis()
        traceBean.msg = buffer.toString()
        return traceBean
    }


    /**
     * 收集当前测cpu负载
     */
    fun putCpuInfo(){
        if (mCpuStateList.size >= trace_max_size) {
            mCpuStateList.removeAt(0)
        }
        val cpuBean = sampleCpuInfo()
        cpuBean?.let {
            //Log.d(TAG,"putCpuInfo - cpuBean:"+cpuBean.msg)
            mCpuStateList.add(it)
        }
    }

    /**
     * 采样cpu信息
     */
    public fun sampleCpuInfo():TraceBean? {

        if(!doCollectCpuInfo) return null

        var cpuReader: BufferedReader? = null
        var pidReader: BufferedReader? = null
        var traceBean:TraceBean = TraceBean()
        traceBean.stamp = System.currentTimeMillis()

        try {
            cpuReader = BufferedReader(InputStreamReader(
                    FileInputStream("/proc/stat")), BUFFER_SIZE)
            var cpuRate: String? = cpuReader.readLine()
            if (cpuRate == null) {
                cpuRate = ""
            }

            var mPid = android.os.Process.myPid()

            pidReader = BufferedReader(InputStreamReader(
                    FileInputStream("/proc/$mPid/stat")), BUFFER_SIZE)
            var pidCpuRate: String? = pidReader.readLine()
            if (pidCpuRate == null) {
                pidCpuRate = ""
            }
            val info = parse(cpuRate, pidCpuRate)
            traceBean.msg = info
            return traceBean
        } catch (throwable: Throwable) {
            Log.e(TAG, "doSample: ", throwable)
        } finally {
            try {
                cpuReader?.close()
                pidReader?.close()
            } catch (exception: IOException) {
                Log.e(TAG, "doSample: ", exception)
            }

        }

        return  null

    }


    /**
     * 解析cpu信息
     */
    private fun parse(cpuRate: String, pidCpuRate: String):String {
        val cpuInfoArray = cpuRate.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (cpuInfoArray.size < 9) {
            return ""
        }

        val user = java.lang.Long.parseLong(cpuInfoArray[2]) //处于用户态的cpu运行时间
        val nice = java.lang.Long.parseLong(cpuInfoArray[3])  //nice值为负的进程锁占用的CPU时间
        val system = java.lang.Long.parseLong(cpuInfoArray[4]) //处于内核态的cpu运行时间
        val idle = java.lang.Long.parseLong(cpuInfoArray[5]) //其它空闲等待时间
        val ioWait = java.lang.Long.parseLong(cpuInfoArray[6]) //iowait等待的cpu时间
        val total = (user + nice + system + idle + ioWait
                + java.lang.Long.parseLong(cpuInfoArray[7])  // 硬中断时间

                + java.lang.Long.parseLong(cpuInfoArray[8])) // 软中断时间

        val pidCpuInfoList = pidCpuRate.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (pidCpuInfoList.size < 17) {
            return ""
        }

        val appCpuTime = (java.lang.Long.parseLong(pidCpuInfoList[13]) //utime 该任务在用户态的运行时间

                + java.lang.Long.parseLong(pidCpuInfoList[14]) //stime 该进程在内核态的运行时间

                + java.lang.Long.parseLong(pidCpuInfoList[15]) //cutime 所有已死线程在用户态运行的时间

                + java.lang.Long.parseLong(pidCpuInfoList[16])) //cstime 所有已死在核心态运行的时间

        val stringBuilder = StringBuilder()
        if (mlastCpuInfo.totalTime != 0L) {

            val idleTime = idle - mlastCpuInfo.idleTime
            val totalTime = total - mlastCpuInfo.totalTime
            if(totalTime >0){
                stringBuilder
                    .append("cpu:")
                    .append((totalTime - idleTime) * 100L / totalTime)
                    .append("% ")
                    .append("app:")
                    .append((appCpuTime - mlastCpuInfo.appCpuTime) * 100L / totalTime)
                    .append("% ")
                    .append("[")
                    .append("user:").append((user - mlastCpuInfo.userTime) * 100L / totalTime)
                    .append("% ")
                    .append("system:").append((system - mlastCpuInfo.systemTime) * 100L / totalTime)
                    .append("% ")
                    .append("ioWait:").append((ioWait - mlastCpuInfo.ioWaitTime) * 100L / totalTime)
                    .append("% ]")
            }
        }
        mlastCpuInfo.userTime = user
        mlastCpuInfo.systemTime = system
        mlastCpuInfo.idleTime = idle
        mlastCpuInfo.ioWaitTime = ioWait
        mlastCpuInfo.totalTime = total
        mlastCpuInfo.appCpuTime = appCpuTime

        return stringBuilder.toString()
    }


    /**
     * 卡顿发生时 手机相关信息
     */
    fun collectAnrInfo(blockInfo: BlockInfo){
        Log.d(TAG,"collectAnrInfo:"+"enableCollectSystemTrace:${enableCollectSystemTrace}" +
                ",enableCollectAllThreadState:${enableCollectAllThreadState}" +
                ",enableCollectMainThreadHis:${enableCollectMainThreadHis}"+
                "enableCollectLogcat:${enableCollectLogcat}")


        var fileList = mutableListOf<String>()

        fileList.add(FileWriter.collectBlockInfo2File(blockInfo))


        if(enableCollectAllThreadState){
            fileList.add(FileWriter.collectAllThreadState2File(blockInfo.id))
        }

        if(enableCollectMainThreadHis){
            fileList.add(FileWriter.collectMainThreadHistory2File(blockInfo.id,getTraceArrayCopy()))
        }

        if(enableCollectLogcat){
            fileList.add(FileWriter.cutLogCat2Folder(blockInfo.id))
        }
        if(enableCollectSystemTrace){
            if(FileWriter.getAnrTraceFile(blockInfo.id).exists()){
                fileList.add(FileWriter.getAnrTraceFileNameById(blockInfo.id))
            }
        }


        blockInfo.fileList = fileList

    }



     class TraceBean {
        var stamp: Long = 0
        var msg:String = ""
    }

    companion object {
        val TAG = TraceCollector::class.java.simpleName
        var MAX_TRACE_SIZE = 20
        var TRACE_INTERVAL = 1000//1秒钟取一次 主线程trace.
        private val BUFFER_SIZE = 1000

        private var mInstance:TraceCollector? = null

        fun getInstance():TraceCollector{
            return mInstance?:TraceCollector().also { mInstance = it }
        }

        fun generateStackByThread(thread: Thread): String {
            val trace = thread.stackTrace
            val buffer = StringBuffer()
            buffer.append(",thread:" + thread.name + ",id:" + thread.id + ",prio:" + thread.priority + ",state:" + thread.state + "\n")
            var length = 0
            if (trace != null && trace.size > 0) {
                length = trace.size
                for (i in 0 until length) {
                    val info = convertStatck2String(trace[i])
                    buffer.append(info)
                    buffer.append(LINESEPERATOR)
                }
            }

            return buffer.toString()
        }

        fun convertStatck2String(stackElement: StackTraceElement): String {
            val stringBuilder = StringBuilder("    ")
            stringBuilder.append(stackElement.className + ".")
                    .append(stackElement.methodName + "(")
                    .append(stackElement.fileName + ":")
                    .append(stackElement.lineNumber.toString() + ")")

            return stringBuilder.toString()
        }

    }
}
