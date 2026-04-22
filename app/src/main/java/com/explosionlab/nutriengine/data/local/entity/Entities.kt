package com.explosionlab.nutriengine.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "consumo_diario")
data class ConsumoDiarioEntity(
    @PrimaryKey val data: String,
    val kcal: Double,
    val proteinaG: Double,
    val carboG: Double,
    val gorduraG: Double,
    val atualizadoEm: Long
)

@Entity(tableName = "listas", indices = [Index("data")])
data class ListaEntity(
    @PrimaryKey val id: String,
    val data: String,
    val timestamp: Long,
    val horaTexto: String
)

@Entity(
    tableName = "alimentos",
    foreignKeys = [
        ForeignKey(
            entity = ListaEntity::class,
            parentColumns = ["id"],
            childColumns = ["listaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listaId")]
)
data class AlimentoEntity(
    @PrimaryKey(autoGenerate = true) val idInterno: Long = 0,
    val listaId: String,
    val alimentoId: String,
    val descricao: String,
    val categoria: String,
    val quantidadeG: Double,
    val kcalPer100g: Double,
    val proteinasPer100g: Double,
    val carboidratosPer100g: Double,
    val gordurasPer100g: Double
)
