package com.sogou.tm.commonlib.log.client;

import android.content.Context;
import android.util.Log;

import com.sogou.tm.commonlib.log.bean.TMLogBean;
import com.sogou.tm.commonlib.log.bean.ThrowableBean;
import com.sogou.tm.commonlib.log.utils.ALog;

import java.util.ArrayList;

/**
 * 日志工具，负责将String形式的日志 组装成TMLogBean,然后分发给LogManager
 */
public class LogCient implements ILogInterface {

    public static final String TAG = LogCient.class.getSimpleName();

    /**
     * log写入器
     */
    private LogManager logManager;

    /**
     * 用于指定打印日志时 提取堆栈的层级。
     * mBaseIndex 为 TmLog.d() 层级的方法被调用时 应该选择的堆栈打印层级.
     * <p>
     * 计算 mBaseIndex 的方法，从TmLog.d() 开始数，到内部的Thread.currentThread().getStackTrace() 的函数调用的层级个数(计为M),则mBaseIndex = M+2
     * （Thread.currentThread().getStackTrace() 方法会占用两个层级）
     * <p>
     * 下面为一个堆栈层级的实例:
     * <p>
     * index:10 ----,className:android.os.HandlerThread,method:run,fileName:HandlerThread.java,lineNumber:61
     * index:9  ----,className:android.os.Looper,method:loop,fileName:Looper.java,lineNumber:154
     * index:8  ----,className:android.os.Handler,method:dispatchMessage,fileName:Handler.java,lineNumber:95
     * index:7  ----,className:android.os.Handler,method:handleCallback,fileName:Handler.java,lineNumber:755
     * index:6  ----,className:com.sogou.teemo.translate.launcher.base.AppContext$2,method:run,fileName:AppContext.java,lineNumber:206
     * index:5  ----,className:com.sogou.tm.commonlib.log.client.LogUtil,method:d,fileName:LogUtil.java,lineNumber:83
     * index:4  ----,className:com.sogou.tm.commonlib.log.client.TmLog,method:d,fileName:TmLog.java,lineNumber:54
     * index:3  ----,className:com.sogou.tm.commonlib.log.client.TmLog,method:log,fileName:TmLog.java,lineNumber:175
     * index:2  ----,className:com.sogou.tm.commonlib.log.client.TmLog,method:reseal,fileName:TmLog.java,lineNumber:126
     * index:1  ----,className:java.lang.Thread,method:getStackTrace,fileName:Thread.java,lineNumber:1566
     * index:0  ----,className:dalvik.system.VMStack,method:getThreadStackTrace,fileName:VMStack.java,lineNumber:-2
     */
    public static int mBaseIndex = 5;


    public void initLogEngine(Context context) {
        ALog.i("TmLog", "init");
        logManager = LogManager.getInstance(context);
    }


    /**
     * 接口实现
     **********************/

    @Override
    public void logu(int type, int level, String msg) {
        checkLogMangerInited();
        if(logManager != null) logManager.logu(type, level, msg);
    }

    @Override
    public void crash(int type, int leve, String msg, Throwable ex) {
        checkLogMangerInited();
        flushLog();
        try {
            ThrowableBean throwableBean = null;
            if (ex != null) {
                throwableBean = new ThrowableBean(logManager.getShortPackageName(), ex.getClass().getName(), ex.getMessage(), ex.getStackTrace());
            }
            ArrayList<TMLogBean> beans = new ArrayList<>();
            beans.add(new TMLogBean(type, leve, logManager.getShortPackageName(), "", msg, throwableBean));
            if (logManager != null) {
                logManager.logToServices(beans);
            }
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
    }

    @Override
    public void anr(int type, int level, String msg) {
        checkLogMangerInited();
        flushLog();
        try {
            ArrayList<TMLogBean> beans = new ArrayList<>();
            beans.add(new TMLogBean(type, level, logManager.getShortPackageName(), "", msg, null));
            Log.d(TAG, "anr_ :" + type + ",msg:" + msg);
            if(logManager != null) logManager.logToServices(beans);
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
    }

    @Override
    public void flushLog() {
        checkLogMangerInited();
        if (logManager != null) logManager.flush();
    }

    @Override
    public void collectLogcat(String savepath, long delaytime, boolean clearlogcat) {
        checkLogMangerInited();
        if (logManager != null) logManager.collectLogcat(savepath, delaytime, clearlogcat);
    }

    @Override
    public void saveLogWithPath(String log, String path) {
        checkLogMangerInited();
        if (logManager != null) logManager.saveLogWithPath(log, path);
    }

    public void probeStackLevel() {
        checkLogMangerInited();
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = stackTrace.length - 1; i >= 0; i--) {
            Log.d("probeStackLevel", "index:" + i + ",className:" + stackTrace[i].getClassName() + ",method:" + stackTrace[i].getMethodName() + ",fileName:" + stackTrace[i].getFileName() + ",lineNumber:" + stackTrace[i].getLineNumber());
        }
    }


    /**
     * set方法
     **********************/

    public void enableTooLargeAutoDevide(boolean enable) {
        LogManager.enableTooLargeTransactDevide = enable;
        Log.d(TAG, "enableTooLargeAutoDevide:" + enable);
    }

    public static void setAutoDivideRatio(float ratio) {
        LogManager.ratio = ratio;
        Log.d(TAG, "setAutoDivideRatio:" + ratio);
    }

    /**
     * 设置日志输出的文件夹
     *
     * @param logFolder
     */
    public void setLogSaveFoloder(String logFolder) {
        if(logManager != null) logManager.setLogSaveFolder(logFolder);
    }

    public void checkLogMangerInited(){
        if(logManager == null){
            ALog.e(" ========= Logu.initLogEngine() must be called, before call Logu  =========");
            ALog.e(" ========= Logu.initLogEngine() must be called, before call Logu  =========");
            ALog.e(" ========= Logu.initLogEngine() must be called, before call Logu  =========");
            ALog.e(" ========= Logu.initLogEngine() must be called, before call Logu  =========");
        }
    }

}
