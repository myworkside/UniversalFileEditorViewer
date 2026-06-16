package com.sumitupdat.universalfileeditorviewer.data.local

import android.content.Context
import androidx.room.*
import com.sumitupdat.universalfileeditorviewer.data.model.FileCategory

class Converters {
    @TypeConverter
    fun fromFileCategory(value: FileCategory): String = value.name

    @TypeConverter
    fun toFileCategory(value: String): FileCategory = FileCategory.valueOf(value)

    @TypeConverter
    fun fromTransferStatus(value: TransferStatus): String = value.name

    @TypeConverter
    fun toTransferStatus(value: String): TransferStatus = TransferStatus.valueOf(value)

    @TypeConverter
    fun fromTransferType(value: TransferType): String = value.name

    @TypeConverter
    fun toTransferType(value: String): TransferType = TransferType.valueOf(value)

    @TypeConverter
    fun fromByteArray(value: ByteArray): String = android.util.Base64.encodeToString(value, android.util.Base64.NO_WRAP)

    @TypeConverter
    fun toByteArray(value: String): ByteArray = android.util.Base64.decode(value, android.util.Base64.NO_WRAP)
}

@Database(
    entities = [FavoriteFile::class, RecentFile::class, IndexedFile::class, TransferEntity::class, VaultFileEntity::class, VaultAuditLog::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun transferDao(): TransferDao
    abstract fun vaultDao(): VaultFileDao

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
