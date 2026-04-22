package com.explosionlab.nutriengine.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.repository.AuthRepository
import com.explosionlab.nutriengine.repository.ConsumoRepository
import com.explosionlab.nutriengine.repository.MercadoRepository
import com.explosionlab.nutriengine.repository.Parceiro
import com.explosionlab.nutriengine.repository.PerfilRepository
import com.explosionlab.nutriengine.repository.RecomendacaoProduto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

class MercadoViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo     = AuthRepository(application)
    private val perfilRepo   = PerfilRepository(application)
    private val consumoRepo  = ConsumoRepository(application)
    private val mercadoRepo  = MercadoRepository(authRepo)

    private val prefs = application.getSharedPreferences("mercado_prefs", Context.MODE_PRIVATE)
    private val LAST_UPDATE_KEY = "last_mercado_update"
    private var isFirstLoadInSession = true

    private val _recomendacoes = MutableStateFlow<List<RecomendacaoProduto>>(emptyList())
    private val _parceiros     = MutableStateFlow<List<Parceiro>>(emptyList())
    private val _carregando    = MutableStateFlow(false)
    private val _erro          = MutableStateFlow("")

    val recomendacoes: StateFlow<List<RecomendacaoProduto>> = _recomendacoes.asStateFlow()
    val parceiros:     StateFlow<List<Parceiro>>            = _parceiros.asStateFlow()
    val carregando:    StateFlow<Boolean>                   = _carregando.asStateFlow()
    val erro:          StateFlow<String>                    = _erro.asStateFlow()

    init {
        carregarDados()
    }

    fun carregarDados(force: Boolean = false) {
        val agora      = System.currentTimeMillis()
        val cincoHoras = 5 * 60 * 60 * 1000L
        val lastUpdate = prefs.getLong(LAST_UPDATE_KEY, 0L)

        // Só atualiza se: for forçado, for a primeira vez na sessão, ou passar de 5h
        val deveAtualizar = force || isFirstLoadInSession || (agora - lastUpdate > cincoHoras)

        if (!deveAtualizar && _recomendacoes.value.isNotEmpty()) {
            return
        }

        isFirstLoadInSession = false

        viewModelScope.launch {
            _carregando.value = true
            _erro.value       = ""
            try {
                // Carrega parceiros e recomendações em paralelo
                val jobParceiros = launch {
                    _parceiros.value = mercadoRepo.listarParceiros()
                }

                val jobRecomendacoes = launch {
                    val (perfilJson, consumoJson, gapJson) = montarContexto()
                    _recomendacoes.value = mercadoRepo.buscarRecomendacoes(
                        perfilJson  = perfilJson,
                        consumoHoje = consumoJson,
                        gapJson     = gapJson,
                    )
                }

                jobParceiros.join()
                jobRecomendacoes.join()

                // Salva o timestamp do sucesso
                prefs.edit().putLong(LAST_UPDATE_KEY, System.currentTimeMillis()).apply()

            } catch (e: Exception) {
                Log.e("MercadoViewModel", "Erro: ${e.message}")
                _erro.value = "Não foi possível carregar as recomendações."
            } finally {
                _carregando.value = false
            }
        }
    }

    fun abrirProduto(urlCompra: String, context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(urlCompra))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e("MercadoViewModel", "Erro ao abrir URL: ${e.message}")
        }
    }

    // ── Montagem do contexto nutricional ──────────────────────────────────────

    private suspend fun montarContexto(): Triple<JSONObject, JSONObject, JSONObject> {
        val perfil = perfilRepo.carregarPerfil(
            nomeGoogleFallback = authRepo.carregarNome()
        )

        val consumoHoje = consumoRepo.carregarConsumoLocal(LocalDate.now().toString())

        val kcalConsumida  = consumoHoje.kcal
        val protConsumida  = consumoHoje.proteinaG
        val carboConsumido = consumoHoje.carboG
        val gordConsumida  = consumoHoje.gorduraG

        // Metas de macros estimadas a partir da meta calórica e objetivo
        val kcalMeta  = perfil.caloriasRecomendadas.toDouble()
        val (pCarbo, pProt, pGord) = when (perfil.objetivo) {
            com.explosionlab.nutriengine.repository.Objetivo.GANHAR_MUSCULOS      -> Triple(0.45, 0.30, 0.25)
            com.explosionlab.nutriengine.repository.Objetivo.PERDER_PESO          -> Triple(0.40, 0.35, 0.25)
            com.explosionlab.nutriengine.repository.Objetivo.MELHORAR_ALIMENTACAO -> Triple(0.50, 0.25, 0.25)
        }
        val protMeta  = kcalMeta * pProt  / 4.0
        val carboMeta = kcalMeta * pCarbo / 4.0
        val gordMeta  = kcalMeta * pGord  / 9.0

        val perfilJson = JSONObject().apply {
            put("objetivo",          perfil.objetivo.label)
            put("kcal_recomendadas", kcalMeta.toInt())
            put("sexo",              perfil.sexo.label)
            if (perfil.idade > 0) put("idade", perfil.idade)
        }

        val consumoJson = JSONObject().apply {
            put("kcal",           kcalConsumida.toInt())
            put("proteinas_g",    protConsumida)
            put("carboidratos_g", carboConsumido)
            put("gorduras_g",     gordConsumida)
        }

        val gapJson = JSONObject().apply {
            put("kcal",           (kcalMeta  - kcalConsumida).coerceAtLeast(0.0).toInt())
            put("proteinas_g",    (protMeta  - protConsumida).coerceAtLeast(0.0))
            put("carboidratos_g", (carboMeta - carboConsumido).coerceAtLeast(0.0))
            put("gorduras_g",     (gordMeta  - gordConsumida).coerceAtLeast(0.0))
        }

        return Triple(perfilJson, consumoJson, gapJson)
    }
}