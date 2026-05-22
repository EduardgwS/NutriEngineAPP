package com.explosionlab.nutriengine.features.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.core.data.repository.AuthRepository
import com.explosionlab.nutriengine.core.data.repository.ConsumoRepository
import com.explosionlab.nutriengine.core.data.repository.PerfilRepository
import com.explosionlab.nutriengine.core.di.NetworkModule
import com.explosionlab.nutriengine.core.model.DicaMacro
import com.explosionlab.nutriengine.core.model.Objetivo
import com.explosionlab.nutriengine.core.model.RecomendacaoReceita
import com.explosionlab.nutriengine.features.health.HealthConnectRepository
import com.explosionlab.nutriengine.core.data.repository.HealthContextRepository
import com.explosionlab.nutriengine.features.megumi.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

data class HomeUiState(
    val caloriasHoje: Double = 0.0,
    val caloriasRecomendadas: Int = 0,
    val recomendacaoReceita: RecomendacaoReceita? = null,
    val macroState: MacroState = MacroState(),
    val dicaMacro: DicaMacro? = null,
    val streak: Int = 0,
    val semanaStatus: List<Boolean> = emptyList(),
    val insightMegumi: String? = null,
    val carregandoInsight: Boolean = false,
)

data class MacroState(
    val proteinaConsumida: Double = 0.0,
    val carboConsumido: Double = 0.0,
    val gorduraConsumida: Double = 0.0,
    val proteinaMeta: Double = 0.0,
    val carboMeta: Double = 0.0,
    val gorduraMeta: Double = 0.0,
) {
    private val proteinaGap get() = (proteinaMeta - proteinaConsumida).coerceAtLeast(0.0)
    private val carboGap get() = (carboMeta - carboConsumido).coerceAtLeast(0.0)
    private val gorduraGap get() = (gorduraMeta - gorduraConsumida).coerceAtLeast(0.0)

    val maiorDeficit: Int
        get() {
            if (proteinaMeta <= 0) return -1
            val pctProt = proteinaGap / proteinaMeta
            val pctCarbo = carboGap / carboMeta.coerceAtLeast(1.0)
            val pctGord = gorduraGap / gorduraMeta.coerceAtLeast(1.0)
            val max = maxOf(pctProt, pctCarbo, pctGord)
            return when {
                max < 0.10 -> -1
                max == pctProt -> 0
                max == pctCarbo -> 1
                else -> 2
            }
        }
}

private data class ProporcoesMacro(val carbo: Double, val proteina: Double, val gordura: Double)

