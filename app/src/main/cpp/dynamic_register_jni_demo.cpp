//
// Created by 飞飞 on 2019-05-09.
//

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

#include <android/log.h>
#define LOG_TAG "feifei_crash"

#define ALOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


#ifdef __cplusplus
extern "C"{
#endif




static const char * className = "com/sogou/translate/jni/NativeUtils";

//自定义的nativie 方法
static void doSomeThing(JNIEnv * env,jobject iobject,jstring message){
    const char *msg = env->GetStringUTFChars(message,0);
    ALOGD("JNI  natvie doSomeThing:%s",msg);
    env->ReleaseStringUTFChars(message, msg);
}

/**
 * 将 java方法 与Native方法 doSomeThing 进行关联
 */
static JNINativeMethod gJni_Methods_table[]={
        {"sayHello","(Ljava/lang/String;)V",(void*)doSomeThing},
};


//注册Native方法
static int jniRegisterNativeMethods(JNIEnv*env, const char *className,
        const JNINativeMethod * gMethods,int numMethods
        ){
    jclass clazz;
    ALOGD("JNI ,Registering %s natvies functions\n",className);

    clazz = env->FindClass(className);
    if(clazz == NULL){
        ALOGD("Native registration unable to find class '%s'",className);
        return -1;
    }

    int result = 0;
    //env->注册 Natives方法
    if(env->RegisterNatives(clazz,gMethods,numMethods)<0){

        ALOGD("RegisterNatives failed for '%s'",className);
        return -1;
    }

    env->DeleteLocalRef(clazz);

    return result;

}



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

    if(vm->GetEnv((void**)&env,JNI_VERSION_1_6)!=JNI_OK){
        return JNI_ERR;
    }

   int result = jniRegisterNativeMethods(env, className, gJni_Methods_table, sizeof(gJni_Methods_table) / sizeof(JNINativeMethod));

    ALOGD("jniRegisterNativeMethods,result:%d", result);

    return JNI_VERSION_1_6;
}

/**
 * 当虚拟机释放该C库时，则会调用JNI_OnUnload()函数来进行善后清除动作
 * @param vm
 * @param reserved
 */
//JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
//    ALOGD("===============feife JNI_OnUnload ================");
//    JNIEnv *env = NULL;
//    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
//        return;
//    }
//
//    return;
//
//}

#ifdef __cplusplus
}
#endif


