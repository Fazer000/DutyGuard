package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "task_occurrences",
    primaryKeys = ["dutyId", "presetId", "targetTimeEpochMillis"]
)
data class TaskOccurrence(
    val dutyId: Long,
    val presetId: Int,
    val title: String,
    val targetTimeEpochMillis: Long,
    val isCompleted: Boolean = false,
    val isVibrated: Boolean = false,
    val disableInFreeShift: Boolean = false
)

@Dao
interface TaskOccurrenceDao {
    @Query("SELECT * FROM task_occurrences WHERE dutyId = :dutyId ORDER BY targetTimeEpochMillis ASC")
    fun getOccurrencesForDutyFlow(dutyId: Long): Flow<List<TaskOccurrence>>

    @Query("SELECT * FROM task_occurrences WHERE dutyId = :dutyId ORDER BY targetTimeEpochMillis ASC")
    suspend fun getOccurrencesForDuty(dutyId: Long): List<TaskOccurrence>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrences(occurrences: List<TaskOccurrence>)

    @Query("UPDATE task_occurrences SET isCompleted = :isCompleted WHERE dutyId = :dutyId AND presetId = :presetId AND targetTimeEpochMillis = :targetTime")
    suspend fun updateCompletion(dutyId: Long, presetId: Int, targetTime: Long, isCompleted: Boolean)

    @Query("UPDATE task_occurrences SET isVibrated = :isVibrated WHERE dutyId = :dutyId AND presetId = :presetId AND targetTimeEpochMillis = :targetTime")
    suspend fun updateVibrated(dutyId: Long, presetId: Int, targetTime: Long, isVibrated: Boolean)

    @Query("DELETE FROM task_occurrences WHERE dutyId = :dutyId")
    suspend fun deleteOccurrencesForDuty(dutyId: Long)

    @Query("DELETE FROM task_occurrences WHERE presetId = :presetId")
    suspend fun deleteOccurrencesByPreset(presetId: Int)
}
