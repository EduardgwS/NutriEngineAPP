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
import com.explosionlab.nutriengine.core.model.Mensagem
import com.explosionlab.nutriengine.core.data.repository.AuthRepository
import com.explosionlab.nutriengine.core.data.repository.ConsumoRepository
import com.explosionlab.nutriengine.features.health.HealthConnectRepository
import com.explosionlab.nutriengine.core.data.repository.PerfilRepository
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
    private val healthRepo     = HealthConnectRepository(application)
    private val perfilRepo     = PerfilRepository(application)
    private val consumoRepo    = ConsumoRepository(application)

    private val _mensagens = MutableStateFlow<List<Mensagem>>(emptyList())
    val mensagens: StateFlow<List<Mensagem>> = _mensagens.asStateFlow()

    var carregando by mutableStateOf(false); private set
    var semConexao by mutableStateOf(false); private set

    val nomeUsuario: String get() = authRepository.carregarNome()

    init { carregarHistorico() }

    // ── Histórico de mensagens ────────────────────────────────────────────────

    fun recarregar() = carregarHistorico()

    private fun carregarHistorico() {
        viewModelScope.launch {
            carregando = true
            try { _mensagens.value = chatRepository.carregarHistorico(limite = 50) }
            finally { carregando = false }
        }
    }

    // ── Envio de mensagem ─────────────────────────────────────────────────────

    fun enviarMensagem(texto: String, imagemBytes: ByteArray? = null) {
        val textoLimpo = texto.trim()
        if (textoLimpo.isEmpty() && imagemBytes == null) return
        if (carregando) return

        if (!isConectado()) { semConexao = true; return }
        semConexao = false

        // 1. Adiciona mensagem do usuário
        _mensagens.value = _mensagens.value + Mensagem(textoLimpo, ehUsuario = true)
        
        carregando = true

        viewModelScope.launch {
            try {
                val historicoJson = montarHistoricoSaudeJson()
                val resposta = chatRepository.enviarMensagem(textoLimpo, imagemBytes, historicoJson)
                _mensagens.value = _mensagens.value + Mensagem(resposta, ehUsuario = false)
            } catch (e: Exception) {
                Log.e("MegumiViewModel", "Erro ao enviar mensagem: ${e.message}")
            } finally {
                carregando = false
            }
        }
    }

    fun descartarAvisoSemConexao() { semConexao = false }

    // ── Conectividade ─────────────────────────────────────────────────────────

    private fun isConectado(): Boolean {
        val cm = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // ── Contexto de saúde para a IA ───────────────────────────────────────────

    /**
     * Monta JSON com perfil + histórico dos últimos 7 dias.
     * Cada dia inclui totais agregados (do HC quando disponível) e a lista
     * detalhada de alimentos registrados no app — agrupados por refeição.
     */
    private suspend fun montarHistoricoSaudeJson(): String {
        return try {
            val perfil = perfilRepo.carregarPerfil(
                nomeGoogleFallback = authRepository.carregarNome()
            )

            val perfilJson = JSONObject().apply {
                if (perfil.peso   > 0) put("peso_kg",  perfil.peso)
                if (perfil.altura > 0) put("altura_m", perfil.altura)
                if (perfil.idade  > 0) put("idade",    perfil.idade)
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

                // Totais: prefere HC, usa local como fallback
                val (kcal, prot, carbo, gord) = if (hcDisponivel) {
                    val n = healthRepo.lerNutricaoDia(data)
                    listOf(n.calorias, n.proteinas, n.carboidratos, n.gorduras)
                } else {
                    val local = consumoRepo.carregarConsumoLocal(data.toString())
                    listOf(local.kcal, local.proteinaG, local.carboG, local.gorduraG)
                }

                // Refeições salvas manualmente no app
                val listas = consumoRepo.carregarListas(data.toString())

                if (kcal > 0 || prot > 0 || carbo > 0 || gord > 0 || listas.isNotEmpty()) {
                    historicoArray.put(JSONObject().apply {
                        put("data",            data.toString())
                        put("kcal_total",      kcal.toInt())
                        put("proteinas_g",     "%.1f".format(prot).toDouble())
                        put("carboidratos_g",  "%.1f".format(carbo).toDouble())
                        put("gorduras_g",      "%.1f".format(gord).toDouble())

                        if (listas.isNotEmpty()) {
                            put("refeicoes", JSONArray().also { refArr ->
                                listas.forEach { lista ->
                                    refArr.put(JSONObject().apply {
                                        put("hora",  lista.horaTexto)
                                        put("alimentos", JSONArray().also { aArr ->
                                            lista.alimentos.forEach { a ->
                                                aArr.put(JSONObject().apply {
                                                    put("nome",           a.descricao)
                                                    put("quantidade_g",   a.quantidadeG.toInt())
                                                    put("kcal",           "%.0f".format(a.kcal).toDouble())
                                                    put("proteinas_g",    "%.1f".format(a.proteinas).toDouble())
                                                    put("carboidratos_g", "%.1f".format(a.carboidratos).toDouble())
                                                    put("gorduras_g",     "%.1f".format(a.gorduras).toDouble())
                                                })
                                            }
                                        })
                                    })
                                }
                            })
                        }
                    })
                }
            }

            JSONObject().apply {
                put("perfil",                perfilJson)
                put("historico_nutricional", historicoArray)
            }.toString()

        } catch (e: Exception) {
            Log.w("MegumiViewModel", "Falha ao montar histórico: ${e.message}")
            ""
        }
    }
}