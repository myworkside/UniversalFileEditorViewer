package com.sumitupdat.universalfileeditorviewer.domain.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import com.sumitupdat.universalfileeditorviewer.data.local.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PreferencesRepository"

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<UserPreferences>,
    @ApplicationContext private val context: Context
) {
    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            Log.e(TAG, "Error reading preferences", exception)
            if (exception is IOException) {
                emit(UserPreferences())
            } else {
                throw exception
            }
        }

    suspend fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        try {
            dataStore.updateData { currentPrefs ->
                update(currentPrefs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating preferences", e)
        }
    }
}
