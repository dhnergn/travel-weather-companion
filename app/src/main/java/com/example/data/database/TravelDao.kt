package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDao {
    @Query("SELECT * FROM travel_queries ORDER BY timestamp DESC")
    fun getAllQueries(): Flow<List<TravelQueryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(query: TravelQueryEntity)

    @Query("DELETE FROM travel_queries WHERE id = :id")
    suspend fun deleteQueryById(id: Int)

    @Query("DELETE FROM travel_queries")
    suspend fun clearHistory()

    @Query("SELECT * FROM saved_attractions ORDER BY timestamp DESC")
    fun getAllSavedAttractions(): Flow<List<SavedAttractionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAttraction(attraction: SavedAttractionEntity)

    @Query("DELETE FROM saved_attractions WHERE name = :name AND location = :location")
    suspend fun unsaveAttraction(name: String, location: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_attractions WHERE name = :name AND location = :location)")
    fun isAttractionSavedFlow(name: String, location: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_attractions WHERE name = :name AND location = :location)")
    suspend fun isAttractionSaved(name: String, location: String): Boolean
}
