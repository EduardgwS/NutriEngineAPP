package com.explosionlab.nutriengine.repository

import android.content.Context
import android.util.Log
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
/**
 * Backup local do consumo diário em SharedPreferences.
 *
 * Garante que os totais do dia estejam disponíveis mesmo quando o
 * Health Connect não está acessível (dispositivo sem suporte, permissões
 * negadas, primeiro acesso do dia etc.).
 *
 * A fonte de verdade preferencial continua sendo o Health Connect; este
 * repositório é atualizado sempre que o HC escreve um novo registro, e
 * lido como fallback quando o HC não está disponível.
 */
class ConsumoRepository(context: Context) {

    private val TAG   = "ConsumoRepository"
    private val prefs = context.getSharedPreferences("nutricart_consumo", Context.MODE_PRIVATE)
    private val _mudancas = MutableSharedFlow<Unit>(replay = 1)
    val mudancas: SharedFlow<Unit> = _mudancas.asSharedFlow()


    private fun keyKcal(data: String)       = "consumo_${data}_kcal"
    private fun keyProteina(data: String)   = "consumo_${data}_proteina"
    private fun keyCarbo(data: String)      = "consumo_${data}_carbo"
    private fun keyGordura(data: String)    = "consumo_${data}_gordura"
    private fun keyAtualizado(data: String) = "consumo_${data}_ts"


    /**
     * Salva (substitui) os totais acumulados do dia localmente.
     * Usado quando o Health Connect devolve os totais já somados do dia.
     */
    fun salvarConsumoLocal(
        data:      String = LocalDate.now().toString(),
        kcal:      Double,
        proteinaG: Double,
        carboG:    Double,
        gorduraG:  Double,
    ) {
        prefs.edit()
            .putFloat(keyKcal(data),      kcal.toFloat())
            .putFloat(keyProteina(data),  proteinaG.toFloat())
            .putFloat(keyCarbo(data),     carboG.toFloat())
            .putFloat(keyGordura(data),   gorduraG.toFloat())
            .putLong(keyAtualizado(data), System.currentTimeMillis())
            .apply()
            _mudancas.tryEmit(Unit)
            Log.d(TAG, "Consumo local salvo e alarme disparado")
    }

    /**
     * Acumula (soma) os valores ao que já está salvo no dia.
     * Usado quando o usuário fecha uma lista de alimentos na aba Pesquisar,
     * para não sobrescrever dados que já vieram do Health Connect.
     */
    fun acumularConsumoLocal(
        data:      String = LocalDate.now().toString(),
        kcal:      Double,
        proteinaG: Double,
        carboG:    Double,
        gorduraG:  Double,
    ) {
        val atual = carregarConsumoLocal(data)
        salvarConsumoLocal(
            data      = data,
            kcal      = atual.kcal      + kcal,
            proteinaG = atual.proteinaG + proteinaG,
            carboG    = atual.carboG    + carboG,
            gorduraG  = atual.gorduraG  + gorduraG,
        )
        Log.d(TAG, "Consumo acumulado — $data: +${kcal}kcal (total: ${atual.kcal + kcal}kcal)")

    }


    data class ConsumoLocal(
        val data:         String,
        val kcal:         Double,
        val proteinaG:    Double,
        val carboG:       Double,
        val gorduraG:     Double,
        val atualizadoEm: Long,   // epoch ms; 0 = nunca salvo
    )

    /**
     * Lê os totais do dia nas SharedPreferences.
     * Retorna zeros se nunca foi salvo (dia sem registros).
     */
    fun carregarConsumoLocal(data: String = LocalDate.now().toString()): ConsumoLocal {
        return ConsumoLocal(
            data         = data,
            kcal         = prefs.getFloat(keyKcal(data),       0f).toDouble(),
            proteinaG    = prefs.getFloat(keyProteina(data),   0f).toDouble(),
            carboG       = prefs.getFloat(keyCarbo(data),      0f).toDouble(),
            gorduraG     = prefs.getFloat(keyGordura(data),    0f).toDouble(),
            atualizadoEm = prefs.getLong(keyAtualizado(data),  0L),
        )
    }

    /** True se já há algum registro local para o dia informado. */
    fun temRegistroLocal(data: String = LocalDate.now().toString()): Boolean =
        prefs.getLong(keyAtualizado(data), 0L) > 0L

    /**
     * Retorna os totais dos últimos 7 dias (do mais antigo ao mais recente).
     * Dias sem nenhum registro retornam zeros mas são incluídos para exibição completa.
     */
    fun lerHistorico7Dias(): List<ConsumoLocal> {
        val hoje = LocalDate.now()
        return (6 downTo 0).map { diasAtras ->
            carregarConsumoLocal(hoje.minusDays(diasAtras.toLong()).toString())
        }
    }
}