package com.explosionlab.nutriengine.viewmodel

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {


    private val authRepository = AuthRepository(application)


    var isLoading  by mutableStateOf(false)  ; private set
    var errorMsg   by mutableStateOf("")     ; private set


    fun fazerLogin(activity: Activity, onSucesso: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMsg  = ""

            val resultado = authRepository.fazerLoginGoogle(activity)

            if (resultado.sucesso) {
                onSucesso()
            } else {
                errorMsg = resultado.mensagem
            }

            isLoading = false
        }
    }
}