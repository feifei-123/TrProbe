package com.sogou.tm.commonlib.log.service.log;

import java.util.List;

/**
 * 文件名:ILogger
 * 创建者:baixuefei
 * 创建日期:2020/5/15 4:07 PM
 * 职责描述: Logger顶层接口类
 */
public interface ILogger {

    /**
     * 日志输出
     * @param leve
     * @param msg
     */
    void log(int leve, String msg);

    /**
     * 单个追加
     * @param object
     */
    void append(String object);

    /**
     * 批量追加
     * @param list
     */
    void append(List<String> list);

    /**
     * 清除缓存到文件
     */
    void flush();

    /**
     * 关闭
     */
    void close();

}
