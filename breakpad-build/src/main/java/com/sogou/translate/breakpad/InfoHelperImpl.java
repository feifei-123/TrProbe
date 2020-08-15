package com.sogou.translate.breakpad;

import android.util.Log;

public class InfoHelperImpl implements InfoHelper {

    @Override
    public void onNativeCrash(String keyInfo) {
        Log.d("feifei","-----onNativeCrash:"+Thread.currentThread().getName()+",keyInfo:"+keyInfo);
        ShellUtils.execCommand("logcat -d > /sdcard/logfeifei.log",false);
        Log.d("feifei","-----onNativeCrash finished!");

    }
}
