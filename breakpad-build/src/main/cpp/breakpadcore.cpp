#include <stdio.h>
#include <jni.h>
#include <semaphore.h>
#include "client/linux/handler/exception_handler.h"
#include "client/linux/handler/minidump_descriptor.h"
#include "com_sogou_translate_breakpad_BreakPadCore.h"
#include "pthread.h"
#include "mylog.h"
#include <dlfcn.h>


//global对象 在on_unload方法里 需要手动释放

//全局的infohelper
jobject global_infohelper;
//全局的标志位interruptSystemCrash,标志是否阻断系统处理捕捉到的crash
jboolean global_interruptSystemCrash = JNI_FALSE;
/**crashInfoBuffer 存储crash详细信息的buffer*/
char * crashInfoBuffer;
int crashKeyBufferSize = 3000;


JavaVM* struct_jvm = NULL;
jmethodID struct_method;


/**线程对象*/
pthread_t pthread;
/**信号量 - 用于阻塞线程 等待crash的发生*/
sem_t bin_sem;

/**
 * 将crash信息传递给java对象
 */
extern void passCrashInfo2Java();

/**
 * 线程执行函数
 * @param data
 * @return
 */
extern void *threadDoThings(void *data);

/**
 * 初始化一个线程，等待crash发送
 */
extern void initThreadWaitCrash();


/**
 * BreadPad 处理crash的回调方法
 * @param descriptor
 * @param context
 * @param succeeded
 * @return  true,表示该信号已经被处理,不需要传递给系统;false 表示 该信号量未被消费,继续传递给系统处理。
 */
bool DumpCallback(const google_breakpad::MinidumpDescriptor &descriptor,
                  void *context,
                  bool succeeded,char * keyinfo) {
    ALOGD("===============feifei crash happened ================");
    //ALOGD("Dump path: %s ,%d\n,%s", descriptor.path(),succeeded,keyinfo);
    memset(crashInfoBuffer,'\0',crashKeyBufferSize);
    snprintf(crashInfoBuffer, crashKeyBufferSize, "%s",keyinfo);

    sem_post(&bin_sem);
    sleep(1);

    ALOGD("===============feifei interruptSystemCrash:%d",global_interruptSystemCrash);
    return global_interruptSystemCrash;
}





/**
 * 初始化 breadpad
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_sogou_translate_breakpad_BreakPadCore_initBreakpadNative(JNIEnv *env, jclass type, jstring path_,
                                                         jobject infoHelper) {
    //初始化BreadPad 指定minidump文件保存的位置
    //（1） 调用minidump 向sdcard写minidump
    const char *path = env->GetStringUTFChars(path_, 0);
    google_breakpad::MinidumpDescriptor descriptor(path);

    //(2) 调用marcodump 在logcat中打印日志
    //const google_breakpad::MinidumpDescriptor::MicrodumpOnConsole microdumpOnConsole ={};
    //google_breakpad::MinidumpDescriptor descriptor(microdumpOnConsole);
    static google_breakpad::ExceptionHandler eh(descriptor, NULL, DumpCallback, NULL, true, -1);
    env->ReleaseStringUTFChars(path_, path);

    ALOGD("===============feifei initBreakpadNative ================:process:%d,thread:%d",getpid(),gettid());

    global_infohelper = env->NewGlobalRef(infoHelper);

    initThreadWaitCrash();
}

/**
 * 初始化一个线程,阻塞在bin_sem,等待crash发生
 */
void initThreadWaitCrash(){

    int res = sem_init(&bin_sem, 0, 0);   /* 初始化信号量，并且设置初始值为0*/
    if (res != 0) {
        ALOGD("Semaphore initialization failed");
        exit(EXIT_FAILURE);
    }
    pthread_create(&pthread, NULL, threadDoThings, NULL);

}

void *threadDoThings(void *data)
{
    ALOGD("feifei  jni thread do things, wait signal");
    sem_wait(&bin_sem);
    passCrashInfo2Java();
    pthread_exit(&pthread);
}


/**
 * 将crash详细信息,传递给java层对象
 */
