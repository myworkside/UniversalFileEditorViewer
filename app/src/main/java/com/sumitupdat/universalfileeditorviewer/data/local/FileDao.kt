package com.sumitupdat.universalfileeditorviewer.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteFile)

    @Delete
    suspend fun removeFavorite(favorite: FavoriteFile)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE path = :path)")
    suspend fun isFavorite(path: String): Boolean

    @Query("SELECT * FROM recent_files ORDER BY openedAt DESC LIMIT 50")
    fun getRecentFiles(): Flow<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecentFile(recent: RecentFile)

    @Query("DELETE FROM recent_files WHERE path = :path")
    suspend fun removeRecentFile(path: String)
}
