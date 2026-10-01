package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Duty::class, TaskPreset::class, TaskOccurrence::class, TerritoryPerson::class, DutyNote::class],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dutyDao(): DutyDao
    abstract fun taskPresetDao(): TaskPresetDao
    abstract fun taskOccurrenceDao(): TaskOccurrenceDao
    abstract fun territoryPersonDao(): TerritoryPersonDao
    abstract fun dutyNoteDao(): DutyNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "military_duty_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
