package com.sumitupdat.universalfileeditorviewer.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.sumitupdat.universalfileeditorviewer.data.local.PreferencesSerializer
import com.sumitupdat.universalfileeditorviewer.data.local.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<UserPreferences> {
        return DataStoreFactory.create(
            serializer = PreferencesSerializer,
            produceFile = { context.dataStoreFile("user_prefs.json") }
        )
    }
}
