package com.explosionlab.nutriengine.repository

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class HealthRepository(private val context: Context) {

    private val TAG = "HealthRepository"


    val permissions = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getWritePermission(HeightRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
    )

    // ── Disponibilidade ────────────────────────────────────────────────────────

    fun isDisponivel(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    suspend fun temPermissoes(): Boolean = try {
        client().permissionController.getGrantedPermissions().containsAll(permissions)
    } catch (e: Exception) {
        Log.e(TAG, "Erro ao verificar permissões: ${e.message}"); false
    }

    // ── Helpers de intervalo ───────────────────────────────────────────────────

    private fun rangeDia(data: LocalDate): TimeRangeFilter = TimeRangeFilter.between(
        data.atStartOfDay().toInstant(ZoneOffset.UTC),
        data.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
    )

    private fun ultimoAno(): TimeRangeFilter = TimeRangeFilter.between(
        Instant.now().minus(365, ChronoUnit.DAYS), Instant.now()
    )

    private fun hoje(): TimeRangeFilter = rangeDia(LocalDate.now())

    // ── Dados Nutricionais ─────────────────────────────────────────────────────

    data class NutricaoDiaria(
        val carboidratos: Double = 0.0,   // g
        val proteinas:    Double = 0.0,   // g
        val gorduras:     Double = 0.0,   // g
        val calorias:     Double = 0.0,   // kcal
        val vitaminaC:    Double = 0.0,   // mg
        val vitaminaD:    Double = 0.0,   // mcg
        val vitaminaA:    Double = 0.0,   // mcg
        val vitaminaB12:  Double = 0.0,   // mcg
        val calcio:       Double = 0.0,   // mg
        val ferro:        Double = 0.0,   // mg
        val sodio:        Double = 0.0,   // mg
        val potassio:     Double = 0.0,   // mg
    )

    data class DiaNutricional(
        val data:     LocalDate,
        val nutricao: NutricaoDiaria,
    )

    // ── Leitura — Corpo ────────────────────────────────────────────────────────

    suspend fun lerUltimoPeso(): Double? = try {
        client().readRecords(ReadRecordsRequest(WeightRecord::class, ultimoAno()))
            .records.lastOrNull()?.weight?.inKilograms
    } catch (e: Exception) { Log.e(TAG, "Peso: ${e.message}"); null }

    suspend fun lerUltimaAltura(): Double? = try {
        client().readRecords(ReadRecordsRequest(HeightRecord::class, ultimoAno()))
            .records.lastOrNull()?.height?.inMeters
    } catch (e: Exception) { Log.e(TAG, "Altura: ${e.message}"); null }

    suspend fun lerCaloriasAtivasHoje(): Double = try {
        client().readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, hoje()))
            .records.sumOf { it.energy.inKilocalories }
    } catch (e: Exception) { Log.e(TAG, "Calorias ativas: ${e.message}"); 0.0 }

    // ── Leitura — Nutrição ─────────────────────────────────────────────────────

    /** Soma todos os registros de nutrição de um dia específico. */
    suspend fun lerNutricaoDia(data: LocalDate): NutricaoDiaria = try {
        client()
            .readRecords(ReadRecordsRequest(NutritionRecord::class, rangeDia(data)))
            .records
            .fold(NutricaoDiaria()) { acc, r ->
                acc.copy(
                    carboidratos = acc.carboidratos + (r.totalCarbohydrate?.inGrams ?: 0.0),
                    proteinas    = acc.proteinas    + (r.protein?.inGrams           ?: 0.0),
                    gorduras     = acc.gorduras     + (r.totalFat?.inGrams          ?: 0.0),
                    calorias     = acc.calorias     + (r.energy?.inKilocalories     ?: 0.0),
                    vitaminaC    = acc.vitaminaC    + (r.vitaminC?.inMilligrams     ?: 0.0),
                    vitaminaD    = acc.vitaminaD    + (r.vitaminD?.inMicrograms     ?: 0.0),
                    vitaminaA    = acc.vitaminaA    + (r.vitaminA?.inMicrograms     ?: 0.0),
                    vitaminaB12  = acc.vitaminaB12  + (r.vitaminB12?.inMicrograms   ?: 0.0),
                    calcio       = acc.calcio       + (r.calcium?.inMilligrams      ?: 0.0),
                    ferro        = acc.ferro        + (r.iron?.inMilligrams         ?: 0.0),
                    sodio        = acc.sodio        + (r.sodium?.inMilligrams       ?: 0.0),
                    potassio     = acc.potassio     + (r.potassium?.inMilligrams    ?: 0.0),
                )
            }
    } catch (e: Exception) {
        Log.e(TAG, "Nutrição dia $data: ${e.message}"); NutricaoDiaria()
    }

    /** Atalho para o dia atual. Delega para [lerNutricaoDia]. */
    suspend fun lerNutricaoHoje(): NutricaoDiaria = lerNutricaoDia(LocalDate.now())

    /** Retorna os totais nutricionais dos últimos 7 dias (do mais antigo ao mais recente). */
    suspend fun lerHistorico7Dias(): List<DiaNutricional> {
        val hoje = LocalDate.now()
        return (6 downTo 0).map { diasAtras ->
            val data = hoje.minusDays(diasAtras.toLong())
            DiaNutricional(data = data, nutricao = lerNutricaoDia(data))
        }
    }

    // ── Escrita — Corpo ────────────────────────────────────────────────────────

    suspend fun salvarPeso(kg: Double): Boolean = try {
        client().insertRecords(listOf(
            WeightRecord(
                weight     = Mass.kilograms(kg),
                time       = Instant.now(),
                zoneOffset = ZoneOffset.UTC,
                metadata   = Metadata.manualEntry()
            )
        ))
        true
    } catch (e: Exception) { Log.e(TAG, "Salvar peso: ${e.message}"); false }

    suspend fun salvarAltura(metros: Double): Boolean = try {
        client().insertRecords(listOf(
            HeightRecord(
                height     = Length.meters(metros),
                time       = Instant.now(),
                zoneOffset = ZoneOffset.UTC,
                metadata   = Metadata.manualEntry()
            )
        ))
        true
    } catch (e: Exception) { Log.e(TAG, "Salvar altura: ${e.message}"); false }

    // ── Escrita — Nutrição ─────────────────────────────────────────────────────

    data class EntradaNutricao(
        val carboidratos: Double? = null,
        val proteinas:    Double? = null,
        val gorduras:     Double? = null,
        val calorias:     Double? = null,
        val vitaminaC:    Double? = null,
        val vitaminaD:    Double? = null,
        val vitaminaA:    Double? = null,
        val vitaminaB12:  Double? = null,
        val calcio:       Double? = null,
        val ferro:        Double? = null,
        val sodio:        Double? = null,
        val potassio:     Double? = null,
    )

    suspend fun salvarNutricao(entrada: EntradaNutricao): Boolean = try {
        val agora = Instant.now()
        client().insertRecords(listOf(
            NutritionRecord(
                startTime           = agora.minusSeconds(1),
                endTime             = agora,
                startZoneOffset     = ZoneOffset.UTC,
                endZoneOffset       = ZoneOffset.UTC,
                metadata            = Metadata.manualEntry(),
                totalCarbohydrate   = entrada.carboidratos?.let { Mass.grams(it) },
                protein             = entrada.proteinas?.let    { Mass.grams(it) },
                totalFat            = entrada.gorduras?.let     { Mass.grams(it) },
                energy              = entrada.calorias?.let     { Energy.kilocalories(it) },
                vitaminC            = entrada.vitaminaC?.let    { Mass.milligrams(it) },
                vitaminD            = entrada.vitaminaD?.let    { Mass.micrograms(it) },
                vitaminA            = entrada.vitaminaA?.let    { Mass.micrograms(it) },
                vitaminB12          = entrada.vitaminaB12?.let  { Mass.micrograms(it) },
                calcium             = entrada.calcio?.let       { Mass.milligrams(it) },
                iron                = entrada.ferro?.let        { Mass.milligrams(it) },
                sodium              = entrada.sodio?.let        { Mass.milligrams(it) },
                potassium           = entrada.potassio?.let     { Mass.milligrams(it) },
            )
        ))
        true
    } catch (e: Exception) { Log.e(TAG, "Salvar nutrição: ${e.message}"); false }
}
