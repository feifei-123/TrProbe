package com.sogou.translate.example

import android.util.Log
import com.sogou.iot.arch.crash.context.CrashContext
import com.sogou.iot.arch.crash.CrashInfo
import com.sogou.tm.commonlib.log.Logu

class MyCrashContext: CrashContext(){

    //设置crash日志文件保存路径
    override fun getLogSavePath():String{
        return "/sdcard/sogou/log/crash"
    }

    //crash日志文件压缩和上报的接口,日志成功上报到服务端后,需要调用uploadSuccess,通知crashCacher
    override fun zipAndUpload(crashInfo: CrashInfo, uploadSuccess:((success:Boolean, info:String)->Unit)?){
        Log.d("feifei","zipAndUpload:,crashKeyInfo 标识一个crash,可以用于服务端的去重操作:${crashInfo.getKeyInfo()}\n"
        +"generateCrashMsg 用于显示crash的基本信息:${crashInfo.generateCrashMsg()}\n"
        +"FilePathList 指示属于该次cash的日志文件列表:${crashInfo.conver2FilePathList()}}"
        +"日志成功上报到服务端后,需要调用uploadSuccess,通知crashCacher")
        uploadSuccess?.invoke(true,"success")
    }

    /**
     * 回调Crash事件 给宿主应用
     */
    override fun onCrashEvent(crashInfo:CrashInfo){
        Logu.d("feifei---onCrashEvent:${crashInfo}")
    }

}