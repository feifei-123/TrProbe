package com.sogou.iot.arch.anr.context

import android.content.Context
import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.anr.catcher.BlockWatcherType

interface IBlockContext {

    fun setAppContext(context:Context?)
    /**卡顿检测器类型:WatchDog,AnrFileObverser,Both*/
    fun getMonitorType(): BlockWatcherType
    /**调试模式 是否忽略卡顿,仅针对WatchDog有效*/
    fun getIgnoreDebugger():Boolean
    /**卡顿判定们门限，单位ms*/
    fun getBlockTimeLong():Long
    /**FileObserver监控的文件路径*/
    fun getObserverPath():String
//    /**ANR 发生时,关注的进程名。用于发生ANR 系统写/data/anr 文件时,判断是否上报ANR事件*/
    fun getConcernProcesses4Anr():Array<String>
    /**主线程Trace堆栈采集间隔*/
    fun getTraceSampleIntervel():Long
    /**采集主线程堆栈的最大存储个数*/
    fun getTraceMaxSize():Long
    /**
     *  是否采集发生 block 时的 cpu info,
     *  Android O 之后,只有system app 才有权限访问   /proc/stat,采集cpu信息
     *  所有非系统app，可以关闭此选项，否则会报错。
     */
    fun doCollectCpuInfo():Boolean
    /**是否尝试采集 系统ANR 文件*/
    fun collectSystemTrace():Boolean
    /**是否采集 所有线程的状态*/
    fun collectAllThreadState():Boolean
    /**是否收集logcat信息*/
    fun collectLogcatInfo():Boolean
    /**是否收集 主线程最近N秒的 历史状态*/
    fun collectMainThreadHistory():Boolean
    /**anr 日志保存位置*/
    fun getLogSavePath():String
    /**设置关注的package信息,一般为自己应用的包名,用于从block堆栈信息中 提取出你所关注的函数调用*/
    fun getConcernPackageNames():Array<String>
    /**设置 应该忽略的 Block Case。比如 crash时的ANR 信息.*/
    fun getIgnoreBlockCase():Array<String>
    /**将相关的文件压缩成zip文件,等待上传*/
    fun zipAndUpload(blockInfo: BlockInfo, uploadSuccess:((success:Boolean, info:String)->Unit)?)
    /**是否显示Block Toast提示*/
    fun enableDisplayToast():Boolean
    /** 获取上下文信息*/
    fun getAppContext():Context?
    /**数据库中 Block记录的过期时间,单位:天*/
    fun getBlockOverDueDay():Int
    /**
     * 程序启动之初 经常会出现CPU 资源紧张,导致UIBLOCK的误报,增加一个延时来启动UIBlock监控，来规避此问题。
     */
    fun startMonitorDelay():Long

    /**回调给宿主app Block事件*/
    fun onBlockEvent(blockInfo: BlockInfo)
}
