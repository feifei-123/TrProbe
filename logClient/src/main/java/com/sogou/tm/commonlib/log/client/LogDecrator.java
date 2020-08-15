package com.sogou.tm.commonlib.log.client;

import android.util.Log;

import com.sogou.tm.commonlib.log.LogConstant;


/**
 * 文件名:LogDecrator
 * 创建者:baixuefei
 * 创建日期:2020/5/15 8:04 PM
 * 职责描述: 负责日志字符串的整理和装饰
 */


public class LogDecrator {

    //公共TAG前缀
    public  static String LOG_PRE = "TR_";
    //公用TAG
    public  static String LOG_TAG = "";

    public static String decorateTag(String tagStr){
        String tag;
        tag = (tagStr == null || tagStr.isEmpty()) ? "" : tagStr;
        tag = LOG_PRE + LOG_TAG + tag;
        return tag;
    }

    public  static String decorate4JumpSource(int type,String tagStr, Object obj, Throwable ex, int index){
        String logStr = obj + "";
        if (type != LogConstant.Log_Type_Statistics) {
            logStr = decorate4JumpSource(tagStr, obj, ex, index);
        }

        return logStr;
    }
    /**
     * 重新组装 日志字符串,携带方法和的类的源文件行号,支持点击跳转到源文件
     * @param tagStr
     * @param obj
     * @param ex
     * @param index
     * @return
     */
    public static String decorate4JumpSource(String tagStr, Object obj, Throwable ex, int index) {
        StringBuilder stringBuilder = new StringBuilder();
        String msg;
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

            //Log.d("feifei","reseal statckIndex:"+index+",stackTrace.length:"+stackTrace.length);
            //for(int i = stackTrace.length-1;i>=0;i--){
            //   System.out.println("index:"+i+"feifei ----,className:"+stackTrace[i].getClassName()+",method:"+stackTrace[i].getMethodName()+",fileName:"+stackTrace[i].getFileName()+",lineNumber:"+stackTrace[i].getLineNumber());
            //}

            index = Math.min(index, stackTrace.length - 1);
            String className = stackTrace[index].getFileName();
            String methodName = stackTrace[index].getMethodName();
            int lineNumber = stackTrace[index].getLineNumber();
            methodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
            stringBuilder.append("[(").append(className).append(":").append(lineNumber).append(")#").append(methodName).append("] ");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (obj == null) {
            msg = "Log with null Object";
        } else {
            msg = obj.toString();
        }
        if (msg != null) {
            stringBuilder.append(msg);
        }

        if (ex != null) {
            stringBuilder.append("\n").append(Log.getStackTraceString(ex));
        }
        String logStr = stringBuilder.toString();
        return logStr;
    }

}
