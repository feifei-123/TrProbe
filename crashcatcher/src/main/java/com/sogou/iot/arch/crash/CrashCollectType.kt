package com.sogou.iot.arch.crash

enum class CrashCollectType {

    REBOOT, //crash发生后,app重启时收集crash信息。适用于launcher类的自动启动的应用。
    OTHERPROCESS // crash 发生时,通过其他进程来收集crash信息,适用于常规的手机应用。
}