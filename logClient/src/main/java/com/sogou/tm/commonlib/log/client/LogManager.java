package com.sogou.tm.commonlib.log.client;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.sogou.tm.commonlib.log.ILogService;
import com.sogou.tm.commonlib.log.Logu;
import com.sogou.tm.commonlib.log.bean.TMLogBean;
import com.sogou.tm.commonlib.log.utils.ALog;
import com.sogou.tm.commonlib.log.utils.LogFileUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * 文件名:ISender
 * 创建者:baixuefei
 * 创建日期:2020/5/15 4:07 PM
 * 职责描述: 将日志对象 从client端发送到server端
 */

public class LogManager implements ISender, IBinder.DeathRecipient {
    private static final String TAG = LogManager.class.getSimpleName();
    public static String LOG_FILE_DIR = Environment.getExternalStorageDirectory() + File.separator + "log" + File.separator;

    public static final String DEFAULT_LOG_FILE_NAME = "tmlog.tm";

    //参数 key值
    public static final String KEY_BEAN = "logbean";
    public static final String KEY_BEANS = "logbeans";
    public static final String ACTION_TYPE = "action_type";

    //操作类型
    public static final int ACTION_TYPE_WRITE_LOG = 101; //发送单个日志操作
    public static final int ACTION_TYPE_WRITE_LOGS = 102; //发送日志数组操作
    public static final int ACTION_TYPE_COLLECTLOGCAT = 103; //截取logcat日志操作
    public static final int ACTION_TYPE_SAVEFILE_WITHPATH = 104;//单独保存字符串到文件操作

    //collectLogcat(String savepath,long delaytime,boolean clearlogcat); 参数
    public static final String KEY_LOG_SAVE_PATH = "logsavepath";
    public static final String KEY_LOGCAT_CLEAR = "clearlogcat";
    public static final String KEY_LOGCAT_COLLECT_DELAYTIME = "collectdelaytime";

    //saveLogWithPath(String log, String path) 参数
    public static final String KEY_LOG_CONTENT = "logcontent";

    /**
     * MsgBean/String 占用内存设定的比例,用于调整打印字符串的单条字符串的大小。
     */
    public static float ratio = 1.2F;

    private static LogManager instance;

    //上下文
    private Context mConext;
    /**
     * LogCache缓存队列的长度
     * 当Logservice 是内部进程，启动晚于主进程，为了保证不丢日志
     * 解决方法：1。调大buffer，等待主进程起来  2。等待binder连接上再传输日志
     */
    private final int MAX_LOG_BEANS = 200;

    /**包名简称:去最后一个.分割的单词，如com.sogou.example,包名简称为example*/
    private String mPackageShortName;

    /**
     * 检测到关机广播时,flush日志,防止日志丢失
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
                Logu.d("" + Intent.ACTION_SHUTDOWN);
                flush();
            }
        }
    };


     /**Log队列*/
    private LogQueue mLogQueue = null;
    /**tooLargeProcessor IPC大文件处理*/
    private IPCTooLargeProcessor tooLargeProcessor = null;
    /**LogServier连接状态*/
    private ConnectingState mConnectState = ConnectingState.NotConnected;

    /**
     * LogService连接状态
     */
    public enum ConnectingState{
        NotConnected(0), //未连接
        Connecting(1),  //连接中
        Connected(2);  //连接成功

        int value;

        ConnectingState(int v){
            this.value= v;
        }
    }

    /**
     * 构造方法
     * @param context
     */
    private LogManager(Context context) {
        Log.i(TAG, "LogManager init");
        mConext = context;

        bindService();

        startLogQueue();

        registerReceiver();

    }

    /**
     * 单例
     * @param context
     * @return
     */
    public static LogManager getInstance(Context context) {
        synchronized (LogManager.class) {
            if (instance == null) {
                instance = new LogManager(context);
            }
        }
        return instance;
    }

    /**
     * 开启日志队列线程
     */
    private void startLogQueue() {

        mLogQueue = new LogQueue(this,new LogCache(MAX_LOG_BEANS));

        if(enableTooLargeTransactDevide){
            tooLargeProcessor = new IPCTooLargeProcessor(MAX_LOG_BEANS,ratio);
            mLogQueue.setTooLargeProcessor(tooLargeProcessor);
        }

        mLogQueue.start();
    }


    /**
     * 发送日志到LogServer
     * @param beans
     */
    @Override
    public void send2LogService(ArrayList<TMLogBean> beans) {
        doSend2Service(beans);
    }



    private ILogService mStub = null;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mStub = ILogService.Stub.asInterface(service);

            mConnectState = ConnectingState.Connected;

