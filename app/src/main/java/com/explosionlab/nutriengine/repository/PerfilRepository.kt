package com.explosionlab.nutriengine.repository

import android.content.Context
import java.time.LocalDate
import java.time.Period

enum class Objetivo(val label: String) {
    GANHAR_MUSCULOS      ("Ganhar Músculos"),
    PERDER_PESO          ("Perder Peso"),
    MELHORAR_ALIMENTACAO ("Melhorar a Alimentação"),
}

enum class Sexo(val label: String) {
    MASCULINO("Masculino"),
    FEMININO ("Feminino"),
    OUTRO    ("Outro"),
}

enum class NivelAtividade(
    val label:     String,
    val descricao: String,
    val fator:     Double
) {
    SEDENTARIO(
        label     = "Sedentário",
        descricao = "Pouco ou nenhum exercício",
        fator     = 1.2
    ),
    LEVEMENTE_ATIVO(
        label     = "Levemente ativo",
        descricao = "Exercício leve 1–3 dias/semana",
        fator     = 1.375
    ),
    MODERADAMENTE_ATIVO(
        label     = "Moderadamente ativo",
        descricao = "Exercício moderado 3–5 dias/semana",
        fator     = 1.55
    ),
    MUITO_ATIVO(
        label     = "Muito ativo",
        descricao = "Exercício intenso 6–7 dias/semana",
        fator     = 1.725
    ),
    EXTREMAMENTE_ATIVO(
        label     = "Extremamente ativo",
        descricao = "Treino pesado diário ou trabalho físico intenso",
        fator     = 1.9
    ),
}

data class Perfil(
    val nome:           String         = "",
    val altura:         Double         = 0.0,
    val peso:           Double         = 0.0,
    val dataNascimento: LocalDate?     = null,
    val sexo:           Sexo           = Sexo.MASCULINO,
    val objetivo:       Objetivo       = Objetivo.MELHORAR_ALIMENTACAO,
    val nivelAtividade: NivelAtividade = NivelAtividade.SEDENTARIO,
) {
    val idade: Int get() = dataNascimento
        ?.let { Period.between(it, LocalDate.now()).years }
        ?: 0

    val imc: Double get() = if (altura > 0) peso / (altura * altura) else 0.0

    val imcDescricao: String get() = when {
        imc < 18.5 -> "Palito"
        imc < 25.0 -> "Normal"
        imc < 30.0 -> "Ficando Gordo"
        else       -> "Majin Boo"
    }

    /** Taxa Metabólica Basal — calorias em repouso absoluto (Mifflin-St Jeor). */
    val tmb: Double get() {
        if (peso <= 0 || altura <= 0 || idade <= 0) return 0.0
        return when (sexo) {
            Sexo.MASCULINO -> 10.0 * peso + 6.25 * (altura * 100) - 5.0 * idade + 5.0
            Sexo.FEMININO  -> 10.0 * peso + 6.25 * (altura * 100) - 5.0 * idade - 161.0
            Sexo.OUTRO     -> 10.0 * peso + 6.25 * (altura * 100) - 5.0 * idade - 78.0
        }
    }

    /** Gasto Energético Total — calorias para manter o peso atual (TMB × fator de atividade). */
    val gastoEnergeticoTotal: Int get() =
        if (tmb > 0) (tmb * nivelAtividade.fator).toInt() else 0

    /** Calorias diárias recomendadas com base no objetivo. */
    val caloriasRecomendadas: Int get() = when {
        gastoEnergeticoTotal <= 0               -> 0
        objetivo == Objetivo.PERDER_PESO        -> (gastoEnergeticoTotal - 500).coerceAtLeast(1200)
        objetivo == Objetivo.GANHAR_MUSCULOS    -> gastoEnergeticoTotal + 300
        else                                    -> gastoEnergeticoTotal
    }

    /** Diferença entre a meta e o GET (positivo = superávit, negativo = déficit). */
    val ajusteKcal: Int get() = caloriasRecomendadas - gastoEnergeticoTotal
}

/**
 * Fonte de verdade local para todos os dados do perfil do usuário.
 *
 * Peso e altura são sempre persistidos aqui, independente de o Health Connect
 * estar disponível ou ter permissões. O HC é apenas uma camada de sincronização
 * opcional — o app funciona completamente sem ele.
 */
