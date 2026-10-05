package com.example.data.repository

import com.example.BuildConfig
import com.example.data.database.SavedAttractionEntity
import com.example.data.database.TravelDao
import com.example.data.database.TravelQueryEntity
import com.example.data.model.Attraction
import com.example.data.network.GeminiContent
import com.example.data.network.GeminiGenerationConfig
import com.example.data.network.GeminiPart
import com.example.data.network.GeminiRequest
import com.example.data.network.GeminiRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TravelRepository(private val travelDao: TravelDao) {

    val allQueries: Flow<List<TravelQueryEntity>> = travelDao.getAllQueries()
    val allSavedAttractions: Flow<List<SavedAttractionEntity>> = travelDao.getAllSavedAttractions()

    suspend fun insertQuery(query: TravelQueryEntity) = withContext(Dispatchers.IO) {
        travelDao.insertQuery(query)
    }

    suspend fun deleteQueryById(id: Int) = withContext(Dispatchers.IO) {
        travelDao.deleteQueryById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        travelDao.clearHistory()
    }

    suspend fun saveAttraction(attraction: SavedAttractionEntity) = withContext(Dispatchers.IO) {
        travelDao.saveAttraction(attraction)
    }

    suspend fun unsaveAttraction(name: String, location: String) = withContext(Dispatchers.IO) {
        travelDao.unsaveAttraction(name, location)
    }

    fun isAttractionSavedFlow(name: String, location: String): Flow<Boolean> {
        return travelDao.isAttractionSavedFlow(name, location)
    }

    suspend fun isAttractionSaved(name: String, location: String): Boolean = withContext(Dispatchers.IO) {
        travelDao.isAttractionSaved(name, location)
    }

    /**
     * Calls Gemini API to get attractions based on inputs, parses them, and caches the query in history.
     */
    suspend fun fetchAttractions(
        location: String,
        weatherCondition: String,
        temperature: String,
        interests: List<String>
    ): List<Attraction> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured. Please add GEMINI_API_KEY to secrets.")
        }

        val interestsStr = if (interests.isEmpty()) "General Sightseeing" else interests.joinToString(", ")

        val prompt = """
            Suggest 5 distinct, real local attractions or activities in "$location" for a traveler interested in: $interestsStr.
            The current weather is $temperature and the condition is "$weatherCondition".
            
            IMPORTANT:
            - You MUST suggest attractions that are highly suitable or enjoyable in "$weatherCondition" weather. For instance, if "$weatherCondition" is Rainy, Snowy, or Stormy, prefer indoor attractions (museums, indoor galleries, historic houses, food markets) and describe this in "whyForWeather". If Sunny, Warm, or Clear, suggest scenic viewpoints, parks, walking tours, or outdoor gardens.
            - Provide highly localized, actual attractions in "$location".
            
            Format your response strictly as a JSON array of objects, with these exact keys:
            "name" (string): The attraction name.
            "description" (string): A short, engaging description.
            "category" (string): The primary category (e.g., Historical, Nature, Art, Food, Adventure, Shopping, Family).
            "rating" (double): A rating between 3.5 and 5.0.
            "whyForWeather" (string): A brief sentence explaining why this is perfect for "$weatherCondition" weather.
            "recommendedTime" (string): Recommended time to visit (e.g., Morning, Afternoon, Evening).
            "isIndoor" (boolean): True if this is an indoor venue/activity, false if outdoor.
            
            Return ONLY raw JSON, do not wrap in markdown ```json or any other text.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7f
            )
        )

        val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
        val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty response from Gemini API")

        val attractions = GeminiRetrofitClient.parseAttractionsJson(jsonText)
            ?: throw IllegalStateException("Failed to parse Gemini attractions JSON response")

        // Store this successful search query in the local history database
        val queryEntity = TravelQueryEntity(
            location = location,
            weatherCondition = weatherCondition,
            temperature = temperature,
            interests = interestsStr,
            suggestionsJson = jsonText
        )
        travelDao.insertQuery(queryEntity)

        attractions
    }
}
