package com.sumitupdat.universalfileeditorviewer.di

import android.content.Context
import com.sumitupdat.universalfileeditorviewer.data.local.AppDatabase
import com.sumitupdat.universalfileeditorviewer.data.local.FileDao
import com.sumitupdat.universalfileeditorviewer.data.local.VaultFileDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideFileDao(database: AppDatabase): FileDao {
        return database.fileDao()
    }

    @Provides
    fun provideVaultDao(database: AppDatabase): VaultFileDao {
        return database.vaultDao()
    }
}
