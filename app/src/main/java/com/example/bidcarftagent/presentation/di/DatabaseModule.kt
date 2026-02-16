package com.example.bidcarftagent.presentation.di

import android.content.Context
import androidx.room.Room
import com.example.bidcarftagent.data.local.db.AppDatabase
import com.example.bidcarftagent.data.local.db.SrsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        return Room.databaseBuilder(ctx, AppDatabase::class.java, "bidcraft_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSrsDao(db: AppDatabase): SrsDao = db.srsDao()
}

