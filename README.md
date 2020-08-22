### 一、Introduction
Probe 英文解释为 “探查；用探针探测”
TrProbe 项目主要职责是作为辅助组件,监控和分析 集成它的Android应用的异常。

### 二、项目组成:

#### 1、Logu 日志打印。
特点:
- 支持日志打印到控制台,同时将日志文件输出到本地文件。
- AndroidStudio中中点击打印的日志,蓝色高亮部分，支持点击跳转到源码
- 支持日志文件路径配置和日志文件自动清除策略配置。

#### 2、CrashCatcher Crash的监控和上报
特点:
- 支持Java crash 和Natvie Crash的自动捕捉。其中natvie crash 采用google breakpad 方案，见项目中的breakpad-build模块。
- 支持crash发生时的信息收集，目前会收集crash的崩溃信息,并截取logcat输入到日志文件。
- 支持crash的自动上报,上报时机为app再次启动时。上报操作暴露接口,为宿主项目完成。
- 支持一些个性配置,详细配置信息见CrashContext类

#### 3、BlockCatcher 主线程卡顿监控和上报
特点:
- 支持主线程卡顿(UIBlock)和ANR 监控。UIBlock 监控采用看门狗原理,ANR监控 才有/data/anr文件监听实现。
- 支持主线程卡顿信息的收集，目前可以收集，主线程卡顿发生时的主线程堆栈、最近20秒的主线程堆栈历史、卡顿线程现场所有线程的堆栈信息、卡顿现场的logcat信息输出。
- 支持UIBLOCK卡顿 发生时弹框浮层提示,展示主要信息。
- 支持UIBLOCK发生后的自动上报。上报时机为发生卡顿之后和app再次启动。上报行为对外暴露接口，供宿主app上报到自己的服务器。
- 支持一些个性化配置，详细配置信息见BlockContext类。

### 三、集成方式
#### 1、引入方式

在项目根目录build.gradle buildscrpit中加入      maven { url 'https://dl.bintray.com/feifei123/maven/' } 仓库


```
buildscript {

    repositories {
        google()
        jcenter()
            maven { url 'https://dl.bintray.com/feifei123/maven/' }
        maven {
            url 'https://maven.google.com/'
            name 'Google'
        }
    }
}
```
在主模块buid.gradle中加入依赖

```
dependencies {
      implementation "com.sogou.iot:trprobe:$archives_probe_version"
       implementation "com.sogou.iot:logClient:$archives_probe_version"
       implementation "com.sogou.iot:logService:$archives_probe_version"
       implementation "com.sogou.iot:store:$archives_probe_version"
       implementation "com.sogou.iot:blockcatcher:$archives_probe_version"
       implementation "com.sogou.iot:crashcatcher:$archives_probe_version"
       implementation "com.sogou.iot:breakpad-build:$archives_probe_version"

}
```

#### 2、使用方法

（1）自定义LogContext
```
class MyLogContext:LogContext() {

    //定义日志文件保存路径
    override fun getLogSavePath(): String {
        return "/sdcard/sogou/log"
    }

    //是否将日志输出到logcat中
     
    override fun getShowLog(): Boolean {
        return BuildConfig.DEBUG
    }

    //设置日志输出公共前缀
    override fun getLogPre(): String {
        return "tr"
    }

}
```

（2）自定义BlockContext类
```

class MyBlockContext():BlockContext() {

    //设置block文件保存的路径
    override fun getLogSavePath():String{
        return "/sdcard/sogou/log/anr"
    }

    //Android O 之后,只有system app 才有权限访问/proc/stat,采集cpu信息,非系统app建议关闭。
     override fun doCollectCpuInfo():Boolean{
        return  false
    }

    //选择BLock卡顿监控模式
    override fun getMonitorType(): BlockWatcherType {
        return BlockWatcherType.WatchDog
    }
    //ANR 发生时,关注的进程名。用于发生ANR 系统写/data/anr 文件时,判断是否上报ANR事件
    override fun getConcernProcesses4Anr():Array<String>{
        var processNames = arrayOf("com.sogou.iot.b1pro.launcher","com.sogou.translate.example")
        return processNames
    }

    //设置关注的package信息,一般为自己应用的包名,用于从block堆栈信息中 提取出你所关注的函数调用
    override fun getConcernPackageNames():Array<String>{
        var concerns:Array<String> = arrayOf("com.sogou.translate.example");
        return concerns;
    }

    //用于 上报block信息和block文件,block信息上传成功后，需要回调uploadSuccess() 告知blockCacher已经上报成功.
    override fun zipAndUpload(blockInfo: BlockInfo, uploadSuccess:((success:Boolean, info:String)->Unit)?){
        //此处完成 文件压缩和block上报
        Log.d("feifei","zipAndUpload:KeyInfo 标识某一个block,可以用来供服务端去重:${blockInfo.getKeyInfo()}\n"
        +"info 为block的基本信息:${blockInfo.generateShowMsg()}\n"
        + "FilePathList 为block文件的日志文件列表:${blockInfo.conver2FilePathList()}\n"
        + "block信息上传成功后，需要回调uploadSuccess() 告知blockCacher已经上报成功"
                )
        uploadSuccess?.invoke(true,"success")
    }
}
```

（3）自定义CrashContext
```
class MyCrashContext: CrashContext(){

    //设置crash日志文件保存路径
    override fun getLogSavePath():String{
        return "/sdcard/sogou/log/crash"
    }
    
    //crash日志文件压缩和上报的接口,日志成功上报到服务端后,需要调用uploadSuccess,通知crashCacher
    override fun zipAndUpload(crashInfo: CrashInfo, uploadSuccess:((success:Boolean, info:String)->Unit)?){
        Log.d("feifei","zipAndUpload:,crashKeyInfo 标识一个crash,可以用于服务端的去重操作:${crashInfo.getKeyInfo()}\n"
        +"generateCrashMsg 用于显示crash的基本信息:${crashInfo.generateCrashMsg()}\n"
        +"FilePathList 指示属于该次cash的日志文件列表:${crashInfo.conver2FilePathList()}}"
        +"日志成功上报到服务端后,需要调用uploadSuccess,通知crashCacher")
        uploadSuccess?.invoke(true,"success")
    }
}
```

（4）在app自定义的Application类中初始化ProbeEngine
```

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        ProbeEngine.setContext(this)
            .setLogContext(MyLogContext())
            .setBlockContext(MyBlockContext())
            .setCrashContext(MyCrashContext())
            .install()
        
        mContext = this
      
    }
}
```
### 四、原理介绍

[信号机制和Android natvie crash捕捉](https://www.jianshu.com/p/e00e23d0fa01)

[breakpad的正确编译和常规用法](https://www.jianshu.com/p/1e15640fae7a)