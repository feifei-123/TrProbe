package com.sogou.iot.arch.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = arrayOf(BlockWrapper::class, CrashWrapper::class),version = 1)
@TypeConverters(Convertors::class)
abstract class ProbeDatabase : RoomDatabase() {

    companion object {
        @Volatile private var INSTANCE: ProbeDatabase? = null
        fun getInstance(context: Context): ProbeDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(context.applicationContext,
                        ProbeDatabase::class.java, "probe.db")
                        .build()
    }

    abstract fun blockDao():BlockDao;
    abstract fun crashDao():CrashDao;
}