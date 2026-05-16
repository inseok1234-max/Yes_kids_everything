package com.ois.stickymemo.data

import android.content.Context
import androidx.room.*
import com.ois.stickymemo.BuildConfig
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@TypeConverters(Converters::class)
@Database(
    entities = [Memo::class, Restaurant::class],
    version = 8,
    exportSchema = true
)
abstract class MemoDatabase : RoomDatabase() {

    abstract fun memoDao(): MemoDao
    abstract fun restaurantDao(): RestaurantDao

    companion object {
        @Volatile
        private var INSTANCE: MemoDatabase? = null

        fun getDatabase(context: Context): MemoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoDatabase::class.java,
                    "sticky_memo_database"
                ).apply {
                    addMigrations(MIGRATION_7_8)
                    if (BuildConfig.DEBUG) {
                        // Development-only escape hatch for pre-v7 local databases.
                        // Never enable destructive migration for release builds.
                        @Suppress("DEPRECATION")
                        fallbackToDestructiveMigration()
                    }
                }.build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema intentionally unchanged. This version records the first
                // non-destructive migration boundary after the rebuild stabilization.
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromMemoType(value: MemoType): String = value.name

    @TypeConverter
    fun toMemoType(value: String): MemoType = MemoType.valueOf(value)
}
