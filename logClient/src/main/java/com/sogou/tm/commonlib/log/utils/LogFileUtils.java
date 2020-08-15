package com.sogou.tm.commonlib.log.utils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.util.List;

/**
 * Created by zhangcb on 2018/7/17.
 */

public class LogFileUtils {
    private static boolean IS_SDExist = false;
    public static boolean isSdExist() {
        if (IS_SDExist || Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            IS_SDExist = true;
            return true;
        }
        return false;
    }
    /**
     * 计算SD卡的剩余空间
     *
     * @return 剩余空间
     */
    @SuppressWarnings("deprecation")
    public static long getSDAvailableSize() {
        if (isSdExist()) {
            try {
                File sdcardDir = Environment.getExternalStorageDirectory();
                StatFs sf = new StatFs(sdcardDir.getPath());
                long blockSize = sf.getBlockSize();
                long availCount = sf.getAvailableBlocks();
                return availCount * blockSize;
            } catch (IllegalArgumentException e) {
                return 0;
            }
        }
        return 0;
    }

    public static boolean isServiceRunning(Context context, String className) {
        Log.i("zcb","isServiceRunning className:"+className);
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);
        if(!(serviceList.size() > 0)) {
            return false;
        }
        for(int i = 0; i < serviceList.size(); i++) {
            ActivityManager.RunningServiceInfo serviceInfo = serviceList.get(i);
            ComponentName serviceName = serviceInfo.service;
            if(serviceName.getClassName().equals(className)) {
                return true;
            }
        }
        return false;
    }
}
