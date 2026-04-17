package com.eventmonitor.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.compose.rememberNavController
import com.eventmonitor.app.presentation.navigation.NavGraph
import com.eventmonitor.app.presentation.navigation.Screen
import com.eventmonitor.core.common.theme.HeadCounterTheme
import com.eventmonitor.core.common.theme.InkDark
import com.eventmonitor.core.common.theme.Paper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(InkDark.toArgb())
                    } else {
                        SystemBarStyle.light(Paper.toArgb(), InkDark.toArgb())
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(InkDark.toArgb())
                    } else {
                        SystemBarStyle.light(Paper.toArgb(), InkDark.toArgb())
                    },
                )
                onDispose {}
            }
            HeadCounterTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    startDestination = Screen.VenueList.route,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
