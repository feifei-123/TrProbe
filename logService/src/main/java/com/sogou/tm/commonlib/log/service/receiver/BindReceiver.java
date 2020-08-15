package com.sogou.tm.commonlib.log.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sogou.tm.commonlib.log.service.LogService;
import com.sogou.tm.commonlib.log.utils.ALog;

/**
 * 开机启动广播
 */

public class BindReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ALog.d("TM_LogService", "" + intent.getAction());
        context.startService(new Intent(context, LogService.class));
    }
}
