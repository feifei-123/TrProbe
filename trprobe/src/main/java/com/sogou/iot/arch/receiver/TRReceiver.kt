package com.sogou.iot.arch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import com.sogou.iot.arch.upload.UploadStrategy

class TRReceiver :BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val connectivityManager = context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isAvailable) {
                Log.d("TRReceiver","网路连接成功")
                UploadStrategy.checkUpload()
            } else {
                Log.d("TRReceiver","网路连接断开")
            }
        }
    }


}