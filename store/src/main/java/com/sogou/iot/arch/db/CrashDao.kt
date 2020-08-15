package com.sogou.iot.arch.db

import androidx.room.*

@Dao
interface CrashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCrashInfo(crash : CrashWrapper):Long

    @Delete
    fun deleteCrashInfo(crash: CrashWrapper):Int

    @Query("SELECT * FROM crash")
    fun getAllCrash():List<CrashWrapper>

    @Query("SELECT * FROM crash WHERE isReported = 0 ")
    fun getAllCrashsUnReported():List<CrashWrapper>

    @Update
    fun  updateCrashInfo(crash: CrashWrapper):Int

    @Query("SELECT * FROM crash WHERE timeStamp < :stamp")
    fun getCrashOverDue(stamp:Long):List<CrashWrapper>


    @Query("SELECT * FROM crash  ORDER BY timeStamp DESC limit 1 ")
    fun getLastCrash():CrashWrapper?

}