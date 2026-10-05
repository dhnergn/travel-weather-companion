package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.SavedAttractionEntity
import com.example.data.database.TravelDatabase
import com.example.data.database.TravelQueryEntity
import com.example.data.model.Attraction
import com.example.data.repository.TravelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AttractionsUiState {
    object Idle : AttractionsUiState
    object Loading : AttractionsUiState
    data class Success(val attractions: List<Attraction>) : AttractionsUiState
    data class Error(val message: String) : AttractionsUiState
}

class TravelViewModel(private val repository: TravelRepository) : ViewModel() {

    // --- Inputs State ---
    private val _locationInput = MutableStateFlow("Paris")
    val locationInput: StateFlow<String> = _locationInput.asStateFlow()

    private val _selectedWeatherCondition = MutableStateFlow("Sunny")
    val selectedWeatherCondition: StateFlow<String> = _selectedWeatherCondition.asStateFlow()

    private val _selectedTemperature = MutableStateFlow("72°F / 22°C")
    val selectedTemperature: StateFlow<String> = _selectedTemperature.asStateFlow()

    private val _selectedInterests = MutableStateFlow(setOf("Art", "Historical"))
    val selectedInterests: StateFlow<Set<String>> = _selectedInterests.asStateFlow()

    // --- Suggestions UI State ---
    private val _attractionsState = MutableStateFlow<AttractionsUiState>(AttractionsUiState.Idle)
    val attractionsState: StateFlow<AttractionsUiState> = _attractionsState.asStateFlow()

    // --- History & Saved flows from database ---
    val searchHistory: StateFlow<List<TravelQueryEntity>> = repository.allQueries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedAttractions: StateFlow<List<SavedAttractionEntity>> = repository.allSavedAttractions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- User Actions ---

    fun updateLocationInput(location: String) {
        _locationInput.value = location
    }

    fun updateWeather(condition: String, temp: String) {
        _selectedWeatherCondition.value = condition
        _selectedTemperature.value = temp
    }

    fun toggleInterest(interest: String) {
        val current = _selectedInterests.value.toMutableSet()
        if (current.contains(interest)) {
            current.remove(interest)
        } else {
            current.add(interest)
        }
        _selectedInterests.value = current
    }

    fun getSuggestions() {
        val location = _locationInput.value.trim()
        if (location.isEmpty()) {
            _attractionsState.value = AttractionsUiState.Error("Please enter a location.")
            return
        }

        viewModelScope.launch {
            _attractionsState.value = AttractionsUiState.Loading
            try {
                val results = repository.fetchAttractions(
                    location = location,
                    weatherCondition = _selectedWeatherCondition.value,
                    temperature = _selectedTemperature.value,
                    interests = _selectedInterests.value.toList()
                )
                _attractionsState.value = AttractionsUiState.Success(results)
            } catch (e: Exception) {
                _attractionsState.value = AttractionsUiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    /**
     * Loads a past query back into the active search screen.
     */
    fun loadPastQuery(query: TravelQueryEntity, parsedAttractions: List<Attraction>) {
        _locationInput.value = query.location
        _selectedWeatherCondition.value = query.weatherCondition
        _selectedTemperature.value = query.temperature
        
        val parsedInterests = query.interests.split(",").map { it.trim() }.toSet()
        _selectedInterests.value = parsedInterests
        _attractionsState.value = AttractionsUiState.Success(parsedAttractions)
    }

    fun toggleSaveAttraction(attraction: Attraction, location: String, weather: String) {
        viewModelScope.launch {
            val isCurrentlySaved = savedAttractions.value.any { 
                it.name.equals(attraction.name, ignoreCase = true) && it.location.equals(location, ignoreCase = true)
            }

            if (isCurrentlySaved) {
                repository.unsaveAttraction(attraction.name, location)
            } else {
                repository.saveAttraction(
                    SavedAttractionEntity(
                        name = attraction.name,
                        description = attraction.description,
                        category = attraction.category,
                        rating = attraction.rating,
                        whyForWeather = attraction.whyForWeather,
                        recommendedTime = attraction.recommendedTime,
                        isIndoor = attraction.isIndoor,
                        location = location,
                        weatherAtSaved = weather
                    )
                )
            }
        }
    }

    fun toggleSaveAttractionEntity(entity: SavedAttractionEntity) {
        viewModelScope.launch {
            repository.unsaveAttraction(entity.name, entity.location)
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteQueryById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

// --- ViewModel Factory ---

class TravelViewModelFactory(private val repository: TravelRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TravelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TravelViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