void passCrashInfo2Java(){
    JNIEnv *env = NULL;
    int getEnvStat;
    bool isAttached = false;
    getEnvStat = struct_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if(env == NULL){
        ALOGD("feifei status was %d ,env was null,%p",getEnvStat,env);
    } else{
        ALOGD("feifei status was %d ,env was not null %p",getEnvStat,env);
    }


    if (getEnvStat == JNI_EDETACHED||getEnvStat == JNI_ERR||getEnvStat == JNI_EVERSION) {
        if (struct_jvm->AttachCurrentThread(&env, NULL))////将当前线程注册到虚拟机中
        {
            return;
        }
        isAttached = true;
    }
    ALOGD("feifei isAttached %d,env address:%p",isAttached,env);

    jobject jobject = global_infohelper;
    if((env)->IsSameObject(jobject,NULL)){
        ALOGD("feifei AllocObject is null ");
    } else{
        ALOGD("feifei AllocObject not null,%p",jobject);
    }


    if(struct_jvm == NULL){
        ALOGD("feifei struct_jvm is null ");
    } else{
        ALOGD("feifei struct_jvm is not null %p",struct_jvm);
    }

    jclass  myClass = (env)->GetObjectClass(jobject);

    struct_method = (env)->GetMethodID(myClass,"onNativeCrash","(Ljava/lang/String;)V");
    if(struct_method == NULL){
        ALOGD("feifei struct_method is null ");
    } else{
        ALOGD("feifei struct_method is not null %p",struct_method);
    }

    //调用Java方法
    jstring crashInfo = (env)->NewStringUTF(crashInfoBuffer);
    (env)->CallVoidMethod(jobject, struct_method,crashInfo);
    (env)->DeleteLocalRef(crashInfo);

    if (isAttached) {
        struct_jvm->DetachCurrentThread();
    }

}


// ------- 几个创造crash的方法 -------

int testCrash_null_point(){
    volatile int *a = (int *) (NULL);
    *a = 1;
    return 1;
}

int testCrash_divide_zero(){
    int i = 0;
    int j = 10/i;
}



int call_dangerous_function() {
    testCrash_null_point();
    return 42;
}


// ------------ JNI 方法  ------------


/**
 * JNI 加载之初
 * @param vm
 * @param reserved
 * @return
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {

    ALOGD("===============feife JNI_OnLoad ================");

    if (vm == NULL)
    {
        return JNI_ERR;
    }

    JNIEnv *env = NULL;
    struct_jvm = vm;

    if(vm->GetEnv((void**)&env,JNI_VERSION_1_6)!=JNI_OK){
        return JNI_ERR;
    }

    //初始化crashInfoBuffer
    crashInfoBuffer = (char *)malloc(crashKeyBufferSize);
    return JNI_VERSION_1_6;
}

/**
 * 当虚拟机释放该C库时，则会调用JNI_OnUnload()函数来进行善后清除动作
 * @param vm
 * @param reserved
 */
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    ALOGD("===============feife JNI_OnUnload ================");
    JNIEnv *env = NULL;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    env->DeleteGlobalRef(global_infohelper);
    sem_destroy(&bin_sem);
    free(crashInfoBuffer);
    return;

}



/**
 * 创造一个 natvie crash
 * @param env
 * @param clazz
 */
JNIEXPORT void JNICALL Java_com_sogou_translate_breakpad_BreakPadCore_go2crash
        (JNIEnv * env, jclass clazz){

    call_dangerous_function();
//    testCrash_divide_zero();
//    testCrash_stackoverflow();
}


/**
 * 设置捕捉到natvie crash之后,是否将该crash传递给系统
 * @param env
 * @param jclazz
 * @param dointerrupt
 */
JNIEXPORT void JNICALL Java_com_sogou_translate_breakpad_BreakPadCore_setInterrupteSysNativeCrash
        (JNIEnv * env, jclass jclazz, jboolean dointerrupt){
    ALOGD("feifei setInterrupteSysNativeCrash:%d",dointerrupt);
    global_interruptSystemCrash = dointerrupt;

}


/**
 * 测试UnWind 打印当前线程的堆栈
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_sogou_translate_breakpad_BreakPadCore_testUnWind(JNIEnv *env, jclass type) {

    void* uframes[BACKTRACE_FRAMES_MAX] = {0};
    ssize_t  n = coffeecatch_unwind_signal(uframes,BACKTRACE_FRAMES_MAX);
    printStack(n,uframes);

}

