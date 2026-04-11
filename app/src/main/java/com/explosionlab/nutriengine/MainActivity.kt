package com.explosionlab.nutriengine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.explosionlab.nutriengine.navigation.NutriNavGraph
import com.explosionlab.nutriengine.ui.theme.NutriEngineTheme
import com.explosionlab.nutriengine.viewmodel.AppViewModel
import com.explosionlab.nutriengine.viewmodel.HomeViewModel
import com.explosionlab.nutriengine.viewmodel.MegumiViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriEngineTheme {
                val appViewModel:    AppViewModel    = viewModel()
                val homeViewModel:   HomeViewModel   = viewModel()
                val megumiViewModel: MegumiViewModel = viewModel()

                NutriNavGraph(
                    appViewModel    = appViewModel,
                    homeViewModel   = homeViewModel,
                    megumiViewModel = megumiViewModel,
                )
            }
        }
    }
}
