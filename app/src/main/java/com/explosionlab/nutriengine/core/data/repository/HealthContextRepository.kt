package com.explosionlab.nutriengine.core.data.repository

import android.content.Context
import android.util.Log
import com.explosionlab.nutriengine.features.health.HealthConnectRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.roundToInt

class HealthContextRepository(context: Context) {

    private val authRepository = AuthRepository(context)
    private val perfilRepo     = PerfilRepository(context)
    private val healthRepo     = HealthConnectRepository(context)
    private val consumoRepo    = ConsumoRepository(context)

    suspend fun montarHistoricoSaudeJson(): String? {
        return try {
            val perfil = perfilRepo.carregarPerfil(
                nomeGoogleFallback = authRepository.carregarNome()
            )

            val perfilJson = JSONObject().apply {
                if (perfil.peso   > 0) put("peso_kg",  perfil.peso)
                if (perfil.altura > 0) put("altura_m", perfil.altura)
                if (perfil.idade  > 0) put("idade",    perfil.idade)
                put("objetivo",        perfil.objetivo.label)
                put("nivel_atividade", perfil.nivelAtividade.label)
                put("sexo",            perfil.sexo.label)
                if (perfil.caloriasRecomendadas > 0)
                    put("kcal_recomendadas", perfil.caloriasRecomendadas)
            }

            val hcDisponivel = healthRepo.isDisponivel() && healthRepo.temPermissoes()
            val hoje         = LocalDate.now()

            val historicoArray = JSONArray()
            for (diasAtras in 6 downTo 0) {
                val data = hoje.minusDays(diasAtras.toLong())

                // Totais: tenta ler do Health Connect, senão usa o banco local
                val nutricao = if (hcDisponivel) {
                    val n = healthRepo.lerNutricaoDia(data)
                    if (n.calorias > 0 || n.proteinas > 0 || n.carboidratos > 0 || n.gorduras > 0) {
                        n
                    } else {
                        val local = consumoRepo.carregarConsumoLocal(data.toString())
                        HealthConnectRepository.NutricaoDiaria(
                            calorias = local.kcal,
                            proteinas = local.proteinaG,
                            carboidratos = local.carboG,
                            gorduras = local.gorduraG
                        )
                    }
                } else {
                    val local = consumoRepo.carregarConsumoLocal(data.toString())
                    HealthConnectRepository.NutricaoDiaria(
                        calorias = local.kcal,
                        proteinas = local.proteinaG,
                        carboidratos = local.carboG,
                        gorduras = local.gorduraG
                    )
                }

                // Refeições salvas manualmente no aplicativo
                val listas = consumoRepo.carregarListas(data.toString())

                if (nutricao.calorias > 0 || nutricao.proteinas > 0 || nutricao.carboidratos > 0 || nutricao.gorduras > 0 || listas.isNotEmpty()) {
                    historicoArray.put(JSONObject().apply {
                        put("data",            data.toString())
                        put("kcal_total",      nutricao.calorias.toInt())
                        put("proteinas_g",     nutricao.proteinas.round(1))
                        put("carboidratos_g",  nutricao.carboidratos.round(1))
                        put("gorduras_g",      nutricao.gorduras.round(1))

                        // Micronutrientes (se disponíveis no Health Connect)
                        if (nutricao.vitaminaC > 0)   put("vitamina_c_mg",   nutricao.vitaminaC.round(1))
                        if (nutricao.vitaminaD > 0)   put("vitamina_d_mcg",  nutricao.vitaminaD.round(1))
                        if (nutricao.vitaminaA > 0)   put("vitamina_a_mcg",  nutricao.vitaminaA.round(1))
                        if (nutricao.vitaminaB12 > 0) put("vitamina_b12_mcg",nutricao.vitaminaB12.round(1))
                        if (nutricao.calcio > 0)      put("calcio_mg",       nutricao.calcio.round(1))
                        if (nutricao.ferro > 0)       put("ferro_mg",        nutricao.ferro.round(1))
                        if (nutricao.sodio > 0)       put("sodio_mg",        nutricao.sodio.round(1))
                        if (nutricao.potassio > 0)    put("potassio_mg",     nutricao.potassio.round(1))

                        if (listas.isNotEmpty()) {
                            put("refeicoes", JSONArray().also { refArr ->
                                listas.forEach { lista ->
                                    refArr.put(JSONObject().apply {
                                        put("hora",  lista.horaTexto)
                                        put("alimentos", JSONArray().also { aArr ->
                                            lista.alimentos.forEach { a ->
                                                aArr.put(JSONObject().apply {
                                                    put("nome",           a.descricao)
                                                    put("quantidade_g",   a.quantidadeG.toInt())
                                                    put("kcal",           a.kcal.roundToInt())
                                                    put("proteinas_g",    a.proteinas.round(1))
                                                    put("carboidratos_g", a.carboidratos.round(1))
                                                    put("gorduras_g",     a.gorduras.round(1))
                                                })
                                            }
                                        })
                                    })
                                }
                            })
                        }
                    })
                }
            }

            if (historicoArray.length() == 0 && perfil.peso <= 0) return null

            JSONObject().apply {
                put("perfil",                perfilJson)
                put("historico_nutricional", historicoArray)
            }.toString()

        } catch (e: Exception) {
            Log.w("HealthContextRepository", "Falha ao montar histórico: ${e.message}")
            null
        }
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return (this * multiplier).roundToInt() / multiplier
    }
}
