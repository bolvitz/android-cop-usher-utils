package com.copheadcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.copheadcounter.ui.CountingScreen
import com.copheadcounter.ui.theme.CopHeadCounterTheme
import com.copheadcounter.viewmodel.CounterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CopHeadCounterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: CounterViewModel = viewModel()

                    CountingScreen(
                        counterItems = viewModel.counterItems,
                        onIncrementCount = { id -> viewModel.incrementCount(id) },
                        onDecrementCount = { id -> viewModel.decrementCount(id) },
                        onAddNewCounter = { name -> viewModel.addCounter(name) }
                    )
                }
            }
        }
    }
}
