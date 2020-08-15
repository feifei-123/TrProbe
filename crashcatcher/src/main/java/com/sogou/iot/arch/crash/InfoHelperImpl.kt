package com.sogou.iot.arch.crash

import com.sogou.translate.breakpad.InfoHelper


class InfoHelperImpl(var nativeCrashHappen:((crashInfo:String)->Unit)) : InfoHelper {

    override fun onNativeCrash(keyInfo:String) {
        nativeCrashHappen.invoke(keyInfo)

    }

}