class PerfilRepository(private val context: Context) {

    private val PREFS_NAME = "nutriengine_prefs"

    companion object {
        const val KEY_PERFIL_COMPLETO  = "perfil_completo"
        const val KEY_NOME             = "perfil_nome"
        const val KEY_DATA_NASCIMENTO  = "perfil_data_nascimento"   // ISO "YYYY-MM-DD"
        const val KEY_SEXO             = "perfil_sexo"
        const val KEY_OBJETIVO         = "perfil_objetivo"
        const val KEY_NIVEL_ATIVIDADE  = "perfil_nivel_atividade"
        const val KEY_PESO             = "perfil_peso"              // Double em kg
        const val KEY_ALTURA           = "perfil_altura"            // Double em metros
    }

    private fun prefs() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun perfilCompleto(): Boolean = prefs().getBoolean(KEY_PERFIL_COMPLETO, false)


    fun carregarNome(fallback: String = ""): String =
        prefs().getString(KEY_NOME, null)?.takeIf { it.isNotBlank() } ?: fallback


    fun carregarPeso(): Double   = Double.fromBits(prefs().getLong(KEY_PESO,   0L))
    fun carregarAltura(): Double = Double.fromBits(prefs().getLong(KEY_ALTURA, 0L))

    fun salvarMedidas(peso: Double, altura: Double) {
        prefs().edit()
            .putLong(KEY_PESO,   peso.toBits())
            .putLong(KEY_ALTURA, altura.toBits())
            .apply()
    }

    // ── Perfil completo ────────────────────────────────────────────────────────

    fun salvarPerfil(
        nome:           String,
        dataNascimento: LocalDate,
        sexo:           Sexo,
        objetivo:       Objetivo,
        nivelAtividade: NivelAtividade,
        peso:           Double,
        altura:         Double,
    ) {
        prefs().edit()
            .putString(KEY_NOME,             nome.trim())
            .putString(KEY_DATA_NASCIMENTO,  dataNascimento.toString())
            .putString(KEY_SEXO,             sexo.name)
            .putString(KEY_OBJETIVO,         objetivo.name)
            .putString(KEY_NIVEL_ATIVIDADE,  nivelAtividade.name)
            .putLong(KEY_PESO,               peso.toBits())
            .putLong(KEY_ALTURA,             altura.toBits())
            .putBoolean(KEY_PERFIL_COMPLETO, true)
            .apply()
    }

    /**
     * Carrega o perfil completo das SharedPreferences locais.
     *
     * [pesoOverride] e [alturaOverride] permitem que o RelatorioViewModel
     * substitua os valores locais por leituras frescas do HC (quando disponível),
     * sem quebrar o fluxo quando o HC está ausente.
     */
    fun carregarPerfil(
        nomeGoogleFallback: String  = "",
        pesoOverride:       Double? = null,
        alturaOverride:     Double? = null,
    ): Perfil {
        val p       = prefs()
        val dataStr = p.getString(KEY_DATA_NASCIMENTO, null)

        val pesoLocal   = Double.fromBits(p.getLong(KEY_PESO,   0L))
        val alturaLocal = Double.fromBits(p.getLong(KEY_ALTURA, 0L))

        return Perfil(
            nome           = carregarNome(fallback = nomeGoogleFallback),
            peso           = pesoOverride   ?: pesoLocal,
            altura         = alturaOverride ?: alturaLocal,
            dataNascimento = dataStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            sexo           = Sexo.valueOf(
                p.getString(KEY_SEXO, Sexo.MASCULINO.name) ?: Sexo.MASCULINO.name
            ),
            objetivo       = Objetivo.valueOf(
                p.getString(KEY_OBJETIVO, Objetivo.MELHORAR_ALIMENTACAO.name)
                    ?: Objetivo.MELHORAR_ALIMENTACAO.name
            ),
            nivelAtividade = NivelAtividade.valueOf(
                p.getString(KEY_NIVEL_ATIVIDADE, NivelAtividade.SEDENTARIO.name)
                    ?: NivelAtividade.SEDENTARIO.name
            ),
        )
    }
}
