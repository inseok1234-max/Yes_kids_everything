package com.ois.stickymemo.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@TypeConverters(Converters::class)
@Database(
    entities = [Memo::class, Restaurant::class],
    version = 7,
    exportSchema = false
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
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
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