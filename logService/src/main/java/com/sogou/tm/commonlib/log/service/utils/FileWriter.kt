package com.sogou.tm.commonlib.log.service.utils

import java.io.File

object FileWriter {

     fun saveLogWithPath(log: String, path: String) {
        var file = File(path)
        if(file != null && !file.parentFile.exists()) file.parentFile.mkdirs()
        if(!file.exists()){
            file.createNewFile();
        }
        file.appendText("=======================")
        file.appendText(log)
    }
}