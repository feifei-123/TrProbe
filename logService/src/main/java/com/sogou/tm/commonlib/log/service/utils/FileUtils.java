package com.sogou.tm.commonlib.log.service.utils;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.sogou.tm.commonlib.log.Logu;
import com.sogou.tm.commonlib.log.utils.ALog;

import java.io.File;
import java.util.List;

/**
 * 文件名:FileUtils
 * 创建者:baixuefei
 * 创建日期:2020/5/18 4:02 PM
 * 职责描述: TODO
 */


public class FileUtils {

    public static void searchFile(File file, List<File> fileList) {
        if (file.exists() && file.isDirectory()) {
            File[] fs = file.listFiles();
            if (fs != null) {
                for (File f : fs) {
                    searchFile(f, fileList);
                }
            }
        } else if (file.exists() && file.isFile()) {
            fileList.add(file);
        }
    }

    /**
     * 获取文件夹大小
     *
     * @param file File实例
     * @return long
     */
    public static long getFolderSize(File file) {
        long size = 0;
        if(!file.exists()){
            ALog.d("getFolderSize:"+file.getAbsolutePath()+",do not exist");
            return 0;
        }
        try {
            Log.d("feifei","getFolderSize:"+file.getAbsolutePath());
            File[] fileList = file.listFiles();
            if(fileList != null){
                for (int i = 0; i < fileList.length; i++) {
                    if (fileList[i].isDirectory()) {
                        size = size + getFolderSize(fileList[i]);
                    } else {
                        size = size + fileList[i].length();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
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

    private static boolean IS_SDExist = false;
    private static boolean isSdExist() {
        if (IS_SDExist || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            IS_SDExist = true;
            return true;
        }
        return false;
    }

}
