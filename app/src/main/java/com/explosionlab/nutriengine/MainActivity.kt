package com.explosionlab.nutriengine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import com.explosionlab.nutriengine.navigation.NutriNavGraph
import com.explosionlab.nutriengine.notifications.NutriNotificationManager
import com.explosionlab.nutriengine.notifications.NotificationScheduler
import com.explosionlab.nutriengine.ui.theme.NutriEngineTheme
import com.explosionlab.nutriengine.viewmodel.AppViewModel
import com.explosionlab.nutriengine.viewmodel.HomeViewModel
import com.explosionlab.nutriengine.viewmodel.MegumiViewModel
import com.explosionlab.nutriengine.viewmodel.MercadoViewModel
import com.explosionlab.nutriengine.viewmodel.TemaApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Cria os canais de notificação (seguro chamar múltiplas vezes)
        NutriNotificationManager.criarCanais(this)

        // Garante que as notificações estejam agendadas se houver permissão e se estiverem ativas no app
        val prefs = getSharedPreferences("nutriengine_prefs", Context.MODE_PRIVATE)
        val ativadasNoApp = prefs.getBoolean("notificacoes_ativas", true)

        if (ativadasNoApp && (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        ) {
            // Usamos REPLACE aqui para garantir que o agendamento seja renovado do zero ao abrir o app,
            // resolvendo problemas de workers que param de disparar sozinhos.
            NotificationScheduler.ativar(this, ExistingPeriodicWorkPolicy.REPLACE)
        }

        setContent {
            val appViewModel:     AppViewModel     = viewModel()
            val homeViewModel:    HomeViewModel    = viewModel()
            val megumiViewModel:  MegumiViewModel  = viewModel()
            val mercadoViewModel: MercadoViewModel = viewModel()

            val darkTheme = when (appViewModel.tema) {
                TemaApp.ESCURO  -> true
                TemaApp.CLARO   -> false
                TemaApp.SISTEMA -> isSystemInDarkTheme()
            }

            NutriEngineTheme(darkTheme = darkTheme) {
                NutriNavGraph(
                    appViewModel     = appViewModel,
                    homeViewModel    = homeViewModel,
                    megumiViewModel  = megumiViewModel,
                    mercadoViewModel = mercadoViewModel,
                )
            }
        }
    }
}