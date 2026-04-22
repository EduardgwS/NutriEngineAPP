package com.explosionlab.nutriengine.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.explosionlab.nutriengine.repository.AuthRepository
import com.explosionlab.nutriengine.repository.PerfilRepository
import com.explosionlab.nutriengine.screens.NutriTab

enum class TemaApp { CLARO, ESCURO, SISTEMA }

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo   = AuthRepository(application)
    private val perfilRepo = PerfilRepository(application)
    private val prefs      = application.getSharedPreferences("nutriengine_prefs", Context.MODE_PRIVATE)

    // ── Tema ──────────────────────────────────────────────────────────────────

    var tema by mutableStateOf(
        TemaApp.valueOf(prefs.getString("tema_app", TemaApp.SISTEMA.name) ?: TemaApp.SISTEMA.name)
    )
        private set

    fun atualizarTema(novoTema: TemaApp) {
        tema = novoTema
        prefs.edit().putString("tema_app", novoTema.name).apply()
    }

    // ── Roteamento inicial ────────────────────────────────────────────────────

    val telaInicial: String get() = when {
        !authRepo.estaLogado()       -> "login"
        !perfilRepo.perfilCompleto() -> "hc_intro"
        else                         -> NutriTab.INICIO.rota
    }

    fun perfilCompleto(): Boolean = perfilRepo.perfilCompleto()

    // ── Sessão ────────────────────────────────────────────────────────────────

    fun logout() {
        authRepo.limparToken()
    }

    fun limparTodosDados() {
        getApplication<Application>()
            .getSharedPreferences("nutriengine_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()

        getApplication<Application>()
            .getSharedPreferences("nutricart_consumo", Context.MODE_PRIVATE)
            .edit().clear().apply()

        authRepo.limparToken()
    }
}
