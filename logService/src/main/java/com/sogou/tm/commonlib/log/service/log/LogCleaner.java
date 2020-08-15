package com.sogou.tm.commonlib.log.service.log;

import android.util.Log;

import com.sogou.tm.commonlib.log.LogConstant;
import com.sogou.tm.commonlib.log.service.utils.FileUtils;
import com.sogou.tm.commonlib.log.utils.ALog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


/**
 * 文件名:LogCleaner
 * 创建者:baixuefei
 * 创建日期:2020/5/19 10:31 AM
 * 职责描述: 负责清理日志
 */


public class LogCleaner {

    public static final String TAG = LogCleaner.class.getSimpleName();

    public static final int DEFAULT_KEEP_DAY_NUMBERS = 7;// 日志文件保留天数
    public static final int DEFAULT_MAX_FOLDER_SIZE = 50;// 文件夹最大可保存50M文件
    public static final int DEFAULT_KEEP_FREE_STORE = 80;// SDCard存储空间小于80M时自动清理

    public int mKeepDayNumbers = DEFAULT_KEEP_DAY_NUMBERS;
    public int mMaxfoldersize = DEFAULT_MAX_FOLDER_SIZE;
    public int mKeepfreestore = DEFAULT_KEEP_FREE_STORE;
    public static final int MaxFileCanDeleteOnce = 50;//磁盘空间不足时，单次清除操作可以清除的文件的个数。

    public String folderPath = "";

    private static final int CheckByOverdueInvterval = 10*60*1000; //10分钟
    private long mLastTimeCheck = System.currentTimeMillis();

    public LogCleaner(String path){
        this.folderPath = path;
    }

    public boolean tryClearLog(){
        long currentStamp = System.currentTimeMillis();
        //十分钟检测一次
        boolean check = ((currentStamp - mLastTimeCheck)) > CheckByOverdueInvterval;
        if (!check) {
            return false;
        }
        mLastTimeCheck  = System.currentTimeMillis();
        clearFileByOverDue();
        clearFileByDiskSize();
        return true;
    }

    /**
     * 清理 时间上过期的日志文件(超过7或n天的日志)
     */
    public void clearFileByOverDue(){

        File logdir = new File(folderPath);
        ALog.d(TAG,"clearFileByOverDue:"+logdir.getName());
        //检测是否有KEEP_DAY_NUMBERS天以前的，如果有删除
        long tmpStamp = System.currentTimeMillis();
        long shouldDeleteTime = tmpStamp - (mKeepDayNumbers*24*60*60*1000L);
        //Log.d(TAG,"tmpStamp:"+tmpStamp+",mKeepDayNumbers:"+(mKeepDayNumbers*24*60*60*1000L)+",shouldDeleteTime:"+shouldDeleteTime);

        List<File> logFileList = new ArrayList<>();
        FileUtils.searchFile(logdir, logFileList);//搜索 某个文件下的所有的文件,保存在logFileList中
        for (File f : logFileList) {
            if(!canDeleteFile(f)) {//开机启动的文件不能删除,正在写的文件不能删除
                continue;
            }

            if (f.lastModified() < shouldDeleteTime) {
                if (f.exists()) {
                    boolean success = f.delete();
                    ALog.e(TAG, "overdue delete f:" + f.getAbsolutePath()+"，lastModified:"+getWholeTimeString(f.lastModified())+",currentTime:"+getWholeTimeString(shouldDeleteTime)+",success:"+success);
                }
            }
        }
    }

    public void clearFileByDiskSize(){
        File logdir = new File(folderPath);
        if(!logdir.exists()){
            ALog.e(TAG,"clearFileByDiskSize file do not exist:"+ logdir.getName());
            return;
        }
        ALog.d(TAG,"clearFileByDiskSize:"+ logdir.getName());
        List<File> logFileList = new ArrayList<>();
        FileUtils.searchFile(logdir, logFileList);//搜索 某个文件下的所有的文件,保存在logFileList中

        for(int i = 0;i< MaxFileCanDeleteOnce;i++){
            //检测log dir 占用空间是否超过设定限制，如果超过需要删除最老的文件
            long curSize = FileUtils.getFolderSize(logdir) / 1024 / 1024;
            long availableSize = FileUtils.getSDAvailableSize() / 1024 / 1024;
            ALog.i(TAG, "clearFileByDiskSize logDir:"+logdir+",curSize:" + curSize + "M, maxsize:" + mMaxfoldersize + "M,availableSize:" + availableSize+"M,mKeepfreestore:"+ mKeepfreestore+"M");

            if (curSize > mMaxfoldersize || availableSize < mKeepfreestore) {
                //排序（最后修改日期由小到大）
                logFileList.clear();
                FileUtils.searchFile(logdir, logFileList);//搜索 某个文件下的所有的文件,保存在logFileList中
                Collections.sort(logFileList, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        long t1 = f1.lastModified();
                        long t2 = f2.lastModified();
                        return t1 > t2 ? 1 : (t1 < t2 ? -1 : 0);
                    }
                });

                //删除第一条(最旧的一条log文件)
                if (logFileList != null && logFileList.size() > 0) {
                    File needDelFile = logFileList.get(0);
                    //ALog.e(TAG, "oversize delete|needDelFile|" + needDelFile.getAbsolutePath()+",exist:"+needDelFile.exists()+",canDeleteFile:"+canDeleteFile(needDelFile));
                    if (needDelFile != null && needDelFile.exists() && canDeleteFile(needDelFile)) {
                        ALog.e(TAG, "oversize delete:" + needDelFile.getAbsolutePath());
                        needDelFile.delete();
                    }
                }
            }  else {
                break;
            }
        }
    }

    public static String getWholeTimeString(long time) {

        String str = String.valueOf(time);
        if (str.length() < 13) {
            time = time * 1000;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss,SSS");
        String result =  formatter.format(new Date(time));
        ALog.d(TAG,"time:"+time+",readable:"+result);
        return result;
    }

    public boolean canDeleteFile(File file){
        if (file.getName().endsWith(".tm")) {//正在写的文件不能删除
            return false;
        }else {
            return true;
        }
    }


    public void setKeepFreeStore(int keepFree){
        mKeepfreestore = keepFree;
        ALog.d(TAG,"setKeepFreeStore:"+keepFree+",fileName:"+folderPath);
    }

    public void setMaxFolderSize(int maxFSize){
        mMaxfoldersize = maxFSize;
        ALog.d(TAG,"setMaxFolderSize:"+maxFSize+",fileName:"+folderPath);
    }

    public void setKeepDayNumber(int keepDayNumber){
        mKeepDayNumbers = keepDayNumber;
        ALog.d(TAG,"setKeepDayNumber:"+keepDayNumber+",fileName:"+folderPath);
    }

}
