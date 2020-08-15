package com.sogou.tm.commonlib.log.client;

import com.sogou.tm.commonlib.log.bean.TMLogBean;

/**
 * 文件名:IQueue
 * 创建者:baixuefei
 * 创建日期:2020/5/15 4:50 PM
 * 职责描述: LogBean的队列
 */


public interface IQueue {
    public void put(TMLogBean bean);
}
