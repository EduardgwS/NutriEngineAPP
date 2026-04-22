package com.explosionlab.nutriengine.repository

import android.content.Context
import android.util.Log
import com.explosionlab.nutriengine.data.local.AppDatabase
import com.explosionlab.nutriengine.data.local.entity.AlimentoEntity
import com.explosionlab.nutriengine.data.local.entity.ConsumoDiarioEntity
import com.explosionlab.nutriengine.data.local.entity.ListaEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ConsumoRepository(context: Context) {

    private val TAG = "ConsumoRepository"
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.consumoDao()

    private val _mudancas = MutableSharedFlow<Unit>(replay = 1)
    val mudancas: SharedFlow<Unit> = _mudancas.asSharedFlow()

    // ── Modelos ───────────────────────────────────────────────────────────────

    data class AlimentoSalvo(
        val id: String,
        val descricao: String,
        val categoria: String,
        val quantidadeG: Double,
        val kcalPer100g: Double,
        val proteinasPer100g: Double,
        val carboidratosPer100g: Double,
        val gordurasPer100g: Double,
    ) {
        private val fator: Double get() = quantidadeG / 100.0
        val kcal: Double get() = kcalPer100g * fator
        val proteinas: Double get() = proteinasPer100g * fator
        val carboidratos: Double get() = carboidratosPer100g * fator
        val gorduras: Double get() = gordurasPer100g * fator

        fun comQuantidade(g: Double) = copy(quantidadeG = g.coerceAtLeast(0.1))
    }

    data class ListaSalva(
        val id: String,
        val timestamp: Long,
        val horaTexto: String,
        val alimentos: List<AlimentoSalvo>,
    ) {
        val totalKcal: Double get() = alimentos.sumOf { it.kcal }
        val totalProteinas: Double get() = alimentos.sumOf { it.proteinas }
        val totalCarboidratos: Double get() = alimentos.sumOf { it.carboidratos }
        val totalGorduras: Double get() = alimentos.sumOf { it.gorduras }
    }

    data class ConsumoLocal(
        val data: String,
        val kcal: Double,
        val proteinaG: Double,
        val carboG: Double,
        val gorduraG: Double,
        val atualizadoEm: Long,
    )

    data class ConsumoCompleto(
        val consumo: ConsumoLocal,
        val listas: List<ListaSalva>,
    )

    // ── Mapeadores ────────────────────────────────────────────────────────────

    private fun AlimentoEntity.toAlimentoSalvo() = AlimentoSalvo(
        id = alimentoId,
        descricao = descricao,
        categoria = categoria,
        quantidadeG = quantidadeG,
        kcalPer100g = kcalPer100g,
        proteinasPer100g = proteinasPer100g,
        carboidratosPer100g = carboidratosPer100g,
        gordurasPer100g = gordurasPer100g
    )

    private fun AlimentoSalvo.toEntity(listaId: String) = AlimentoEntity(
        listaId = listaId,
        alimentoId = id,
        descricao = descricao,
        categoria = categoria,
        quantidadeG = quantidadeG,
        kcalPer100g = kcalPer100g,
        proteinasPer100g = proteinasPer100g,
        carboidratosPer100g = carboidratosPer100g,
        gordurasPer100g = gordurasPer100g
    )

    // ── Listas — escrita ──────────────────────────────────────────────────────

    suspend fun salvarListaDeAlimentos(
        data: String = LocalDate.now().toString(),
        alimentos: List<AlimentoSalvo>,
    ) {
        if (alimentos.isEmpty()) return
        val hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val listaId = UUID.randomUUID().toString()
        
        val listaEntity = ListaEntity(
            id = listaId,
            data = data,
            timestamp = System.currentTimeMillis(),
            horaTexto = hora
        )
        
        val alimentosEntities = alimentos.map { it.toEntity(listaId) }
        
        dao.salvarListaCompleta(listaEntity, alimentosEntities)
        recalcularTotais(data)
        Log.d(TAG, "Lista salva — $data $hora: ${alimentos.size} itens")
    }

    // ── Listas — edição ───────────────────────────────────────────────────────

    suspend fun editarAlimento(
        data: String,
        listaId: String,
        alimentoId: String,
        novaQuantidadeG: Double,
    ) {
        dao.atualizarQuantidadeAlimento(listaId, alimentoId, novaQuantidadeG)
        recalcularTotais(data)
        _mudancas.emit(Unit)
    }

    suspend fun removerAlimento(
        data: String,
        listaId: String,
        alimentoId: String,
    ) {
        dao.removerAlimento(listaId, alimentoId)
        
        // Verifica se a lista ficou vazia
        val restantes = dao.getAlimentosDaLista(listaId)
        if (restantes.isEmpty()) {
            dao.deletarLista(listaId)
        }
        
        recalcularTotais(data)
        _mudancas.emit(Unit)
    }

    suspend fun removerLista(data: String, listaId: String) {
        dao.deletarLista(listaId)
        recalcularTotais(data)
        _mudancas.emit(Unit)
    }

    // ── Listas — leitura ──────────────────────────────────────────────────────

    suspend fun carregarListas(data: String = LocalDate.now().toString()): List<ListaSalva> {
        val listaEntities = dao.getListasPorData(data)
        return listaEntities.map { le ->
            val alimentos = dao.getAlimentosDaLista(le.id).map { it.toAlimentoSalvo() }
            ListaSalva(
                id = le.id,
                timestamp = le.timestamp,
                horaTexto = le.horaTexto,
                alimentos = alimentos
            )
        }
    }

    // ── Persistência interna ──────────────────────────────────────────────────

    private suspend fun recalcularTotais(data: String) {
        val listas = carregarListas(data)
        salvarConsumoLocal(
            data = data,
            kcal = listas.sumOf { it.totalKcal },
            proteinaG = listas.sumOf { it.totalProteinas },
            carboG = listas.sumOf { it.totalCarboidratos },
            gorduraG = listas.sumOf { it.totalGorduras },
        )
    }

    // ── Consumo agregado ──────────────────────────────────────────────────────

    suspend fun salvarConsumoLocal(
        data: String = LocalDate.now().toString(),
        kcal: Double,
        proteinaG: Double,
        carboG: Double,
        gorduraG: Double,
    ) {
        val entity = ConsumoDiarioEntity(
            data = data,
            kcal = kcal,
            proteinaG = proteinaG,
            carboG = carboG,
            gorduraG = gorduraG,
            atualizadoEm = System.currentTimeMillis()
        )
        dao.inserirConsumoDiario(entity)
        _mudancas.emit(Unit)
    }

    suspend fun acumularConsumoLocal(
        data: String = LocalDate.now().toString(),
        kcal: Double,
        proteinaG: Double,
        carboG: Double,
        gorduraG: Double,
    ) {
        val atual = carregarConsumoLocal(data)
        salvarConsumoLocal(
            data = data,
            kcal = atual.kcal + kcal,
            proteinaG = atual.proteinaG + proteinaG,
            carboG = atual.carboG + carboG,
            gorduraG = atual.gorduraG + gorduraG,
        )
    }

    suspend fun carregarConsumoLocal(data: String = LocalDate.now().toString()): ConsumoLocal {
        val entity = dao.getConsumoDiario(data)
        return if (entity != null) {
            ConsumoLocal(
                data = entity.data,
                kcal = entity.kcal,
                proteinaG = entity.proteinaG,
                carboG = entity.carboG,
                gorduraG = entity.gorduraG,
                atualizadoEm = entity.atualizadoEm
            )
        } else {
            ConsumoLocal(data, 0.0, 0.0, 0.0, 0.0, 0)
        }
    }

    suspend fun temRegistroLocal(data: String = LocalDate.now().toString()): Boolean {
        return dao.getConsumoDiario(data) != null
    }

    // ── Histórico ─────────────────────────────────────────────────────────────

    suspend fun lerHistorico7Dias(): List<ConsumoLocal> {
        val hoje = LocalDate.now()
        val datas = (0..6).map { hoje.minusDays(it.toLong()).toString() }
        val entities = dao.getConsumosPorDatas(datas).associateBy { it.data }
        
        return datas.map { d ->
            val entity = entities[d]
            if (entity != null) {
                ConsumoLocal(entity.data, entity.kcal, entity.proteinaG, entity.carboG, entity.gorduraG, entity.atualizadoEm)
            } else {
                ConsumoLocal(d, 0.0, 0.0, 0.0, 0.0, 0)
            }
        }.reversed()
    }

    suspend fun lerHistoricoCompleto7Dias(): List<ConsumoCompleto> {
        val hoje = LocalDate.now()
        return (6 downTo 0).map { diasAtras ->
            val data = hoje.minusDays(diasAtras.toLong()).toString()
            ConsumoCompleto(
                consumo = carregarConsumoLocal(data),
                listas = carregarListas(data),
            )
        }
    }
}
