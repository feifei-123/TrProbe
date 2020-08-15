package com.sogou.translate.breakpad;


public class BreakPadCore {
    public static final String TAG = BreakPadCore.class.getSimpleName();
    static {
        System.loadLibrary("breakpad-core");
    }

    public static void initBreakpad(String path, InfoHelper infoHelper){
        initBreakpadNative(path,infoHelper);
    }

    public native  static void initBreakpadNative(String path,InfoHelper infoHelper);

    /**
     * 是否阻断系统的crash处理
     * @param interrupteSysNativeCrash true时,捕捉到native crash 信号量,处理后，不会传递给系统处理(这样就打断了系统的nativie crash的感知)
     */
    public native static void setInterrupteSysNativeCrash(boolean interrupteSysNativeCrash);

    public native static void go2crash();

    public native static void testUnWind();



}

