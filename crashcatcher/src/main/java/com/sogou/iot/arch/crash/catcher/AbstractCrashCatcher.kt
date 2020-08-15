package com.sogou.iot.arch.crash.catcher

import com.sogou.iot.arch.crash.context.ICrashContext

interface AbstractCrashCatcher {
    fun install(crashContext: ICrashContext)
}