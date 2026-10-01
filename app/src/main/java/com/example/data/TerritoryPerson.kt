package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "territory_personnel")
data class TerritoryPerson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rank: String,
    val callsign: String,
    val isPresent: Boolean = true, // true = На территории, false = Уехал
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Dao
interface TerritoryPersonDao {
    @Query("SELECT * FROM territory_personnel ORDER BY lastUpdatedMillis DESC")
    fun getAllPersonnelFlow(): Flow<List<TerritoryPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: TerritoryPerson)

    @Update
    suspend fun updatePerson(person: TerritoryPerson)

    @Query("DELETE FROM territory_personnel WHERE id = :id")
    suspend fun deletePersonById(id: Long)
}
