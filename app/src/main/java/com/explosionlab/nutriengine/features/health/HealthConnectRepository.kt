package com.explosionlab.nutriengine.features.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class HealthConnectRepository(private val context: Context) {

    companion object {
        private const val TAG = "HealthConnectRepository"
    }

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

    private fun rangeDia(data: LocalDate): TimeRangeFilter {
        val zone = ZoneId.systemDefault()
        return TimeRangeFilter.between(
            data.atStartOfDay(zone).toInstant(),
            data.plusDays(1).atStartOfDay(zone).toInstant()
        )
    }

    private fun ultimoAno(): TimeRangeFilter = TimeRangeFilter.between(
        Instant.now().minus(365, ChronoUnit.DAYS), Instant.now()
    )

    // ── Modelos ────────────────────────────────────────────────────────────────

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

    // ── Leitura — Corpo ────────────────────────────────────────────────────────

    suspend fun lerUltimoPeso(): Double? = try {
        client().readRecords(ReadRecordsRequest(WeightRecord::class, ultimoAno()))
            .records.lastOrNull()?.weight?.inKilograms
    } catch (e: Exception) { Log.e(TAG, "Peso: ${e.message}"); null }

    suspend fun lerUltimaAltura(): Double? = try {
        client().readRecords(ReadRecordsRequest(HeightRecord::class, ultimoAno()))
            .records.lastOrNull()?.height?.inMeters
    } catch (e: Exception) { Log.e(TAG, "Altura: ${e.message}"); null }

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

}
