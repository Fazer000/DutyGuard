package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "duties")
data class Duty(
    @PrimaryKey val id: Long, // Use start epoch timestamp as ID
    val title: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val isActive: Boolean = false,
    val guardOneName: String = "Дежурный 1",
    val guardTwoName: String = "Дежурный 2",
    val shiftCustomIntervals: String = "0-2,4-6,8-10,12-14,16-18,20-22", // Comma-separated intervals start-end for Guard 1
    val deviceOwnerIndex: Int = 1, // 1 = Guard 1, 2 = Guard 2
    val guardNamesStr: String = "Дежурный 1,Дежурный 2",
    val hourAssignmentsStr: String = "", // 24 comma-separated values from 0 to N-1
    val dailyPassword: String = "" // Format "Слово - Слово" (e.g. "Рязань - Пенза")
) {
    fun getGuardNames(): List<String> {
        if (guardNamesStr.isNotBlank()) {
            return guardNamesStr.split(",")
        }
        return listOf(guardOneName, guardTwoName)
    }

    fun getHourAssignments(): List<Int> {
        if (hourAssignmentsStr.isNotBlank()) {
            val list = hourAssignmentsStr.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (list.size == 24) return list
        }
        // Fallback to shiftCustomIntervals
        val g1Intervals = ShiftSegment.parseIntervals(shiftCustomIntervals)
        val arr = IntArray(24) { 1 } // Default to Guard 2 (index 1)
        for (interval in g1Intervals) {
            for (h in interval.start until interval.end) {
                if (h in 0..23) {
                    arr[h] = 0 // Guard 1 (index 0)
                }
            }
        }
        return arr.toList()
    }
}

@Dao
interface DutyDao {
    @Query("SELECT * FROM duties ORDER BY startEpochMillis DESC")
    fun getAllDutiesFlow(): Flow<List<Duty>>

    @Query("SELECT * FROM duties WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveDuty(): Duty?

    @Query("SELECT * FROM duties WHERE isActive = 1 LIMIT 1")
    fun getActiveDutyFlow(): Flow<Duty?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuty(duty: Duty)

    @Update
    suspend fun updateDuty(duty: Duty)

    @Query("UPDATE duties SET dailyPassword = :password WHERE id = :dutyId")
    suspend fun updateDailyPassword(dutyId: Long, password: String)

    @Query("UPDATE duties SET isActive = 0 WHERE id != :activeId")
    suspend fun deactivateAllBut(activeId: Long)

    @Query("DELETE FROM duties WHERE id = :id")
    suspend fun deleteDutyById(id: Long)
}
