package com.sogou.iot.arch.crash


enum class CrashType(var value:Int) {
    JAVACRASH(1),
    NATIVECRASH(2);

    companion object{
        fun toCrashType(value:Int): CrashType {
            return when(value){
                1-> CrashType.JAVACRASH
                2-> CrashType.NATIVECRASH
                else -> CrashType.NATIVECRASH
            }
        }
    }
}