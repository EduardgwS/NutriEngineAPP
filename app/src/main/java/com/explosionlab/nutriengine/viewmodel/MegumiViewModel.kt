package com.explosionlab.nutriengine.viewmodel

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
import com.explosionlab.nutriengine.model.Mensagem
import com.explosionlab.nutriengine.repository.AuthRepository
import com.explosionlab.nutriengine.repository.ChatRepository
import com.explosionlab.nutriengine.repository.ConsumoRepository
import com.explosionlab.nutriengine.repository.HealthRepository
import com.explosionlab.nutriengine.repository.PerfilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class MegumiViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val chatRepository = ChatRepository(authRepository)
    private val healthRepo     = HealthRepository(application)
    private val perfilRepo     = PerfilRepository(application)
    private val consumoRepo    = ConsumoRepository(application)



    private val _mensagens = MutableStateFlow<List<Mensagem>>(emptyList())
    val mensagens: StateFlow<List<Mensagem>> = _mensagens.asStateFlow()

    var carregando   by mutableStateOf(false); private set


    var semConexao   by mutableStateOf(false); private set

    val nomeUsuario: String get() = authRepository.carregarNome()

    init {
        carregarHistorico()
    }



    fun recarregar() = carregarHistorico()

    private fun carregarHistorico() {
        viewModelScope.launch {
            carregando = true
            try {
                _mensagens.value = chatRepository.carregarHistorico(limite = 50)
            } finally {
                carregando = false
            }
        }
    }



    fun enviarMensagem(texto: String, imagemBytes: ByteArray? = null) {
        val textoLimpo = texto.trim()
        if (textoLimpo.isEmpty() && imagemBytes == null) return
        if (carregando) return


        if (!isConectado()) {
            semConexao = true
            return
        }
        semConexao = false

        _mensagens.value = _mensagens.value + Mensagem(textoLimpo, ehUsuario = true)
        carregando = true

        viewModelScope.launch {
            val historicoJson = montarHistoricoSaudeJson()
            val resposta      = chatRepository.enviarMensagem(textoLimpo, imagemBytes, historicoJson)
            _mensagens.value  = _mensagens.value + Mensagem(resposta, ehUsuario = false)
            carregando = false
        }
    }


    fun descartarAvisoSemConexao() { semConexao = false }



    private fun isConectado(): Boolean {
        val cm = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }



    private suspend fun montarHistoricoSaudeJson(): String {
        return try {
            val perfil = perfilRepo.carregarPerfil(nomeGoogleFallback = authRepository.carregarNome())

            val perfilJson = JSONObject().apply {
                if (perfil.peso   > 0) put("peso_kg",   perfil.peso)
                if (perfil.altura > 0) put("altura_m",  perfil.altura)
                if (perfil.idade  > 0) put("idade",     perfil.idade)
                put("objetivo",        perfil.objetivo.label)
                put("nivel_atividade", perfil.nivelAtividade.label)
                put("sexo",            perfil.sexo.label)
                if (perfil.caloriasRecomendadas > 0)
                    put("kcal_recomendadas", perfil.caloriasRecomendadas)
            }

            val hcDisponivel = healthRepo.isDisponivel() && healthRepo.temPermissoes()
            val hoje         = LocalDate.now()

            val historicoArray = JSONArray()
            for (diasAtras in 6 downTo 0) {
                val data = hoje.minusDays(diasAtras.toLong())

                val (kcal, prot, carbo, gord) = if (hcDisponivel) {
                    val n = healthRepo.lerNutricaoDia(data)
                    listOf(n.calorias, n.proteinas, n.carboidratos, n.gorduras)
                } else {
                    val local = consumoRepo.carregarConsumoLocal(data.toString())
                    listOf(local.kcal, local.proteinaG, local.carboG, local.gorduraG)
                }

                if (kcal > 0 || prot > 0 || carbo > 0 || gord > 0) {
                    historicoArray.put(JSONObject().apply {
                        put("data",           data.toString())
                        put("kcal",           kcal.toInt())
                        put("proteinas_g",    String.format("%.1f", prot).toDouble())
                        put("carboidratos_g", String.format("%.1f", carbo).toDouble())
                        put("gorduras_g",     String.format("%.1f", gord).toDouble())
                    })
                }
            }

            JSONObject().apply {
                put("perfil",               perfilJson)
                put("historico_nutricional", historicoArray)
            }.toString()

        } catch (e: Exception) {
            Log.w("MegumiViewModel", "Falha ao montar histórico de saúde: ${e.message}")
            ""
        }
    }
}
