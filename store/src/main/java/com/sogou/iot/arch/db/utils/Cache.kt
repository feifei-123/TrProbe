package com.sogou.iot.arch.utils

import android.content.Context

object Cache {

    private val path = "com.sogou.iot.b1pro"

    fun setShutDownId(context: Context,shutdownID:String){
        val key = "shutdownId"
        setStringSP(context,key,shutdownID)
    }

    fun getShutDownId(context: Context):String{
        val key = "shutdownId"
        return getStringSP(context,key)
    }

    fun setStringSP(context:Context,sPKey: String, value: String, sync: Boolean = false) {
        val sp = context.getSharedPreferences(path, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString(sPKey, value)
        if (sync) {
            editor.commit()
        } else {
            editor.apply()
        }
    }


    /**
     * 获取crashId  用于判断是否需要自动上传crash日志
     */
    fun getCrashId(context: Context):String{
        val key = "trcrashId"
        return getStringSP(context,key)
    }

    /**
     * 设置crashId
     */
    fun setCrashId(context: Context,crashId:String){
        val key = "trcrashId"
        setStringSP(context,key,crashId)
    }

    /**
     * 获取 anrId
     */
    fun getAnrId(context: Context):String{
        val key = "tranrId"
        return getStringSP(context,key)
    }

    fun setAnrId(context: Context,anrId:String){
        val key = "tranrId"
        setStringSP(context,key,anrId)
    }

    fun getStringSP(context: Context,sPKey: String): String {
        val sp = context!!.getSharedPreferences(path, Context.MODE_PRIVATE)
        return sp.getString(sPKey, "")!!
    }




}