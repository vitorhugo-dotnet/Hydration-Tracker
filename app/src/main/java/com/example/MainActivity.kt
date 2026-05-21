package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.WaterDatabase
import com.example.data.WaterRepository
import com.example.ui.WaterTrackerApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.WaterTrackerViewModel
import com.example.viewmodel.WaterTrackerViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core room database initialization
        val db = WaterDatabase.getDatabase(applicationContext)
        val repository = WaterRepository(db.waterDao())
        
        // Factory creation for MVVM state inject patterns
        val factory = WaterTrackerViewModelFactory(repository, applicationContext)
        val viewModel = ViewModelProvider(this, factory)[WaterTrackerViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WaterTrackerApp(viewModel = viewModel)
                }
            }
        }
    }
}
