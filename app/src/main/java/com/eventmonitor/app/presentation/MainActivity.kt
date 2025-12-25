package com.eventmonitor.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.eventmonitor.app.presentation.navigation.NavGraph
import com.eventmonitor.core.common.theme.HeadCounterTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HeadCounterTheme {
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }

                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        darkIcons = !useDarkIcons
                    )
                    systemUiController.setNavigationBarColor(
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        darkIcons = !useDarkIcons
                    )
                }

                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        startDestination = com.eventmonitor.app.presentation.navigation.Screen.VenueList.route,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun androidx.compose.ui.graphics.Color.luminance(): Float {
        return (0.299f * red + 0.587f * green + 0.114f * blue)
    }
}
