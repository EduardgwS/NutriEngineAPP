package com.explosionlab.nutriengine.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.explosionlab.nutriengine.data.local.entity.AlimentoEntity
import com.explosionlab.nutriengine.data.local.entity.ConsumoDiarioEntity
import com.explosionlab.nutriengine.data.local.entity.ListaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsumoDao {

    // --- Consumo Diário Agregado ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirConsumoDiario(consumo: ConsumoDiarioEntity)

    @Query("SELECT * FROM consumo_diario WHERE data = :data")
    suspend fun getConsumoDiario(data: String): ConsumoDiarioEntity?

    @Query("SELECT * FROM consumo_diario WHERE data IN (:datas) ORDER BY data ASC")
    suspend fun getConsumosPorDatas(datas: List<String>): List<ConsumoDiarioEntity>

    // --- Listas e Alimentos (Relacional) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirLista(lista: ListaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirAlimentos(alimentos: List<AlimentoEntity>)

    @Transaction
    suspend fun salvarListaCompleta(lista: ListaEntity, alimentos: List<AlimentoEntity>) {
        inserirLista(lista)
        inserirAlimentos(alimentos)
    }

    @Query("SELECT * FROM listas WHERE data = :data ORDER BY timestamp ASC")
    suspend fun getListasPorData(data: String): List<ListaEntity>

    @Query("SELECT * FROM alimentos WHERE listaId = :listaId")
    suspend fun getAlimentosDaLista(listaId: String): List<AlimentoEntity>

    @Transaction
    @Query("DELETE FROM listas WHERE id = :listaId")
    suspend fun deletarLista(listaId: String)

    @Query("UPDATE alimentos SET quantidadeG = :novaQtd WHERE listaId = :listaId AND alimentoId = :alimentoId")
    suspend fun atualizarQuantidadeAlimento(listaId: String, alimentoId: String, novaQtd: Double)

    @Query("DELETE FROM alimentos WHERE listaId = :listaId AND alimentoId = :alimentoId")
    suspend fun removerAlimento(listaId: String, alimentoId: String)
}
