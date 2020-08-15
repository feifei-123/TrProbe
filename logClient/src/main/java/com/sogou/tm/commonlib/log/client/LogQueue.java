package com.sogou.tm.commonlib.log.client;

import com.sogou.tm.commonlib.log.LogConstant;
import com.sogou.tm.commonlib.log.bean.TMLogBean;
import com.sogou.tm.commonlib.log.utils.ALog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件名:LogQueue
 * 创建者:baixuefei
 * 创建日期:2020/5/15 4:06 PM
 * 职责描述: LogBean对队列实现。
 * 两级缓存:
 * 1、消费者模式队列mLogQeque,大发送的消息日志 首先会放到该队列。
 * 2、缓存队列LogCache：
 *  2.1 mLogQeque中取出的日志对象，会优先放如缓存队列中,缓存满了之后,发送到LogServer端 进行写文件 ->减少IPC 跨进程通信的次数
 *  2.2 当mLogQeque为空时,会清空Cache队列中的消息。-> 防止日志残留问题: 当上层业务模块停止调用Logu,日志但日志缓存队列Cache未满,导致Cache中的日志残留在client端，无法发送到server端。
 */


public class LogQueue extends Thread implements IQueue{

    private IPCTooLargeProcessor tooLargeProcessor;
    private ArrayDeque<TMLogBean> mLogQeque = new ArrayDeque<>();
    private Object mLogLock = new Object();
    private boolean mLogRuning = true;
    private LogCache mCache;

    ISender mSender ;
    public LogQueue(ISender sender,LogCache cache){
        mSender = sender;
        mCache = cache;
        mCache.setFlushListener(new LogCache.FlushListner() {
            @Override
            public void doFlush(ArrayList<TMLogBean> logBeans) {
                mSender.send2LogService(logBeans);
            }
        });
    }

    /**
     * 两个队列:mLogQeque 日志队列 和 mLogCacheBeans日志缓存队列.
     * mLogQeque 为操作TMLogBean的第一级队列
     * (1)线程启动时,当前队列取空时: 若当前日志没有缓存,启动LogService,然后wait(); 若当前日志有缓存，则写日志到LogService
     * (2)向mLogQeque快速添加日志时（快于LogThread处理能力），日志TMLogBean 会被缓存。添加TMLogBean变慢 或者停止添加MLogBean时，会flush到logService
     * (3)慢速添加MLogBean时，添加一个，就会flush一个 到LogService
     */
    @Override
    public void run() {
        while (mLogRuning) {
            synchronized (mLogLock) {
                TMLogBean logBean = mLogQeque.poll();
                //正常入队列的日志，首先会加入logCache缓存,缓存满了之后再发送到LogService
                //ALog.d("LogQueue.poll():"+logBean);
                if(logBean != null){
                    mCache.put(logBean);
                }else { //队列空闲时,尝试发送日志到LogService
                    try {
                        mCache.doFlush();
                        mLogLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

            }
        }
    }

    @Override
    public void put(TMLogBean bean){
        if(tooLargeProcessor != null){
            tooLargeProcessor.enque(bean,new IPCTooLargeProcessor.EnqueListener(){
                @Override
                public void doQueue(TMLogBean bean) {
                    innerPut(bean);
                }
            });
        }else {
            innerPut(bean);
        }
    }

    /**
     * 具体的入队操作
     * @param bean
     */
    public void innerPut(TMLogBean bean){
        synchronized (mLogLock) {
            mLogQeque.add(bean);
            mLogLock.notifyAll();
        }
    }

    public void  setTooLargeProcessor(IPCTooLargeProcessor processor){
        tooLargeProcessor = processor;
    }

    public void flushCache(){
        mCache.doFlush();
    }

}
