package com.monorange.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monorange.app.ui.screens.RangefinderScreen
import com.monorange.app.ui.theme.MonoRangeTheme
import com.monorange.app.viewmodel.RangefinderViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: RangefinderViewModel = viewModel()
            val state by vm.uiState.collectAsState()
            MonoRangeTheme(darkMono = state.darkMonoTheme) {
                RangefinderScreen(viewModel = vm)
            }
        }
    }
}
