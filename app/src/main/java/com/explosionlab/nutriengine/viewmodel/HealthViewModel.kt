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
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HealthUiState(
    val peso:           Double? = null,
    val altura:         Double? = null,
    val caloriasAtivas: Double  = 0.0,
    val nutricao:       HealthRepository.NutricaoDiaria = HealthRepository.NutricaoDiaria(),
    val isCarregando:   Boolean = false,
    val mensagemErro:   String  = "",
    val mensagemOk:     String  = "",
    val temPermissoes:  Boolean = false,
    val hcDisponivel:   Boolean = false,
)

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val healthRepo  = HealthRepository(application)
    private val authRepo    = AuthRepository(application)
    private val consumoRepo = ConsumoRepository(application)

    var state by mutableStateOf(HealthUiState(hcDisponivel = healthRepo.isDisponivel()))
        private set


    val permissions = healthRepo.permissions


    fun verificarPermissoes() {
        viewModelScope.launch {
            state = state.copy(temPermissoes = healthRepo.temPermissoes())
        }
    }


    fun carregarDados() {
        viewModelScope.launch {
            state = state.copy(isCarregando = true, mensagemErro = "", mensagemOk = "")

            val nutricaoHC = healthRepo.lerNutricaoHoje()

            if (nutricaoHC.calorias > 0 || nutricaoHC.proteinas > 0
                || nutricaoHC.carboidratos > 0 || nutricaoHC.gorduras > 0) {

                consumoRepo.salvarConsumoLocal(
                    data      = LocalDate.now().toString(),
                    kcal      = nutricaoHC.calorias,
                    proteinaG = nutricaoHC.proteinas,
                    carboG    = nutricaoHC.carboidratos,
                    gorduraG  = nutricaoHC.gorduras,
                )
            }

            state = state.copy(
                peso           = healthRepo.lerUltimoPeso(),
                altura         = healthRepo.lerUltimaAltura(),
                caloriasAtivas = healthRepo.lerCaloriasAtivasHoje(),
                nutricao       = nutricaoHC,
                isCarregando   = false,
            )
        }
    }



    fun salvarPeso(input: String) {
        val kg = input.toDoubleOrNull()
        if (kg == null || kg <= 0) {
            state = state.copy(mensagemErro = "Peso inválido. Ex: 72.5")
            return
        }
        viewModelScope.launch {
            state = state.copy(isCarregando = true, mensagemErro = "", mensagemOk = "")
            val ok = healthRepo.salvarPeso(kg)
            state = state.copy(
                isCarregando = false,
                mensagemOk   = if (ok) "Peso salvo!"          else "",
                mensagemErro = if (!ok) "Falha ao salvar peso." else "",
            )
            if (ok) carregarDados()
        }
    }



    fun salvarAltura(input: String) {
        val metros = input.toDoubleOrNull()
        if (metros == null || metros <= 0) {
            state = state.copy(mensagemErro = "Altura inválida. Ex: 1.75")
            return
        }
        viewModelScope.launch {
            state = state.copy(isCarregando = true, mensagemErro = "", mensagemOk = "")
            val ok = healthRepo.salvarAltura(metros)
            state = state.copy(
                isCarregando = false,
                mensagemOk   = if (ok) "Altura salva!"          else "",
                mensagemErro = if (!ok) "Falha ao salvar altura." else "",
            )
            if (ok) carregarDados()
        }
    }



    fun salvarNutricao(entrada: HealthRepository.EntradaNutricao) {
        val temAlgo = listOf(
            entrada.carboidratos, entrada.proteinas, entrada.gorduras, entrada.calorias,
            entrada.vitaminaC, entrada.vitaminaD, entrada.vitaminaA, entrada.vitaminaB12,
            entrada.calcio, entrada.ferro, entrada.sodio, entrada.potassio
        ).any { it != null }

        if (!temAlgo) {
            state = state.copy(mensagemErro = "Preencha pelo menos um campo nutricional.")
            return
        }

        viewModelScope.launch {
            state = state.copy(isCarregando = true, mensagemErro = "", mensagemOk = "")

            val ok = healthRepo.salvarNutricao(entrada)

            if (ok) {
                val totalHoje = healthRepo.lerNutricaoHoje()
                consumoRepo.salvarConsumoLocal(
                    data      = LocalDate.now().toString(),
                    kcal      = totalHoje.calorias,
                    proteinaG = totalHoje.proteinas,
                    carboG    = totalHoje.carboidratos,
                    gorduraG  = totalHoje.gorduras,
                )
            }

            state = state.copy(
                isCarregando = false,
                mensagemOk   = if (ok) "Nutrição registrada!"          else "",
                mensagemErro = if (!ok) "Falha ao salvar nutrição."    else "",
            )
            if (ok) carregarDados()
        }
    }
}
