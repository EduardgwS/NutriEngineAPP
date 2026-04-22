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

    // ── Pesquisa ──────────────────────────────────────────────────────────────

    private val _resultados = MutableStateFlow<List<Alimento>>(emptyList())
    val resultados: StateFlow<List<Alimento>> = _resultados.asStateFlow()

    private val _recentes = MutableStateFlow<List<Alimento>>(emptyList())
    val recentes: StateFlow<List<Alimento>> = _recentes.asStateFlow()

    var query         by mutableStateOf("") ; private set
    var carregando    by mutableStateOf(false) ; private set
    var semResultados by mutableStateOf(false) ; private set

    private var jobPesquisa: Job? = null

    // ── Lista de escolhidos ───────────────────────────────────────────────────

    /**
     * Alimento selecionado pelo usuário com a quantidade em gramas.
     * Os campos *Escalonado são calculados sob demanda a partir dos
     * valores base por 100 g.
     */
    data class AlimentoComQuantidade(
        val alimento:   Alimento,
        val quantidadeG: Double,
    ) {
        private val fator: Double           get() = quantidadeG / 100.0
        val kcalEscalonado:         Double  get() = alimento.kcal         * fator
        val proteinasEscalonado:    Double  get() = alimento.proteinas    * fator
        val carboidratosEscalonado: Double  get() = alimento.carboidratos * fator
        val gordurasEscalonado:     Double  get() = alimento.gorduras     * fator
    }

    private val _listaEscolhidos = MutableStateFlow<List<AlimentoComQuantidade>>(emptyList())
    val listaEscolhidos: StateFlow<List<AlimentoComQuantidade>> = _listaEscolhidos.asStateFlow()

    fun adicionarNaLista(alimento: Alimento, quantidadeG: Double = 100.0) {
        _listaEscolhidos.value = _listaEscolhidos.value +
                AlimentoComQuantidade(alimento, quantidadeG)
    }

    fun removerDaLista(item: AlimentoComQuantidade) {
        _listaEscolhidos.value = _listaEscolhidos.value - item
    }

    fun limparLista() {
        _listaEscolhidos.value = emptyList()
    }

    /**
     * Persiste os itens como uma nova [ConsumoRepository.ListaSalva] com
     * os valores nutricionais *por 100 g* (base) + quantidade escolhida.
     * O repositório recalcula os totais do dia automaticamente.
     */
    fun salvarLista(itens: List<AlimentoComQuantidade>) {
        if (itens.isEmpty()) return
        viewModelScope.launch {
            consumoRepo.salvarListaDeAlimentos(
                alimentos = itens.map { a ->
                    ConsumoRepository.AlimentoSalvo(
                        id                  = a.alimento.id,
                        descricao           = a.alimento.descricao,
                        categoria           = a.alimento.categoria,
                        quantidadeG         = a.quantidadeG,
                        kcalPer100g         = a.alimento.kcal,
                        proteinasPer100g    = a.alimento.proteinas,
                        carboidratosPer100g = a.alimento.carboidratos,
                        gordurasPer100g     = a.alimento.gorduras,
                    )
                }
            )
        }
    }

    // ── Identificação por imagem ──────────────────────────────────────────────

    var identificando        by mutableStateOf(false)           ; private set
    var alimentoIdentificado by mutableStateOf<Alimento?>(null) ; private set
    var gramasIdentificados  by mutableStateOf<Double?>(null)   ; private set
    var erroIdentificacao    by mutableStateOf<String?>(null)   ; private set

    fun identificarPorImagem(imagemBytes: ByteArray) {
        viewModelScope.launch {
            identificando        = true
            erroIdentificacao    = null
            alimentoIdentificado = null
            gramasIdentificados  = null

            val identificacao = repo.identificarPorImagem(imagemBytes)

            if (identificacao == null || identificacao.nome.isBlank()) {
                erroIdentificacao = "Não consegui identificar o alimento na foto. Tente outra imagem ou use a busca manual."
                identificando     = false
                return@launch
            }

            val resultado = repo.pesquisar(identificacao.nome).firstOrNull()
            if (resultado == null) {
                erroIdentificacao = "Alimento \"${identificacao.nome}\" identificado, mas não encontrado na tabela TACO. Tente buscar manualmente."
            } else {
                alimentoIdentificado = resultado
                gramasIdentificados  = identificacao.gramas
            }
            identificando = false
        }
    }

    fun limparIdentificacao() {
        alimentoIdentificado = null
        gramasIdentificados  = null
        erroIdentificacao    = null
    }

    // ── Pesquisa manual ───────────────────────────────────────────────────────

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
        jobPesquisa = viewModelScope.launch { executarPesquisa(q) }
    }

    private suspend fun executarPesquisa(q: String) {
        carregando        = true
        val lista         = repo.pesquisar(q)
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

    fun carregarRecentes() {
        viewModelScope.launch {
            val historico = consumoRepo.lerHistoricoCompleto7Dias()
            val alimentosUnicos = historico
                .flatMap { it.listas }
                .flatMap { it.alimentos }
                .distinctBy { it.id }
                .map { a ->
                    Alimento(
                        id           = a.id,
                        descricao    = a.descricao,
                        categoria    = a.categoria,
                        kcal         = a.kcalPer100g,
                        proteinas    = a.proteinasPer100g,
                        carboidratos = a.carboidratosPer100g,
                        gorduras     = a.gordurasPer100g
                    )
                }
                .take(20)
            _recentes.value = alimentosUnicos
        }
    }
}
