package com.sogou.tm.commonlib.log;


import android.content.Context;
import com.sogou.tm.commonlib.log.client.LogCient;

public class LogContext {

    public  Context mContext;
    /**
     * log文件保存目录
     */
    public String getLogSavePath(){
        return "/sdcard/sogou/log";
    }


    public void setAppContext(Context context){
        mContext = context;
    }

    /**
     * 获取context
     */
    public Context getAppContext(){
        return mContext;
    }


    /**
     * 是否显示logcat 日志
     */
    public Boolean getShowLog(){
        return true;
    }

    /**
     * 大日志内容自动截断
     * 快进程传递数据 大小超过900K,会报错 需要拆分。
     */
    public Boolean enableToolargeAutoDevice(){
        return true;
    }

    /**
     * 大文件拆分 比例
     */
    public Float autoDiviceRatio(){
        return 0.5F;
    }

    /**
     * 打印log时 的堆栈层级,用于获取调用堆栈和行数。点击日志，跳转到源文件位置。
     */
    public Integer getBaseStackIndex(){
        return LogCient.mBaseIndex;
    }

    /**
     * log输出前缀
     */
    public String getLogPre(){
        return "tr";
    }
}