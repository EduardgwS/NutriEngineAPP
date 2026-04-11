package com.explosionlab.nutriengine.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.repository.AuthRepository
import com.explosionlab.nutriengine.repository.HealthRepository
import com.explosionlab.nutriengine.repository.NivelAtividade
import com.explosionlab.nutriengine.repository.Objetivo
import com.explosionlab.nutriengine.repository.PerfilRepository
import com.explosionlab.nutriengine.repository.Sexo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period

data class PerfilUiState(
    val nomeInput:        String         = "",
    val alturaInput:      String         = "",
    val pesoInput:        String         = "",
    val dataNascimento:   LocalDate?     = null,
    val sexo:             Sexo           = Sexo.MASCULINO,
    val objetivo:         Objetivo       = Objetivo.MELHORAR_ALIMENTACAO,
    val nivelAtividade:   NivelAtividade = NivelAtividade.SEDENTARIO,
    val preenchidoPeloHC: Boolean        = false,
    val dataVeioDoGoogle: Boolean        = false,
    val isCarregando:     Boolean        = true,
    val erro:             String         = "",
    val concluido:        Boolean        = false,
)

class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val perfilRepo = PerfilRepository(application)
    private val authRepo   = AuthRepository(application)
    private val healthRepo = HealthRepository(application)

    var state by mutableStateOf(run {
        val nomeGoogle       = authRepo.carregarNome()
        PerfilUiState(
            nomeInput        = nomeGoogle,
        )
    }); private set

    init {
        carregarDadosIniciais()
    }


    private fun carregarDadosIniciais() {
        viewModelScope.launch {
            state = state.copy(isCarregando = true)

            if (healthRepo.isDisponivel()) {

                delay(600L)

                if (healthRepo.temPermissoes()) {
                    val peso   = healthRepo.lerUltimoPeso()
                    val altura = healthRepo.lerUltimaAltura()

                    val pesoPre   = peso?.let   { "%.1f".format(it) } ?: ""
                    val alturaPre = altura?.let { "%.2f".format(it) } ?: ""

                    state = state.copy(
                        pesoInput        = pesoPre,
                        alturaInput      = alturaPre,
                        preenchidoPeloHC = pesoPre.isNotEmpty() || alturaPre.isNotEmpty(),
                    )
                }
            }

            state = state.copy(isCarregando = false)
        }
    }



    fun onNomeChange(v: String)   { state = state.copy(nomeInput   = v, erro = "") }
    fun onAlturaChange(v: String) { state = state.copy(alturaInput = v, erro = "") }
    fun onPesoChange(v: String)   { state = state.copy(pesoInput   = v, erro = "") }
    fun onSexoChange(v: Sexo)     { state = state.copy(sexo = v) }
    fun onObjetivoChange(v: Objetivo)             { state = state.copy(objetivo = v) }
    fun onNivelAtividadeChange(v: NivelAtividade) { state = state.copy(nivelAtividade = v) }

    fun onDataNascimentoChange(d: LocalDate) {
        state = state.copy(dataNascimento = d, dataVeioDoGoogle = false, erro = "")
    }



    fun salvar() {
        val nomeLimpo = state.nomeInput.trim()
        val altura    = state.alturaInput.replace(",", ".").toDoubleOrNull()
        val peso      = state.pesoInput.replace(",", ".").toDoubleOrNull()
        val dataNasc  = state.dataNascimento

        val hoje      = LocalDate.now()
        val idadeCalc = dataNasc?.let { Period.between(it, hoje).years }

        when {
            nomeLimpo.isBlank() ->
            { state = state.copy(erro = "Informe seu nome de usuário."); return }
            nomeLimpo.length < 2 ->
            { state = state.copy(erro = "O nome deve ter ao menos 2 caracteres."); return }
            altura == null || altura <= 0 || altura > 3 ->
            { state = state.copy(erro = "Altura inválida. Use metros, ex: 1.75"); return }
            peso == null || peso <= 0 || peso > 500 ->
            { state = state.copy(erro = "Peso inválido. Ex: 72.5"); return }
            dataNasc == null ->
            { state = state.copy(erro = "Selecione sua data de nascimento."); return }
            dataNasc.isAfter(hoje) ->
            { state = state.copy(erro = "Data de nascimento inválida."); return }
            idadeCalc != null && idadeCalc < 5 ->
            { state = state.copy(erro = "Data de nascimento inválida."); return }
            idadeCalc != null && idadeCalc > 120 ->
            { state = state.copy(erro = "Data de nascimento inválida."); return }
        }

        viewModelScope.launch {
            state = state.copy(isCarregando = true, erro = "")


            perfilRepo.salvarPerfil(
                nome           = nomeLimpo,
                dataNascimento = dataNasc!!,
                sexo           = state.sexo,
                objetivo       = state.objetivo,
                nivelAtividade = state.nivelAtividade,
                peso           = peso!!,
                altura         = altura!!,
            )


            if (healthRepo.isDisponivel() && healthRepo.temPermissoes()) {
                healthRepo.salvarPeso(peso)
                healthRepo.salvarAltura(altura)
            }

            state = state.copy(isCarregando = false, concluido = true)
        }
    }
}