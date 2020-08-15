package com.sogou.iot.arch.crash

interface CrashListener {
    abstract fun onCrashEvent(crashInfo: CrashInfo)
}