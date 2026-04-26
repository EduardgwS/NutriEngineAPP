package com.explosionlab.nutriengine.features.settings

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.core.notifications.NotificationScheduler
import com.explosionlab.nutriengine.features.health.HealthConnectRepository
import kotlinx.coroutines.launch

data class ConfiguracoesUiState(
    val notificacoesAtivas:  Boolean = false,
)

class ConfiguracoesViewModel(application: Application) : AndroidViewModel(application) {

    private val healthRepo = HealthConnectRepository(application)
    private val prefs      = application.getSharedPreferences("nutriengine_prefs", Context.MODE_PRIVATE)

    var state by mutableStateOf(ConfiguracoesUiState()); private set

    init {
        carregarEstadoInicial()
    }

    private fun carregarEstadoInicial() {
        viewModelScope.launch {
            val temPermissaoSistemica = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            val ativadasNoApp = prefs.getBoolean("notificacoes_ativas", true)

            state = state.copy(
                notificacoesAtivas = temPermissaoSistemica && ativadasNoApp,
            )
        }
    }

    // ── Notificações ──────────────────────────────────────────────────────────

    fun onResultadoPermissaoNotificacao(concedida: Boolean) {
        state = state.copy(notificacoesAtivas = concedida)
        prefs.edit().putBoolean("notificacoes_ativas", concedida).apply()

        val app = getApplication<Application>()
        if (concedida) NotificationScheduler.ativar(app)
        else           NotificationScheduler.desativar(app)
    }

    fun desativarNotificacoes() {
        prefs.edit().putBoolean("notificacoes_ativas", false).apply()
        state = state.copy(notificacoesAtivas = false)
        NotificationScheduler.desativar(getApplication())
    }
}