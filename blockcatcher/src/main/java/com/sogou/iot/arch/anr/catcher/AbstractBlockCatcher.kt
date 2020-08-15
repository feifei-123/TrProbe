package com.sogou.iot.arch.anr.catcher

import com.sogou.iot.arch.anr.BlockInfo

interface AbstractBlockCatcher {
    fun doStartWatch()
    fun doStopWatch()
}

interface BlockListener{
    fun onBlock(blockInfo: BlockInfo)
}

enum class BlockWatcherType(val value: Int){
    WatchDog(1), //看门狗方式 检测anr
    AnrFileObverser(2), //检测/data/anr 方式检测anr
    Both(3); //上面另种方式都是用

    companion object{
        fun toBlockWatcherType(value:Int): BlockWatcherType {
            return when(value){
                1-> BlockWatcherType.WatchDog
                2-> BlockWatcherType.AnrFileObverser
                else -> BlockWatcherType.Both
            }
        }
    }
}