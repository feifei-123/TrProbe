package com.sogou.iot.arch.anr.context

import android.content.Context
import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.anr.catcher.BlockWatcherType
import com.sogou.iot.arch.anr.catcher.WatchDogCatcher

abstract class CommonBlockConext : IBlockContext {

    /**
     * 上下文信息,必须设置
     */
    var mContext: Context? = null

    override fun  setAppContext(context: Context?){
        mContext = context
    }

    override fun getAppContext(): Context?{
        return mContext
    }

    /**
     * ANR 发生时,关注的进程名。用于发生ANR 系统写/data/anr 文件时,判断是否上报ANR事件
     */
    override fun getConcernProcesses4Anr():Array<String>{
        var processNames = arrayOf<String>()
        return processNames
    }

    /**
     * 卡顿检测器类型:WatchDog,AnrFileObverser,Both
     */
    override fun getMonitorType(): BlockWatcherType {
        return BlockWatcherType.WatchDog
    }

    /**
     * 调试模式 是否忽略卡顿,仅针对WatchDog有效
     */
    override fun getIgnoreDebugger(): Boolean {
        return true
    }

    /**
     * 卡顿判定们门限，单位ms
     */
    override fun getBlockTimeLong(): Long {
        return WatchDogCatcher.BLOCK_TIMEOUT
    }

    /**
     * FileObserver 监测的文件路径
     */
    override fun getObserverPath(): String {
        return "/data/anr/"
    }

    /**
     * 主线程Trace堆栈采集间隔
     */
    override fun getTraceSampleIntervel(): Long {
        return 1000
    }

    /**
     * 采集主线程堆栈的最大存储个数
     */
    override fun getTraceMaxSize(): Long {
        return 20
    }

    /**
     *  是否采集发生 block 时的 cpu info,
     *  Android O 之后,只有system app 才有权限访问   /proc/stat,采集cpu信息
     *  所有非系统app，可以关闭此选项，否则会报错。
     */
    override fun doCollectCpuInfo(): Boolean {
        return false
    }

    /**
     * 是否尝试采集 系统ANR 文件
     */
    override fun collectSystemTrace(): Boolean {
        return false
    }

    /**
     * 是否采集 所有线程的状态
     */
    override fun collectAllThreadState(): Boolean {
        return true
    }

    /**
     * 是否是手机logcat信息
     */
    override fun collectLogcatInfo(): Boolean {
        return true
    }

    /**
     * 是否收集 主线程最近N秒的 历史状态
     */
    override fun collectMainThreadHistory(): Boolean {
        return true
    }

    /**
     * 设置 应该忽略的 Block Case.
     * 比如 crash时的ANR 信息.
     */
    override fun getIgnoreBlockCase(): Array<String> {
        var ignores: Array<String> = arrayOf("JavaCrashCatcher.uncaughtException");
        return ignores;
    }

    /**
     * 是否显示Block Toast提示
     */
    override fun enableDisplayToast(): Boolean {
        return true;
    }

    /**
     * 数据库中 Block记录的过期时间,单位:天
     */
    override fun getBlockOverDueDay(): Int {
        return 7
    }

    /**
     * 程序启动之初 经常会出现CPU 资源紧张,导致UIBLOCK的误报,增加一个延时来启动UIBlock监控，来规避此问题。
     */
    override fun startMonitorDelay(): Long {
        return 5000;
    }

    /**
     * 回调给宿主app Block事件
     */
    override fun onBlockEvent(blockInfo: BlockInfo){

    }

}