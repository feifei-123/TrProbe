// ILogService.aidl
package com.sogou.tm.commonlib.log;

// Declare any non-default types here with import statements
import com.sogou.tm.commonlib.log.bean.TMLogBean;
interface ILogService {
    /**
    * 输出单个日志实体
    */
    void log(in TMLogBean bean);

     /**
       * 输出单个日志实体数组
       */
    void logs(in List<TMLogBean> beans);

    /**
    *  截取logcat日志 输出到指定的文件
    *  @Params savePath 保存路径,
    *  @Params delayTime 延时一段时间 执行截取操作.
    *  @Params delayTime clearLogcat 截取logcat之后 是否清楚logcat
    */
    void collectlogcat(in String savePath,in long delayTime,in boolean clearLogcat);

     /**
       *  将字符串保存到指定文件
       *  @Params log 待保存的日志文件,
       *  @Params path 保存文件的位置
       */
    void saveLogWithPath(in String log,in String path);

    /**
      *  设置默认的日志保存路径
      */
    void setDefaultLogSaveFolder(String path);

}
