package com.example.bidcarftagent.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SrsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SrsEntity)

    @Query("SELECT * FROM srs_table WHERE filename = :name LIMIT 1")
    suspend fun getByFilename(name: String): SrsEntity?

    @Query("SELECT * FROM srs_table ORDER BY ts DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SrsEntity>

    @Query("SELECT * FROM srs_table ORDER BY ts DESC")
    suspend fun getAll(): List<SrsEntity>

    @Query("DELETE FROM srs_table WHERE filename = :name")
    suspend fun deleteByFilename(name: String)
}

