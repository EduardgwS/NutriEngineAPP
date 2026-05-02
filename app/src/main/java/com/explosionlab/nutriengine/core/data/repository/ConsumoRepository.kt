package com.explosionlab.nutriengine.core.data.repository

import android.content.Context
import android.util.Log
import com.explosionlab.nutriengine.core.data.local.AppDatabase
import com.explosionlab.nutriengine.core.data.local.entity.AlimentoEntity
import com.explosionlab.nutriengine.core.data.local.entity.ConsumoDiarioEntity
import com.explosionlab.nutriengine.core.data.local.entity.ListaEntity
import com.explosionlab.nutriengine.features.health.HealthConnectRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ConsumoRepository(private val context: Context) {

    companion object {
        private const val TAG = "ConsumoRepository"
    }

    private val db  = AppDatabase.getDatabase(context)
    private val dao = db.consumoDao()
    private val healthRepo = HealthConnectRepository(context)

    private val _mudancas = MutableSharedFlow<Unit>(replay = 1)
    val mudancas: SharedFlow<Unit> = _mudancas.asSharedFlow()

    //Modelos

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
        val kcal: Double         get() = kcalPer100g * fator
        val proteinas: Double    get() = proteinasPer100g * fator
        val carboidratos: Double get() = carboidratosPer100g * fator
        val gorduras: Double     get() = gordurasPer100g * fator
    }

    data class ListaSalva(
        val id: String,
        val timestamp: Long,
        val horaTexto: String,
        val alimentos: List<AlimentoSalvo>,
    ) {
        val totalKcal: Double        get() = alimentos.sumOf { it.kcal }
        val totalProteinas: Double   get() = alimentos.sumOf { it.proteinas }
        val totalCarboidratos: Double get() = alimentos.sumOf { it.carboidratos }
        val totalGorduras: Double    get() = alimentos.sumOf { it.gorduras }
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

    //Mappers

    private fun AlimentoEntity.toAlimentoSalvo() = AlimentoSalvo(
        id               = alimentoId,
        descricao        = descricao,
        categoria        = categoria,
        quantidadeG      = quantidadeG,
        kcalPer100g      = kcalPer100g,
        proteinasPer100g = proteinasPer100g,
        carboidratosPer100g = carboidratosPer100g,
        gordurasPer100g  = gordurasPer100g
    )

    private fun AlimentoSalvo.toEntity(listaId: String) = AlimentoEntity(
        listaId          = listaId,
        alimentoId       = id,
        descricao        = descricao,
        categoria        = categoria,
        quantidadeG      = quantidadeG,
        kcalPer100g      = kcalPer100g,
        proteinasPer100g = proteinasPer100g,
        carboidratosPer100g = carboidratosPer100g,
        gordurasPer100g  = gordurasPer100g
    )

    private fun ConsumoDiarioEntity.toConsumoLocal() = ConsumoLocal(
        data         = data,
        kcal         = kcal,
        proteinaG    = proteinaG,
        carboG       = carboG,
        gorduraG     = gorduraG,
        atualizadoEm = atualizadoEm
    )

    //Salvamento das listas de alimentos

    suspend fun salvarListaDeAlimentos(
        data: String = LocalDate.now().toString(),
        alimentos: List<AlimentoSalvo>,
    ) {
        if (alimentos.isEmpty()) return
        val hora    = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val listaId = UUID.randomUUID().toString()

        dao.salvarListaCompleta(
            ListaEntity(id = listaId, data = data, timestamp = System.currentTimeMillis(), horaTexto = hora),
            alimentos.map { it.toEntity(listaId) }
        )
        recalcularTotais(data)
        Log.d(TAG, "Lista salva — $data $hora: ${alimentos.size} itens")
    }

    //Edição dos alimentos

    suspend fun editarAlimento(data: String, listaId: String, alimentoId: String, novaQuantidadeG: Double) {
        dao.atualizarQuantidadeAlimento(listaId, alimentoId, novaQuantidadeG)
        recalcularTotais(data)
    }

    suspend fun removerAlimento(data: String, listaId: String, alimentoId: String) {
        dao.removerAlimento(listaId, alimentoId)
        if (dao.getAlimentosDaLista(listaId).isEmpty()) dao.deletarLista(listaId)
        recalcularTotais(data)
    }

    suspend fun removerLista(data: String, listaId: String) {
        dao.deletarLista(listaId)
        recalcularTotais(data)
    }


    suspend fun carregarListas(data: String = LocalDate.now().toString()): List<ListaSalva> =
        dao.getListasPorData(data).map { le ->
            ListaSalva(
                id        = le.id,
                timestamp = le.timestamp,
                horaTexto = le.horaTexto,
                alimentos = dao.getAlimentosDaLista(le.id).map { it.toAlimentoSalvo() }
            )
        }

    //Persistencia interna

    private suspend fun recalcularTotais(data: String) {
        val listas = carregarListas(data)
        val kcal = listas.sumOf { it.totalKcal }
        val prot = listas.sumOf { it.totalProteinas }
        val carbo = listas.sumOf { it.totalCarboidratos }
        val gord = listas.sumOf { it.totalGorduras }

        salvarConsumoLocal(
            data      = data,
            kcal      = kcal,
            proteinaG = prot,
            carboG    = carbo,
            gorduraG  = gord,
        )

        // Sincronizar com Health Connect
        try {
            if (healthRepo.isDisponivel() && healthRepo.temPermissoes()) {
                val localDate = LocalDate.parse(data)
                healthRepo.sincronizarNutricaoDia(
                    data = localDate,
                    nutricao = HealthConnectRepository.NutricaoDiaria(
                        calorias = kcal,
                        carboidratos = carbo,
                        proteinas = prot,
                        gorduras = gord
                    )
                )
                Log.d(TAG, "Sincronizado com Health Connect para o dia $data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar com Health Connect: ${e.message}")
        }

        _mudancas.emit(Unit)
    }

    suspend fun salvarConsumoLocal(
        data: String = LocalDate.now().toString(),
        kcal: Double,
        proteinaG: Double,
        carboG: Double,
        gorduraG: Double,
    ) {
        dao.inserirConsumoDiario(
            ConsumoDiarioEntity(
                data         = data,
                kcal         = kcal,
                proteinaG    = proteinaG,
                carboG       = carboG,
                gorduraG     = gorduraG,
                atualizadoEm = System.currentTimeMillis()
            )
        )
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
            data      = data,
            kcal      = atual.kcal + kcal,
            proteinaG = atual.proteinaG + proteinaG,
            carboG    = atual.carboG + carboG,
            gorduraG  = atual.gorduraG + gorduraG,
        )
    }

    suspend fun carregarConsumoLocal(data: String = LocalDate.now().toString()): ConsumoLocal =
        dao.getConsumoDiario(data)?.toConsumoLocal()
            ?: ConsumoLocal(data, 0.0, 0.0, 0.0, 0.0, 0)


    //Histórico

    suspend fun lerHistorico7Dias(): List<ConsumoLocal> {
        val hoje   = LocalDate.now()
        val datas  = (0..6).map { hoje.minusDays(it.toLong()).toString() }
        val entities = dao.getConsumosPorDatas(datas).associateBy { it.data }

        return datas.map { d ->
            entities[d]?.toConsumoLocal() ?: ConsumoLocal(d, 0.0, 0.0, 0.0, 0.0, 0)
        }.reversed()
    }

    suspend fun lerHistoricoCompleto7Dias(): List<ConsumoCompleto> {
        val hoje = LocalDate.now()
        return (0..6).map { diasAtras ->
            val data = hoje.minusDays(diasAtras.toLong()).toString()
            ConsumoCompleto(
                consumo = carregarConsumoLocal(data),
                listas = carregarListas(data).sortedByDescending { it.timestamp }
            )
        }
    }
}