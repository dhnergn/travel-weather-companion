package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.database.TravelDatabase
import com.example.data.repository.TravelRepository
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TravelViewModel
import com.example.ui.viewmodel.TravelViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize Room Database and Repository
        val database = TravelDatabase.getDatabase(this)
        val repository = TravelRepository(database.travelDao())
        
        // 2. Instantiate TravelViewModel using Factory
        val viewModel: TravelViewModel by viewModels {
            TravelViewModelFactory(repository)
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainDashboard(viewModel = viewModel)
                }
            }
        }
    }
}
