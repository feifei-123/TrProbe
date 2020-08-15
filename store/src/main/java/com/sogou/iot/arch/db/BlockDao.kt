package com.sogou.iot.arch.db


import androidx.room.*

@Dao
interface BlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlockInfo(block : BlockWrapper):Long

    @Delete
    fun deleteBlockInfo(block: BlockWrapper):Int

    @Query("SELECT * FROM block")
    fun getAllBlocks():List<BlockWrapper>

    @Query("SELECT * FROM block WHERE isReported = 0 ")
    fun getAllBlocksUnReported():List<BlockWrapper>

    @Update
    fun  updateBlockInfo(block: BlockWrapper):Int


    @Query("SELECT * FROM block WHERE timeStamp < :stamp")
    fun getBlockOverDue(stamp:Long):List<BlockWrapper>

    @Query("SELECT * FROM block  ORDER BY timeStamp DESC limit 1 ")
    fun getLastBlock():BlockWrapper?
}