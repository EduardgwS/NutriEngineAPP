package com.explosionlab.nutriengine.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.explosionlab.nutriengine.repository.ConsumoRepository
import com.explosionlab.nutriengine.repository.Objetivo
import com.explosionlab.nutriengine.repository.PerfilRepository
import java.time.LocalDate

/**
 * Worker que roda em background em horários específicos, lê o estado
 * nutricional do dia e exibe a notificação mais relevante.
 *
 * O [KEY_SLOT] passado nos inputData determina qual lógica usar.
 */
class NutriCheckWorker(
    private val ctx: Context,
    params:          WorkerParameters,
) : CoroutineWorker(ctx, params) {

    companion object {
        const val KEY_SLOT = "slot"

        // IDs de notificação — um por slot, para não empilhar
        const val ID_MANHA  = 1001
        const val ID_ALMOCO = 1002
        const val ID_LANCHE = 1003
        const val ID_JANTAR = 1004
        const val ID_NOITE  = 1005
    }

    override suspend fun doWork(): Result {
        val slot        = inputData.getString(KEY_SLOT) ?: return Result.success()
        Log.d("NutriCheckWorker", "Iniciando worker para o slot: $slot")

        val perfilRepo  = PerfilRepository(ctx)
        val consumoRepo = ConsumoRepository(ctx)

        val perfil  = perfilRepo.carregarPerfil()
        val consumo = consumoRepo.carregarConsumoLocal(LocalDate.now().toString())

        // Metas de macros
        val kcalMeta  = perfil.caloriasRecomendadas.toDouble()
        Log.d("NutriCheckWorker", "Meta de calorias: $kcalMeta")

        val (pCarbo, pProt, pGord) = when (perfil.objetivo) {
            Objetivo.GANHAR_MUSCULOS      -> Triple(0.45, 0.30, 0.25)
            Objetivo.PERDER_PESO          -> Triple(0.40, 0.35, 0.25)
            Objetivo.MELHORAR_ALIMENTACAO -> Triple(0.50, 0.25, 0.25)
        }
        val protMeta  = kcalMeta * pProt  / 4.0
        val carboMeta = kcalMeta * pCarbo / 4.0

        val kcalConsumido  = consumo.kcal
        val protConsumida  = consumo.proteinaG
        val carboConsumido = consumo.carboG

        val gapKcal  = (kcalMeta  - kcalConsumido).coerceAtLeast(0.0)
        val gapProt  = (protMeta  - protConsumida).coerceAtLeast(0.0)
        val gapCarbo = (carboMeta - carboConsumido).coerceAtLeast(0.0)

        val pctKcal = if (kcalMeta > 0) kcalConsumido / kcalMeta else 0.0

        // Não notifica se perfil não está configurado
        if (kcalMeta <= 0) {
            Log.w("NutriCheckWorker", "Worker abortado: kcalMeta <= 0. Perfil preenchido?")
            return Result.success()
        }

        val (id, titulo, corpo, canal) = when (slot) {

            NotificationScheduler.SLOT_MANHA -> Quad(
                ID_MANHA,
                "Bom dia! ☀️",
                when {
                    protMeta > 0 && gapProt > protMeta * 0.8 ->
                        "Começou o dia zerado em proteína. Que tal uns ovos ou iogurte no café?"
                    else ->
                        "Hora de começar o dia bem! Registre seu café da manhã no NutriEngine."
                },
                NutriNotificationManager.CANAL_REFEICOES,
            )

            NotificationScheduler.SLOT_ALMOCO -> Quad(
                ID_ALMOCO,
                "Hora do almoço! 🍽️",
                when {
                    pctKcal < 0.15 ->
                        "Você ainda não registrou muita coisa hoje. Aproveite o almoço!"
                    gapProt > 30 ->
                        "Faltam %.0fg de proteína. Que tal frango, atum ou ovos no prato?".format(gapProt)
                    gapCarbo > 80 ->
                        "Carboidratos em falta! Um bom prato de arroz e feijão resolve."
                    else ->
                        "Não esqueça de registrar o almoço no app. Cada refeição conta!"
                },
                NutriNotificationManager.CANAL_REFEICOES,
            )

            NotificationScheduler.SLOT_LANCHE -> Quad(
                ID_LANCHE,
                "Lanche da tarde! 🍎",
                when {
                    pctKcal < 0.40 ->
                        "Você consumiu só %.0f%% da meta hoje. Bora comer alguma coisa?".format(pctKcal * 100)
                    gapProt > 25 ->
                        "Ainda faltam %.0fg de proteína. Um punhado de castanhas ou iogurte resolve!".format(gapProt)
                    else ->
                        "Que tal uma fruta ou castanhas para manter a energia até o jantar?"
                },
                NutriNotificationManager.CANAL_METAS,
            )

            NotificationScheduler.SLOT_JANTAR -> Quad(
                ID_JANTAR,
                "Hora do jantar! 🌙",
                when {
                    gapKcal < 200 && gapProt < 10 ->
                        "Você tá quase batendo a meta! Um jantar leve e fecha tudo. 💪"
                    gapProt > 20 ->
                        "Faltam %.0fg de proteína hoje. Que tal um bife acebolado ou salmão?".format(gapProt)
                    gapCarbo > 60 ->
                        "Carboidratos em falta! Uma batata doce assada ou macarrão resolvem."
                    gapKcal > 600 ->
                        "Ainda faltam %.0f kcal para a meta. Não pule o jantar!".format(gapKcal)
                    else ->
                        "Bora registrar o jantar! Cada refeição te aproxima da meta do dia."
                },
                NutriNotificationManager.CANAL_REFEICOES,
            )

            NotificationScheduler.SLOT_NOITE -> Quad(
                ID_NOITE,
                when {
                    pctKcal >= 0.90 -> "Meta batida! 🏆"
                    pctKcal >= 0.70 -> "Quase lá! 💪"
                    else            -> "Resumo do dia 📊"
                },
                when {
                    pctKcal >= 0.90 && gapProt < 10 ->
                        "Parabéns! Você bateu sua meta nutricional hoje. Continue assim amanhã!"
                    gapProt > 15 ->
                        "Faltaram %.0fg de proteína hoje. Amanhã você arrasa no café e almoço!".format(gapProt)
                    pctKcal < 0.50 ->
                        "Você consumiu só %.0f%% da meta hoje. Amanhã é um novo dia — não desiste!".format(pctKcal * 100)
                    else ->
                        "Faltaram %.0f kcal para a meta de hoje. Amanhã você chega lá!".format(gapKcal)
                },
                NutriNotificationManager.CANAL_METAS,
            )

            else -> return Result.success()
        }

        Log.d("NutriCheckWorker", "Disparando notificação: $titulo ($canal)")
        NutriNotificationManager.mostrar(ctx, id, titulo, corpo, canal)
        return Result.success()
    }

    /** Agrupa os 4 valores retornados pelo when de forma legível. */
    private data class Quad(val id: Int, val titulo: String, val corpo: String, val canal: String)
}