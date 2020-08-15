package com.sogou.translate.example

import android.util.Log
import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.anr.context.CommonBlockConext
import com.sogou.tm.commonlib.log.Logu

class MyComBlockContext: CommonBlockConext() {
    /**设置block文件保存的路径*/
    override fun getLogSavePath():String{
        return "/sdcard/sogou/log/anr"
    }

    /**
     *   设置关注的package信息,一般为自己应用的包名,用于从block堆栈信息中 提取出你所关注的函数调用
     */
    override fun getConcernPackageNames():Array<String>{
        var concerns:Array<String> = arrayOf("com.sogou.translate.example");
        return concerns;
    }

    /**
     * 用于 上报block信息和block文件,block信息上传成功后，需要回调uploadSuccess() 告知blockCacher已经上报成功.
     */
    override fun zipAndUpload(blockInfo: BlockInfo, uploadSuccess:((success:Boolean, info:String)->Unit)?){
        //此处完成 文件压缩和block上报
        Log.d("feifei","zipAndUpload:KeyInfo 标识某一个block,可以用来供服务端去重:${blockInfo.getKeyInfo()}\n"
                +"info 为block的基本信息:${blockInfo.generateShowMsg()}\n"
                + "FilePathList 为block文件的日志文件列表:${blockInfo.conver2FilePathList()}\n"
                + "block信息上传成功后，需要回调uploadSuccess() 告知blockCacher已经上报成功"
        )
        uploadSuccess?.invoke(true,"success")
    }

    override fun onBlockEvent(blockInfo: BlockInfo) {
        Logu.d("feifei","onBlockEvent - blockInfo:${blockInfo}")
    }
}