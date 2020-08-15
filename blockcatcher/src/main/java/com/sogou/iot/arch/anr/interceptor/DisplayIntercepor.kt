package com.sogou.iot.arch.anr.interceptor

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.sogou.iot.arch.anr.BlockInfo
import com.sogou.iot.arch.anr.TrBlockMonitor
import com.sogou.iot.arch.anr.view.BlockRemindDialog
import com.sogou.iot.arch.anr.view.BlockRemindView

class DisplayIntercepor:IBlockInterceptor{


    var MSG_SHOW_TOAST:Int = 10001
    var sendDelay:Long = (TrBlockMonitor.monitorContext.getBlockTimeLong()*1.2).toLong()
    var mainHandler: Handler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message?) {
            var blockInfo = msg?.obj as BlockInfo
            blockInfo?.let {
                doshowRemindView(blockInfo)
            }
        }
    }

    override fun onBlock(info: BlockInfo) {
        showBlockToast(info)
    }



    fun showBlockToast(blockInfo: BlockInfo){
        Log.d("DisplayIntercepor","showBlockToast~:${blockInfo.id},blockTime:${blockInfo.blockTime}")
        var msg = Message.obtain()
        msg.what = MSG_SHOW_TOAST
        msg.obj = blockInfo

        mainHandler.removeMessages(MSG_SHOW_TOAST)
        mainHandler.sendMessageDelayed(msg,sendDelay)

    }

    fun  doshowRemindView(blockInfo: BlockInfo){
        Log.d("DisplayIntercepor","doshowRemindView:${blockInfo.id},blockTime:${blockInfo.blockTime}")
        showRemindViewByDialog(blockInfo)
    }


    var remindView:BlockRemindView? = null
    fun showRemindViewByWindow(blockInfo: BlockInfo){
        if(remindView == null){
            remindView = BlockRemindView(TrBlockMonitor.monitorContext.getAppContext())
        }
        remindView?.let {
            if(!it.isShowing()){
                it.showDialog(blockInfo.generateShowMsg())
            }
        }
    }

    fun showRemindViewByDialog(blockInfo: BlockInfo){
        var intent = Intent(TrBlockMonitor.monitorContext.getAppContext(),BlockRemindDialog::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("msg",blockInfo.generateShowMsg())
        TrBlockMonitor.monitorContext.getAppContext()!!.startActivity(intent)
    }
}