package com.sogou.iot.arch.db


import androidx.room.TypeConverter


class Convertors {

    @TypeConverter
    fun stringListToString(strings: List<String>?): String? {
        return strings?.joinToString("/")
    }

    @TypeConverter
    fun stringToStringList(data: String?): List<String> {
        return data?.split("/") ?: ArrayList()
    }
}