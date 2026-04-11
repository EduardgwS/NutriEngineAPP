package com.explosionlab.nutriengine.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.repository.AuthRepository
import com.explosionlab.nutriengine.repository.ConsumoRepository
import com.explosionlab.nutriengine.repository.HealthRepository
import com.explosionlab.nutriengine.repository.Perfil
import com.explosionlab.nutriengine.repository.PerfilRepository
import kotlinx.coroutines.launch

data class RelatorioUiState(
    val perfil:        Perfil?  = null,
    val historico7Dias: List<ConsumoRepository.ConsumoLocal> = emptyList(),
    val carregando:    Boolean  = true,
)

class RelatorioViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo    = AuthRepository(application)
    private val perfilRepo  = PerfilRepository(application)
    private val healthRepo  = HealthRepository(application)
    private val consumoRepo = ConsumoRepository(application)

    var state by mutableStateOf(RelatorioUiState()); private set

    init {



        carregarDados()
    }

    fun carregarDados() {
        viewModelScope.launch {
            state = state.copy(carregando = true)
            try {
                var pesoOverride:   Double? = null
                var alturaOverride: Double? = null

                if (healthRepo.isDisponivel() && healthRepo.temPermissoes()) {


                    val pesoHC   = healthRepo.lerUltimoPeso()
                    val alturaHC = healthRepo.lerUltimaAltura()

                    if (pesoHC != null || alturaHC != null) {
                        val pesoAtual   = pesoHC   ?: perfilRepo.carregarPeso()
                        val alturaAtual = alturaHC ?: perfilRepo.carregarAltura()


                        perfilRepo.salvarMedidas(pesoAtual, alturaAtual)
                        pesoOverride   = pesoAtual
                        alturaOverride = alturaAtual
                    }


                    val nutricao = healthRepo.lerNutricaoHoje()


                    if (nutricao.calorias > 0 || nutricao.proteinas > 0
                        || nutricao.carboidratos > 0 || nutricao.gorduras > 0) {

                        consumoRepo.salvarConsumoLocal(
                            data      = java.time.LocalDate.now().toString(),
                            kcal      = nutricao.calorias,
                            proteinaG = nutricao.proteinas,
                            carboG    = nutricao.carboidratos,
                            gorduraG  = nutricao.gorduras,
                        )
                    }
                }

                val perfil         = perfilRepo.carregarPerfil(
                    nomeGoogleFallback = authRepo.carregarNome(),
                    pesoOverride       = pesoOverride,
                    alturaOverride     = alturaOverride,
                )
                val historico7Dias = consumoRepo.lerHistorico7Dias()

                state = state.copy(
                    perfil         = perfil,
                    historico7Dias = historico7Dias,
                    carregando     = false,
                )

            } catch (e: Exception) {
                // Garante que a tela sai do loading mesmo em caso de erro inesperado.
                // Tenta carregar o perfil só das prefs locais como fallback.
                val perfilFallback   = runCatching {
                    perfilRepo.carregarPerfil(nomeGoogleFallback = authRepo.carregarNome())
                }.getOrNull()
                val historicoFallback = runCatching { consumoRepo.lerHistorico7Dias() }.getOrElse { emptyList() }
                state = state.copy(perfil = perfilFallback, historico7Dias = historicoFallback, carregando = false)
            }
        }
    }
}