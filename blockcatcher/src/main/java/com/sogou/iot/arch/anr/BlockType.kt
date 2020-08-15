package com.sogou.iot.arch.anr

enum class BlockType(var value:Int) {
    UIBLOCK(1), //UI 线程发生卡顿
    ANR(2); //发生了ANR

    companion object{
        fun toBlockType(value:Int):BlockType{
            return when(value){
                1->UIBLOCK
                2->ANR
                else -> ANR
            }
        }
    }

}