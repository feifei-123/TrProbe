package com.sogou.iot.arch.anr

data class CpuInfo(
        var userTime:Long, //用户态的cpu时间
        var systemTime:Long,//内核态的用户时间
        var ioWaitTime:Long,//iowait时间
        var idleTime:Long, //iowait之外的等待时间
        var totalTime:Long,// 总的cpu 时间
        var appCpuTime:Long //特定进程的总cpu运行时间

){

}