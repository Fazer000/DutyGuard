package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "task_presets")
data class TaskPreset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val intervalMinutes: Int,
    val disableInFreeShift: Boolean = false,
    val isSpecificTime: Boolean = false,
    val specificHour: Int = 0,
    val specificMinute: Int = 0
)

@Dao
interface TaskPresetDao {
    @Query("SELECT * FROM task_presets ORDER BY id ASC")
    fun getAllPresetsFlow(): Flow<List<TaskPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: TaskPreset): Long

    @Delete
    suspend fun deletePreset(preset: TaskPreset)
}
