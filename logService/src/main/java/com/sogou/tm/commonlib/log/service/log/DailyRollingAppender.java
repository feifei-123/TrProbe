package com.sogou.tm.commonlib.log.service.log;


import android.util.Log;

import com.sogou.tm.commonlib.log.utils.ALog;
import com.sogou.tm.commonlib.log.service.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;



/**
 * 文件名:DailyRollingAppender
 * 创建者:baixuefei
 * 创建日期:2020/5/15 4:07 PM
 * 职责描述: 带检查日志是否跨天功能的FileWriter
 * 相对于FileAppender 增加了以下几点:
 * - bindLogFile 日志文件初始化(包括重命名)
 * - rollOverDay 日志文件跨天判断
 * - clearFileByOverDue clearFileByDiskSize 日志文件清理操作
 */
public class DailyRollingAppender extends FileAppender {

    public static final String TAG = DailyRollingAppender.class.getSimpleName();

    //日期格式
    private String datePattern = "'.'yyyy-MM-dd";
    SimpleDateFormat sDateFormat;

    /**
     * scheduledFilename 意义:当前打印的日志文件tmlog.tm 对应带日期的文件名,如tmlog.tm.2019-07-16
     * 当日志打印进程跨天后(手机当前的日期 不是2019-07-16时),需要将tmlog.tm 重命名为tmlog.tm.2019-07-16
     * tmlog.tm 永远代表当前正写的日志
     */
    private String scheduledFilename;




    public DailyRollingAppender(String filename,
                                String datePattern) throws IOException {
        super(filename, true);
        this.datePattern = datePattern;
        activateOptions();
    }

    //----- 继承父类的方法 -----//

    @Override
    public void activateOptions() {
        bindLogFile();
        super.activateOptions();
    }

    @Override
    protected void doAppend(String msg) {
        checkRollOverDay();
        try {
            super.doAppend(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doAppend(List<String> list) {
        checkRollOverDay();
        try {
            super.doAppend(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // -------- 扩展的方法 -------- //
    /**
     * 确定FileAppender对象实例 绑定的日志文件
     * 若文件已经存在,则将原始文件重命名。
     */
    void bindLogFile(){
        ALog.e("bindLogFile[" + fileName + "].");
        if (datePattern != null && fileName != null) {
            sDateFormat = new SimpleDateFormat(datePattern);
            File file = new File(fileName);

            //fileName文件已经存在,则将原始文件重命名.
            if (file.exists()) {
                String targetFilename = fileName + sDateFormat.format(new Date(file.lastModified()));
                renameFile(fileName,targetFilename);
            }

             scheduledFilename = fileName + sDateFormat.format(System.currentTimeMillis());

        } else {
            ALog.e("Either File or DatePattern options are not set for appender ["
                    + fileName + "].");
        }
    }


    /**
     * 文件重命名: 将src 重命名为 target.
     * 如 tmlog.tm -> tmlog.tm.2019-07-16
     * 如果tmlog.tm.2019-07-16 文件已经存在了,则将target 修改为tmlog.tm.2019-07-16_1 依次类推
     * @param src
     * @param target
     */
    void renameFile(String src, String target){
        try {

            File targetFile = new File(target);
            int i = 0;
            while (targetFile.exists()) {
                i++;
                targetFile = new File(target + "_" + i);
            }
            File scrFile = new File(src);
            boolean result = scrFile.renameTo(targetFile);
            if (result) {
                ALog.d(src + " ->>> " + target);
            } else {
                ALog.e("Failed to rename [" + src + "] to [" + target + "].");
            }
        }catch (Exception e){
            ALog.e(e.getMessage(),e);
        }
    }


    /**
     * 判断当前日志文件tmlog.tm 有没有跨天
     * 若发生了跨天, 日志文件名tmlog.tm,需要修改为tmlog.tm.日期 (tmlog.tm.2019-07-16_1)的格式
     * 并重新创建tmlog.tm文件
     * @throws IOException
     */
    void rollOverDay() throws IOException {
        if (datePattern == null) {
            ALog.e(TAG,"datePattern is null");
            return;
        }

        String datedFilename = fileName + sDateFormat.format(System.currentTimeMillis());

        if(!scheduledFilename.equals(datedFilename)){
            ALog.e(TAG,"2 scheduledFilename:"+scheduledFilename+",datedFilename:"+datedFilename);
            this.reset();
            renameFile(fileName,scheduledFilename);

            try {
                this.setFile(fileName, true, this.bufferedIO, this.bufferSize);
            } catch (IOException e) {
                ALog.e(TAG,"setFile(" + fileName + ", true) call failed.");
            }
            scheduledFilename = datedFilename;
        }

    }

    void checkRollOverDay(){
        checkFileExist();
        try {
            rollOverDay();
        } catch (IOException ioe) {
            if (ioe instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            ALog.e(TAG,"rollOver() failed.", ioe);
        }
    }

    public void checkFileExist(){

        File currentFile = new File(fileName);
        if(!currentFile.exists()){
            activateOptions();
            ALog.d("checkFileExist fileNotExist , create it again");
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


    // ---------------- getter and setter ----------------//

    public void setDatePattern(String pattern) {
        datePattern = pattern;
    }


    public String getDatePattern() {
        return datePattern;
    }


}




