package com.sogou.iot.arch.anr.interceptor

import com.sogou.iot.arch.anr.BlockInfo

interface IBlockInterceptor {
    fun onBlock(info: BlockInfo)
}