package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "travel_queries")
data class TravelQueryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val location: String,
    val weatherCondition: String,
    val temperature: String,
    val interests: String,
    val suggestionsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_attractions")
data class SavedAttractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val category: String,
    val rating: Double,
    val whyForWeather: String,
    val recommendedTime: String,
    val isIndoor: Boolean,
    val location: String,
    val weatherAtSaved: String,
    val timestamp: Long = System.currentTimeMillis()
)
