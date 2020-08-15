package com.sogou.translate.example

import com.sogou.tm.commonlib.log.LogContext

class MyLogContext:LogContext() {

    //定义日志文件保存路径
    override fun getLogSavePath(): String {
        return "/sdcard/sogou/log"
    }

    //是否将日志输出到logcat中

    override fun getShowLog(): Boolean {
        return BuildConfig.DEBUG
    }

    //设置日志输出公共前缀
    override fun getLogPre(): String {
        return "tr"
    }


}