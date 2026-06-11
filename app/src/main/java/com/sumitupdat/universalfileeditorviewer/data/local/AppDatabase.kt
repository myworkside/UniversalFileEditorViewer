package com.sumitupdat.universalfileeditorviewer.data.local

import android.content.Context
import androidx.room.*
import com.sumitupdat.universalfileeditorviewer.data.model.FileCategory

class Converters {
    @TypeConverter
    fun fromFileCategory(value: FileCategory): String {
        return value.name
    }

    @TypeConverter
    fun toFileCategory(value: String): FileCategory {
        return FileCategory.valueOf(value)
    }
}

@Database(entities = [FavoriteFile::class, RecentFile::class, IndexedFile::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "file_database"
                )
                .fallbackToDestructiveMigration() // Simplified for development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
