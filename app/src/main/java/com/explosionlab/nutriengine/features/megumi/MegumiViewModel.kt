package com.explosionlab.nutriengine.features.megumi

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.core.data.repository.AuthRepository
import com.explosionlab.nutriengine.core.data.repository.HealthContextRepository
import com.explosionlab.nutriengine.core.model.Mensagem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MegumiViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val chatRepository = ChatRepository(authRepository)
    private val healthContextRepo = HealthContextRepository(application)

    private val _mensagens = MutableStateFlow<List<Mensagem>>(emptyList())
    val mensagens: StateFlow<List<Mensagem>> = _mensagens.asStateFlow()

    var carregando by mutableStateOf(false); private set
    var semConexao by mutableStateOf(false); private set

    val nomeUsuario: String get() = authRepository.carregarNome()

    init { carregarHistorico() }

    // Histórico das msg

    fun recarregar() = carregarHistorico()

    private fun carregarHistorico() {
        viewModelScope.launch {
            carregando = true
            try { _mensagens.value = chatRepository.carregarHistorico(limite = 50) }
            finally { carregando = false }
        }
    }

    fun enviarMensagem(texto: String) {
        val textoLimpo = texto.trim()
        if (textoLimpo.isEmpty()) return
        if (carregando) return

        if (!isConectado()) { semConexao = true; return }
        semConexao = false

        _mensagens.value += Mensagem(textoLimpo, ehUsuario = true)
        
        carregando = true

        viewModelScope.launch {
            try {
                val historicoJson = healthContextRepo.montarHistoricoSaudeJson()
                val resposta = chatRepository.enviarMensagem(textoLimpo, historicoJson)
                _mensagens.value += Mensagem(resposta, ehUsuario = false)
            } catch (e: Exception) {
                Log.e("MegumiViewModel", "Erro ao enviar mensagem: ${e.message}")
            } finally {
                carregando = false
            }
        }
    }

    fun descartarAvisoSemConexao() { semConexao = false }

    // Conected

    private fun isConectado(): Boolean {
        val cm = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
