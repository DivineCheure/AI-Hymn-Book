package com.example.aihymnbook

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HymnEntity::class], version = 1, exportSchema = false)
abstract class HymnDatabase : RoomDatabase() {

    abstract fun hymnDao(): HymnDao

    companion object {
        @Volatile
        private var INSTANCE: HymnDatabase? = null

        fun getDatabase(context: Context): HymnDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HymnDatabase::class.java,
                    "hymns.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}