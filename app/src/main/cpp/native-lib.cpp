#include <jni.h>
#include <string>
#include "com_sogou_translate_jni_NativeUtils.h"
#include <android/log.h>
#define LOG_TAG "feifei_crash"


#define ALOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)



extern "C" JNIEXPORT jstring JNICALL
Java_com_sogou_teemo_myapplication_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

void Crash() {
    volatile int *a = (int *) (NULL);
    *a = 1;
}



extern "C"
JNIEXPORT void JNICALL Java_com_sogou_translate_jni_NativeUtils_go2Crash
        (JNIEnv *env, jclass clazz){
    Crash();

}

extern "C"
JNIEXPORT jstring JNICALL Java_com_sogou_translate_jni_NativeUtils_getDbPhase
        (JNIEnv * env, jclass clazz){
    return (env)->NewStringUTF(DB_PHASE);
}

