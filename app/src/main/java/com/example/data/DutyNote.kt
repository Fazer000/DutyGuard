package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "duty_notes")
data class DutyNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dutyId: Long, // 0 for setup drafts or duty.id
    val text: String,
    val isCompleted: Boolean = false,
    val isImportant: Boolean = false,
    val timeLabel: String = "", // e.g. "14:00" or empty
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface DutyNoteDao {
    @Query("SELECT * FROM duty_notes WHERE dutyId = :dutyId ORDER BY isCompleted ASC, isImportant DESC, id ASC")
    fun getNotesForDutyFlow(dutyId: Long): Flow<List<DutyNote>>

    @Query("SELECT * FROM duty_notes WHERE dutyId = :dutyId ORDER BY id ASC")
    suspend fun getNotesForDuty(dutyId: Long): List<DutyNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: DutyNote): Long

    @Update
    suspend fun updateNote(note: DutyNote)

    @Query("UPDATE duty_notes SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateNoteCompletion(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM duty_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM duty_notes WHERE dutyId = :dutyId")
    suspend fun deleteNotesForDuty(dutyId: Long)
}
