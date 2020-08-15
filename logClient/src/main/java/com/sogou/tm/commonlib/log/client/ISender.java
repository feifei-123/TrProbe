package com.sogou.tm.commonlib.log.client;

import com.sogou.tm.commonlib.log.bean.TMLogBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件名:ISender
 * 创建者:baixuefei
 * 创建日期:2020/5/15 4:07 PM
 * 职责描述: 将日志对象 从LogClient端发送到LogServer端
 */

public interface ISender {
    public void send2LogService(ArrayList<TMLogBean> beans);
}
