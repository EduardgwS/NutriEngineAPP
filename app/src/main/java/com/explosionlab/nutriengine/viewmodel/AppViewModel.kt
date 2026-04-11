package com.explosionlab.nutriengine.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.explosionlab.nutriengine.repository.AuthRepository
import com.explosionlab.nutriengine.repository.PerfilRepository
import com.explosionlab.nutriengine.screens.NutriTab


class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo   = AuthRepository(application)
    private val perfilRepo = PerfilRepository(application)


    val telaInicial: String = when {
        !authRepo.estaLogado()       -> "login"
        !perfilRepo.perfilCompleto() -> "hc_intro"
        else                         -> NutriTab.INICIO.rota
    }

    fun perfilCompleto(): Boolean = perfilRepo.perfilCompleto()


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
