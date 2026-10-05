package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Attraction(
    val name: String,
    val description: String,
    val category: String,
    val rating: Double,
    val whyForWeather: String,
    val recommendedTime: String,
    val isIndoor: Boolean
)
