package com.sumitupdat.universalfileeditorviewer.di

import android.content.Context
import com.sumitupdat.universalfileeditorviewer.data.local.FileDao
import com.sumitupdat.universalfileeditorviewer.domain.repository.ArchiveRepository
import com.sumitupdat.universalfileeditorviewer.domain.repository.FileRepository
import com.sumitupdat.universalfileeditorviewer.domain.repository.PresentationRepository
import com.sumitupdat.universalfileeditorviewer.domain.repository.SpreadsheetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFileRepository(fileDao: FileDao, @ApplicationContext context: Context): FileRepository {
        return FileRepository(fileDao, context)
    }

    @Provides
    @Singleton
    fun provideArchiveRepository(@ApplicationContext context: Context): ArchiveRepository {
        return ArchiveRepository(context)
    }

    @Provides
    @Singleton
    fun provideSpreadsheetRepository(@ApplicationContext context: Context): SpreadsheetRepository {
        return SpreadsheetRepository(context)
    }

    @Provides
    @Singleton
    fun providePresentationRepository(@ApplicationContext context: Context): PresentationRepository {
        return PresentationRepository(context)
    }
}