private fun proporcoesPara(objetivo: Objetivo) = when (objetivo) {
    Objetivo.GANHAR_MUSCULOS      -> ProporcoesMacro(carbo = 0.45, proteina = 0.30, gordura = 0.25)
    Objetivo.PERDER_PESO          -> ProporcoesMacro(carbo = 0.40, proteina = 0.35, gordura = 0.25)
    Objetivo.MELHORAR_ALIMENTACAO -> ProporcoesMacro(carbo = 0.50, proteina = 0.25, gordura = 0.25)
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AuthRepository(application)
    private val healthRepo = HealthConnectRepository(application)
    private val perfilRepo = PerfilRepository(application)
    private val consumoRepo = ConsumoRepository(application)
    private val chatRepo = ChatRepository(authRepo)
    private val healthContextRepo = HealthContextRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        carregarDados()
        viewModelScope.launch {
            consumoRepo.mudancas.collect { carregarDados() }
        }
    }

    fun recarregarCaloriasHome() = carregarDados()

    private fun carregarDados() = viewModelScope.launch {
        try {
            val hoje = LocalDate.now()
            val perfil = perfilRepo.carregarPerfil(authRepo.carregarNome())

            val nutri = carregarNutricao(hoje)
            val macroState = calcularMacroState(nutri, perfil.caloriasRecomendadas, perfil.objetivo)

            _uiState.update {
                it.copy(
                    caloriasHoje = nutri.calorias,
                    caloriasRecomendadas = perfil.caloriasRecomendadas,
                    macroState = macroState,
                )
            }

            launch { carregarStreak(hoje, perfil.caloriasRecomendadas) }
            launch { buscarRecomendacoes(perfil.objetivo, macroState) }
            launch { solicitarInsightInterno() }

        } catch (e: Exception) {
            Log.e("HomeViewModel", "Erro ao carregar dados", e)
        }
    }

    private suspend fun carregarNutricao(hoje: LocalDate): HealthConnectRepository.NutricaoDiaria {
        val local = consumoRepo.carregarConsumoLocal(hoje.toString())
        var resultado = HealthConnectRepository.NutricaoDiaria(
            calorias = local.kcal,
            carboidratos = local.carboG,
            proteinas = local.proteinaG,
            gorduras = local.gorduraG,
        )

        if (!healthRepo.isDisponivel() || !healthRepo.temPermissoes()) return resultado

        val hcTotal = healthRepo.lerNutricaoDia(hoje)
        if (hcTotal.calorias > 0) resultado = hcTotal

        val hcProprio = healthRepo.lerNutricaoPropriaDia(hoje)
        val deveSincronizar = local.kcal > 0 && abs(hcProprio.calorias - local.kcal) > 1.0
        if (deveSincronizar) {
            healthRepo.sincronizarNutricaoDia(
                hoje,
                HealthConnectRepository.NutricaoDiaria(
                    calorias = local.kcal,
                    carboidratos = local.carboG,
                    proteinas = local.proteinaG,
                    gorduras = local.gorduraG,
                ),
            )
        }

        return resultado
    }

    private fun calcularMacroState(
        nutri: HealthConnectRepository.NutricaoDiaria,
        meta: Int,
        objetivo: Objetivo,
    ): MacroState {
        val p = proporcoesPara(objetivo)
        return MacroState(
            proteinaConsumida = nutri.proteinas,
            carboConsumido = nutri.carboidratos,
            gorduraConsumida = nutri.gorduras,
            proteinaMeta = meta * p.proteina / 4.0,
            carboMeta = meta * p.carbo / 4.0,
            gorduraMeta = meta * p.gordura / 9.0,
        )
    }

    private suspend fun carregarStreak(hoje: LocalDate, meta: Int) {
        fun isDentroMeta(kcal: Double) = kcal > 0 && kcal <= meta * 1.1

        val hist30 = consumoRepo.lerHistoricoDias(30)
        var streak = 0
        for (dia in hist30.reversed()) {
            if (isDentroMeta(dia.kcal)) streak++
            else if (dia.data != hoje.toString()) break
        }

        val semanaStatus = consumoRepo.lerHistoricoDias(7).map { isDentroMeta(it.kcal) }

        _uiState.update { it.copy(streak = streak, semanaStatus = semanaStatus) }
    }

    private suspend fun buscarRecomendacoes(objetivo: Objetivo, macros: MacroState) {
        try {
            val receita = NetworkModule.api.getReceitaDoDia(objetivo.name).receita
            val dica = NetworkModule.api.getDicaMacro(macros.maiorDeficit, macros.proteinaConsumida).dica
            _uiState.update {
                it.copy(
                    recomendacaoReceita = receita.takeIf { r -> r.titulo != null },
                    dicaMacro = dica,
                )
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Erro na API de recomendações", e)
        }
    }

    private suspend fun solicitarInsightInterno() {
        if (_uiState.value.carregandoInsight) return

        _uiState.update { it.copy(carregandoInsight = true) }
        try {
            val historicoJson = healthContextRepo.montarHistoricoSaudeJson()
            val resposta = chatRepo.pedirInsight(historicoJson)
            _uiState.update { it.copy(insightMegumi = resposta, carregandoInsight = false) }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Erro ao solicitar insight", e)
            _uiState.update { it.copy(carregandoInsight = false) }
        }
    }
}
