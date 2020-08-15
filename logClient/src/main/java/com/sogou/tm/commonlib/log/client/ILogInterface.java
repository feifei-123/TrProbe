package com.sogou.tm.commonlib.log.client;

/**
 * 文件名:IQueue
 * 创建者:baixuefei
 * 创建日期:2020/5/15 4:50 PM
 * 职责描述: LogClient对外暴露的接口
 */

public interface ILogInterface {

    /**
     * 打印正常业务日志，日志文件追加方式写入
     * @param type
     * @param level
     * @param msg
     */
    public void logu(int type, int level, String msg);

    /**
     * 打印crash日志，日志文件追加方式写入
     * @param type
     * @param leve
     * @param msg
     * @param ex
     */
    public void crash(int type, int leve, String msg, Throwable ex);


    /**
     * 打印anr日志，日志文件追加方式写入
     * @param type
     * @param level
     * @param msg
     */
    public void anr(int type,int level,String msg);

    /**
     * 将日志flush进入 文件。
     */
    public void flushLog();

    /**
     * 截取logcat日志 保存到文件，日志文件不追加。
     * @param savepath 文件保存位置
     * @param delaytime 延时时间
     * @param clearlogcat 截取logcat日志之后 是否清除logcat
     */
    public void collectLogcat(String savepath,long delaytime,boolean clearlogcat);

    /**
     * 将字符串 保存到 文件,用于一次性打印少量日志到文件，日志文件不追加。
     * @param log 字符串
     * @param path 文件保存路径
     */
    public void saveLogWithPath(String log,String path);

}
