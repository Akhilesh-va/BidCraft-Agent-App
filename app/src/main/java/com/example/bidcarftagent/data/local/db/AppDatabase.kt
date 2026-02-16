package com.example.bidcarftagent.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SrsEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun srsDao(): SrsDao
}

