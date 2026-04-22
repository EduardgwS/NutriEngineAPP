package com.explosionlab.nutriengine.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.repository.AuthRepository
import com.explosionlab.nutriengine.repository.ConsumoRepository
import com.explosionlab.nutriengine.repository.HealthRepository
import com.explosionlab.nutriengine.repository.Objetivo
import com.explosionlab.nutriengine.repository.PerfilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Consumo e meta de macros do dia — usado para gamificação na HomeScreen. */
data class MacroState(
    // consumido hoje
    val proteinaConsumida:    Double = 0.0,
    val carboConsumido:       Double = 0.0,
    val gorduraConsumida:     Double = 0.0,
    // metas diárias
    val proteinaMeta:         Double = 0.0,
    val carboMeta:            Double = 0.0,
    val gorduraMeta:          Double = 0.0,
) {
    val proteinaGap:  Double get() = (proteinaMeta  - proteinaConsumida).coerceAtLeast(0.0)
    val carboGap:     Double get() = (carboMeta     - carboConsumido).coerceAtLeast(0.0)
    val gorduraGap:   Double get() = (gorduraMeta   - gorduraConsumida).coerceAtLeast(0.0)

    /** Macro mais deficitária em relação à sua meta (0=proteína, 1=carbo, 2=gordura, -1=tudo ok). */
    val maiorDeficit: Int get() {
        if (proteinaMeta <= 0) return -1
        val pctProt  = proteinaGap  / proteinaMeta
        val pctCarbo = carboGap     / carboMeta.coerceAtLeast(1.0)
        val pctGord  = gorduraGap   / gorduraMeta.coerceAtLeast(1.0)
        val max      = maxOf(pctProt, pctCarbo, pctGord)
        if (max < 0.10) return -1   // menos de 10% de gap → tudo ok
        return when (max) {
            pctProt  -> 0
            pctCarbo -> 1
            else     -> 2
        }
    }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val healthRepo     = HealthRepository(application)
    private val perfilRepo     = PerfilRepository(application)
    private val consumoRepo    = ConsumoRepository(application)

    private val _caloriasHoje         = MutableStateFlow(0.0)
    private val _caloriasRecomendadas = MutableStateFlow(0)
    private val _recomendacaoReceita  = MutableStateFlow<RecomendacaoReceita?>(null)
    private val _macroState           = MutableStateFlow(MacroState())

    val caloriasHoje:         StateFlow<Double>               = _caloriasHoje.asStateFlow()
    val caloriasRecomendadas: StateFlow<Int>                  = _caloriasRecomendadas.asStateFlow()
    val recomendacaoReceita:  StateFlow<RecomendacaoReceita?> = _recomendacaoReceita.asStateFlow()
    val macroState:           StateFlow<MacroState>           = _macroState.asStateFlow()

    init {
        carregarCalorias()
        observarMudancas()
    }

    fun recarregarCaloriasHome() = carregarCalorias()

    private fun carregarCalorias() {
        viewModelScope.launch {
            try {
                val hoje   = LocalDate.now()
                val perfil = perfilRepo.carregarPerfil(
                    nomeGoogleFallback = authRepository.carregarNome()
                )

                // ── Consumo de hoje ────────────────────────────────────────────
                var kcalHoje  = 0.0
                var protHoje  = 0.0
                var carboHoje = 0.0
                var gordHoje  = 0.0

                if (healthRepo.isDisponivel() && healthRepo.temPermissoes()) {
                    val hc = healthRepo.lerNutricaoDia(hoje)
                    if (hc.calorias > 0) {
                        kcalHoje  = hc.calorias
                        protHoje  = hc.proteinas
                        carboHoje = hc.carboidratos
                        gordHoje  = hc.gorduras
                        consumoRepo.acumularConsumoLocal(
                            data      = hoje.toString(),
                            kcal      = hc.calorias,
                            proteinaG = hc.proteinas,
                            carboG    = hc.carboidratos,
                            gorduraG  = hc.gorduras,
                        )
                    }
                }

                if (kcalHoje <= 0) {
                    val local = consumoRepo.carregarConsumoLocal(hoje.toString())
                    kcalHoje  = local.kcal
                    protHoje  = local.proteinaG
                    carboHoje = local.carboG
                    gordHoje  = local.gorduraG
                }

                // ── Metas de macros ────────────────────────────────────────────
                val kcalMeta = perfil.caloriasRecomendadas.toDouble()
                val (pCarbo, pProt, pGord) = when (perfil.objetivo) {
                    Objetivo.GANHAR_MUSCULOS      -> Triple(0.45, 0.30, 0.25)
                    Objetivo.PERDER_PESO          -> Triple(0.40, 0.35, 0.25)
                    Objetivo.MELHORAR_ALIMENTACAO -> Triple(0.50, 0.25, 0.25)
                }

                _caloriasHoje.value         = kcalHoje
                _caloriasRecomendadas.value = perfil.caloriasRecomendadas
                _recomendacaoReceita.value  = escolherReceitaDoDia(perfil.objetivo)
                _macroState.value           = MacroState(
                    proteinaConsumida = protHoje,
                    carboConsumido    = carboHoje,
                    gorduraConsumida  = gordHoje,
                    proteinaMeta      = kcalMeta * pProt  / 4.0,
                    carboMeta         = kcalMeta * pCarbo / 4.0,
                    gorduraMeta       = kcalMeta * pGord  / 9.0,
                )

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Erro ao carregar calorias: ${e.message}")
            }
        }
    }

    private fun observarMudancas() {
        viewModelScope.launch {
            consumoRepo.mudancas.collect {
                Log.d("HomeViewModel", "Consumo atualizado — recarregando calorias.")
                carregarCalorias()
            }
        }
    }
}