            try {
                mStub.setDefaultLogSaveFolder(LOG_FILE_DIR);
                service.linkToDeath(LogManager.this, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
                ALog.e("serviceConnection exception:"+e.getLocalizedMessage());
            }
            Log.i(TAG, "[onServiceConnected]");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mStub = null;
            mConnectState = ConnectingState.NotConnected;
            Log.i(TAG, "[onServiceDisconnected]");
        }
    };




    /**
     * 计算包名,只保留最后一个字段
     */
    private String shortPackageName = "";
    public String getShortPackageName(){
        if(TextUtils.isEmpty(shortPackageName)){
            shortPackageName = getPackageName().substring(getPackageName().lastIndexOf(".") + 1);
            ALog.d(TAG, "shortPackageName:" + shortPackageName);
        }

        return shortPackageName;
    }

    public  String getPackageName(){
        if(mPackageShortName == null) {
            mPackageShortName = mConext.getPackageName();
            ALog.d(TAG, "getPackageName:" + mPackageShortName);
        }

        return mPackageShortName;
    }


    private void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SHUTDOWN);
        mConext.registerReceiver(mBroadcastReceiver, intentFilter);
    }


    /**绑定LogService*/
    private void bindService() {
        mConnectState = ConnectingState.Connecting;
        try {
            Intent intent = getLogServiceIntent();
            mConext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            mConnectState = ConnectingState.NotConnected;
            Log.i(TAG, e.getMessage());
        }

    }

    public  Intent getLogServiceIntent() {
        String packageName = getPackageName();
        Intent intent = new Intent("com.sogou.tm.commonlib.log.LogService.action");
        intent.setComponent(new ComponentName(packageName, "com.sogou.tm.commonlib.log.service.LogService"));
        return intent;
    }

    @Override
    public void binderDied() {
        if (mStub != null) {
            mStub.asBinder().unlinkToDeath(this, 0);//解除死亡代理
            mStub = null;
        }
        mConnectState = ConnectingState.NotConnected;
    }


    public static void setLogSaveFolder(String logFolder) {
        LOG_FILE_DIR = logFolder;
        ALog.d(TAG, "setLogSaveFolder:" + logFolder);
    }



    //查询是否存在
    private boolean isRunning() {
        boolean isRuning = LogFileUtils.isServiceRunning(mConext, getLogServiceIntent().getComponent().getClassName());
        Log.e(TAG, "isRuning:" + isRuning);
        return isRuning;
    }

    /**解除绑定*/
    public void release() {
        mConext.unbindService(serviceConnection);
    }

    public void logu(int type, int leve, String msg) {
        TMLogBean bean = new TMLogBean(type, leve, getShortPackageName(), "", msg);
        mLogQueue.put(bean);
    }


    //是否启动 大日志 自动分隔策略
    public static boolean enableTooLargeTransactDevide = true;


    private void go2Connect() {
        if (mConnectState == ConnectingState.NotConnected) {
            bindService();
        }
    }

    private void doSend2Service(ArrayList<TMLogBean> beans) {
        if (beans == null || beans.size() <= 0) return;
        try {
            //ALog.d("doSend2Service beans.size:"+beans.size()+",isLogBinded():"+isLogBinded());
            if (isLogBinded()) {
                log2Binder(beans);
            } else {
                go2Connect();
                logToServices(beans);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "[log]", e);
        }
    }


    /**
     * LogService建立连接后,通过Binder 传递日志实体
     * @param beans
     */
    public void log2Binder(ArrayList<TMLogBean> beans){
        try {
            if (beans.size() > 1) {
                mStub.logs(beans);
            } else {
                mStub.log(beans.get(0));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * LogService未连接时,通过startService的方式传递数据
     * @param beans
     */
    public void logToServices(ArrayList<TMLogBean> beans) {
        if (beans == null || beans.size() <= 0) return;
        try {
            Intent intent = getLogServiceIntent();
            if (beans != null && beans.size() > 1) {
                intent.putExtra(ACTION_TYPE, ACTION_TYPE_WRITE_LOGS);
                intent.putParcelableArrayListExtra(KEY_BEANS, beans);
            } else {
                intent.putExtra(ACTION_TYPE, ACTION_TYPE_WRITE_LOG);
                intent.putExtra(KEY_BEAN, beans.get(0));
            }
            mConext.startService(intent);

        } catch (Exception e) {
            ALog.i(TAG, e.getMessage());
        }
    }


    /**
     * 清空缓存队列
     */
    public void flush(){
        try {
            mLogQueue.flushCache();
        } catch (Exception e) {
            Log.d(TAG,"flushLog exception:"+e.getLocalizedMessage());
        }
    }

    /**
     * LogService是否已经绑定
     * @return
     */
    private boolean isLogBinded(){
        return mConnectState == ConnectingState.Connected && mStub != null;
    }

    /**
     *  收集Logcat日志
     * @param savepath
     * @param delaytime
     * @param clearlogcat
     */
    public void collectLogcat(String savepath, long delaytime, boolean clearlogcat) {
        try {
            if (isLogBinded()) {
                Log.i(TAG, "mStub.collectlogcat");
                mStub.collectlogcat(savepath, delaytime, clearlogcat);
            } else {
                Log.i(TAG, "collectLogcatByStartService");
                try {
                    Intent intent = getLogServiceIntent();
                    intent.putExtra(KEY_LOG_SAVE_PATH, savepath);
                    intent.putExtra(KEY_LOGCAT_CLEAR, delaytime);
                    intent.putExtra(KEY_LOGCAT_COLLECT_DELAYTIME, clearlogcat);
                    mConext.startService(intent);

                } catch (Exception e) {
                    Log.i(TAG, e.getMessage());
                }
                go2Connect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "[collectLogcat]", e);
        }
    }

    /**
     * 保存字符串到独立文件(非追加的方式操作文件)
     * @param log
     * @param path
     */
    public void saveLogWithPath(String log, String path) {
        try {
            if (isLogBinded()) {
                Log.i(TAG, "mStub.collectlogcat");
                mStub.saveLogWithPath(log, path);
            } else {
                Log.i(TAG, "collectLogcatByStartService");
                try {
                    Intent intent = getLogServiceIntent();
                    intent.putExtra(KEY_LOG_SAVE_PATH, path);
                    intent.putExtra(KEY_LOG_CONTENT, log);
                    mConext.startService(intent);

                } catch (Exception e) {
                    Log.i(TAG, e.getMessage());
                }

                go2Connect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "[collectLogcat]", e);
        }
    }




}
