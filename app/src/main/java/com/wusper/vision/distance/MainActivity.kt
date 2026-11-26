package com.wusper.vision.distance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wusper.vision.distance.ui.screens.RangefinderScreen
import com.wusper.vision.distance.ui.theme.MonoRangeTheme
import com.wusper.vision.distance.viewmodel.RangefinderViewModel

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
