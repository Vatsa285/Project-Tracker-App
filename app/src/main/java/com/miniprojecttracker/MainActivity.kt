package com.miniprojecttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.miniprojecttracker.ui.MainAppContainer
import com.miniprojecttracker.ui.navigation.NavGraph
import com.miniprojecttracker.ui.navigation.Screen
import com.miniprojecttracker.util.SeedDataHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var seedDataHelper: SeedDataHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Seed database on first launch (for demonstration purposes)
        lifecycleScope.launch {
            seedDataHelper.seedDatabaseIfEmpty()
        }
        
        setContent {
            val navController = rememberNavController()
            MainAppContainer {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(
                        navController = navController,
                        startDestination = Screen.Login.route
                    )
                }
            }
        }
    }
}
