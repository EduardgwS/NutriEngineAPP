package com.explosionlab.nutriengine.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explosionlab.nutriengine.repository.Alimento
import com.explosionlab.nutriengine.repository.AlimentoRepository
import com.explosionlab.nutriengine.repository.AuthRepository
import com.explosionlab.nutriengine.repository.ConsumoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PesquisarViewModel(application: Application) : AndroidViewModel(application) {

    private val repo        = AlimentoRepository(AuthRepository(application))
    private val consumoRepo = ConsumoRepository(application)


    private val _resultados = MutableStateFlow<List<Alimento>>(emptyList())
    val resultados: StateFlow<List<Alimento>> = _resultados.asStateFlow()

    var query         by mutableStateOf("") ; private set
    var carregando    by mutableStateOf(false) ; private set
    var semResultados by mutableStateOf(false) ; private set

    private var jobPesquisa: Job? = null



    private val _listaEscolhidos = MutableStateFlow<List<Alimento>>(emptyList())
    val listaEscolhidos: StateFlow<List<Alimento>> = _listaEscolhidos.asStateFlow()

    fun adicionarNaLista(alimento: Alimento) {
        _listaEscolhidos.value = _listaEscolhidos.value + alimento
    }

    fun removerDaLista(alimento: Alimento) {
        _listaEscolhidos.value = _listaEscolhidos.value - alimento
    }

    fun limparLista() {
        _listaEscolhidos.value = emptyList()
    }


    fun salvarLista(itens: List<Alimento>) {
        if (itens.isEmpty()) return
        consumoRepo.acumularConsumoLocal(
            kcal      = itens.sumOf { it.kcal },
            proteinaG = itens.sumOf { it.proteinas },
            carboG    = itens.sumOf { it.carboidratos },
            gorduraG  = itens.sumOf { it.gorduras },
        )
    }



    var identificando        by mutableStateOf(false)        ; private set
    var alimentoIdentificado by mutableStateOf<Alimento?>(null) ; private set
    var erroIdentificacao    by mutableStateOf<String?>(null)   ; private set

    fun identificarPorImagem(imagemBytes: ByteArray) {
        viewModelScope.launch {
            identificando        = true
            erroIdentificacao    = null
            alimentoIdentificado = null

            val nomeIdentificado = repo.identificarPorImagem(imagemBytes)

            if (nomeIdentificado.isNullOrBlank()) {
                erroIdentificacao = "Não consegui identificar o alimento na foto. Tente outra imagem ou use a busca manual."
                identificando     = false
                return@launch
            }

            val resultado = repo.pesquisar(nomeIdentificado).firstOrNull()

            if (resultado == null) {
                erroIdentificacao = "Alimento \"$nomeIdentificado\" identificado, mas não encontrado na tabela TACO. Tente buscar manualmente."
            } else {
                alimentoIdentificado = resultado
            }
            identificando = false
        }
    }

    fun limparIdentificacao() {
        alimentoIdentificado = null
        erroIdentificacao    = null
    }




    fun onQueryChange(novo: String) {
        query         = novo
        semResultados = false

        jobPesquisa?.cancel()

        if (novo.isBlank()) {
            _resultados.value = emptyList()
            return
        }

        jobPesquisa = viewModelScope.launch {
            delay(400)
            executarPesquisa(novo)
        }
    }


    fun pesquisar(q: String = query) {
        if (q.isBlank()) return
        jobPesquisa?.cancel()
        jobPesquisa = viewModelScope.launch {
            executarPesquisa(q)
        }
    }

    private suspend fun executarPesquisa(q: String) {
        carregando = true
        val lista  = repo.pesquisar(q)
        _resultados.value = lista
        semResultados     = lista.isEmpty()
        carregando        = false
    }

    fun limpar() {
        jobPesquisa?.cancel()
        query             = ""
        _resultados.value = emptyList()
        semResultados     = false
    }
}
