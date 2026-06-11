package com.sumitupdat.universalfileeditorviewer.data.local

import androidx.room.*
import com.sumitupdat.universalfileeditorviewer.data.model.FileCategory
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

    // Indexing operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndex(files: List<IndexedFile>)

    @Query("DELETE FROM file_index")
    suspend fun clearIndex()

    @Query("SELECT * FROM file_index WHERE name LIKE '%' || :query || '%' OR extension LIKE '%' || :query || '%'")
    fun searchFiles(query: String): Flow<List<IndexedFile>>

    @Query("SELECT * FROM file_index WHERE category = :category")
    fun getFilesByCategory(category: FileCategory): Flow<List<IndexedFile>>

    @Query("SELECT COUNT(*) FROM file_index WHERE category = :category")
    fun getCountByCategory(category: FileCategory): Flow<Int>
}
