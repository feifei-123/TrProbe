package com.sogou.tm.commonlib.log.client;

import android.util.Log;
import android.util.SparseArray;

import com.sogou.tm.commonlib.log.bean.TMLogBean;
import com.sogou.tm.commonlib.log.utils.ALog;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 文件名:LogCache
 * 创建者:baixuefei
 * 创建日期:2020/5/15 11:41 AM
 * 职责描述: 日志实体 - 二级缓存.
 */


public class LogCache {

    public int maxCacheSize = 200;

    private ConcurrentLinkedDeque<TMLogBean> mLogCacheBeans = new ConcurrentLinkedDeque<>();

    private FlushListner mFlushListener;

    public LogCache(int maxCacheSize){
        this.maxCacheSize = maxCacheSize;
    }

    public void put(TMLogBean bean) {
        mLogCacheBeans.add(bean);
        checkFull();
    }

    public int getCacheSize() {
        return mLogCacheBeans.size();
    }

    public void checkFull() {
        if (getCacheSize() > maxCacheSize) {
            doFlush();
        }
    }

    public void doFlush() {
        //将mLogCacheBeans 按照type 进行分组,然后分别送LogServer
        SparseArray<ArrayList<TMLogBean>> groupArray= divideGroup();
        //ALog.d("doFlush groupArray  - size():"+groupArray.size());
        if(groupArray.size() > 0){
            for(int i = 0;i< groupArray.size();i++){
                ArrayList<TMLogBean> typeList = groupArray.valueAt(i);
                //ALog.d("doFlush groupArray keyAt("+i+"):"+groupArray.keyAt(i)+",array.size:"+groupArray.valueAt(i).size());
                if(typeList != null){
                    if (mFlushListener != null) {
                        mFlushListener.doFlush(typeList);
                    }
                }
            }
        }
    }

    /**
     * 将mLogCacheBeans 按照type 进行分组
     * @return
     */
    public SparseArray<ArrayList<TMLogBean>> divideGroup(){
        SparseArray<ArrayList<TMLogBean>> mGroupArray = new SparseArray<>();

        if(mLogCacheBeans.size() == 0) return mGroupArray;
        ArrayList<TMLogBean> tmpList = new ArrayList<>();
        tmpList.addAll(mLogCacheBeans);
        mLogCacheBeans.clear();

        //ALog.d("divideGroup orginal mLogCacheBeans:"+tmpList.size());
        for(int i = 0;i< tmpList.size();i++){
            TMLogBean bean = tmpList.get(i);
             int type = bean.getType();

             ArrayList<TMLogBean> typeGroup = mGroupArray.get(type);
             if(typeGroup == null){
                 typeGroup = new ArrayList<TMLogBean>();
                 mGroupArray.put(type,typeGroup);
             }
            typeGroup.add(bean);
            //ALog.d("divideGroup orginal bean.type:"+type+",after insert:"+typeGroup.size());
        }
        return mGroupArray;

    }

    public void setFlushListener(FlushListner listener) {
        this.mFlushListener = listener;
    }

    public void setMaxCacheSize(int size) {
        this.maxCacheSize = size;
    }


    public interface FlushListner {
        public void doFlush(ArrayList<TMLogBean> logBeans);
    }

}
