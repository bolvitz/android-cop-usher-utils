package com.eventmonitor.app.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.eventmonitor.app.presentation.navigation.NavGraph
import com.eventmonitor.app.presentation.navigation.Screen
import com.eventmonitor.core.common.theme.HeadCounterTheme
import com.eventmonitor.core.common.theme.InkDark
import com.eventmonitor.core.common.theme.LocalThemeMode
import com.eventmonitor.core.common.theme.Paper
import com.eventmonitor.core.common.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.drop

private const val THEME_PREFS = "ui_prefs"
private const val THEME_KEY_DARK = "is_dark"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val prefs = remember {
                context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
            }
            val themeMode = remember {
                ThemeMode(initial = prefs.getBoolean(THEME_KEY_DARK, true))
            }
            // Persist the toggle whenever it changes.
            LaunchedEffect(themeMode) {
                snapshotFlow { themeMode.isDark }
                    .drop(1)
                    .collect { prefs.edit().putBoolean(THEME_KEY_DARK, it).apply() }
            }

            val darkTheme = themeMode.isDark
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
            CompositionLocalProvider(LocalThemeMode provides themeMode) {
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
}
