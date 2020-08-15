package com.sogou.tm.commonlib.log.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sogou.tm.commonlib.log.Logu;
import com.sogou.tm.commonlib.log.bean.TMLogBean;

import java.util.TimeZone;

import static android.content.Intent.ACTION_TIMEZONE_CHANGED;

public class DateTimeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

       String action =  intent.getAction();
       if(ACTION_TIMEZONE_CHANGED.equals(action)){
           Logu.d("DateTimeReceiver - ACTION_TIMEZONE_CHANGED:"+TimeZone.getDefault().toString());
           TMLogBean.S_D_FORMAT.setTimeZone(TimeZone.getDefault());
       }
    }
}
