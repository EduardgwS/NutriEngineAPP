package com.explosionlab.nutriengine.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Agenda um [NutriCheckWorker] por slot de horário usando WorkManager.
 *
 * Cada worker roda a cada 24 horas com um atraso inicial calculado
 * para que a primeira execução caia no horário-alvo do dia.
 */
object NotificationScheduler {

    const val SLOT_MANHA  = "manha"    // 08:00
    const val SLOT_ALMOCO = "almoco"   // 12:00
    const val SLOT_LANCHE = "lanche"   // 15:30
    const val SLOT_JANTAR = "jantar"   // 19:00
    const val SLOT_NOITE  = "noite"    // 21:30

    private val horarios = mapOf(
        SLOT_MANHA  to LocalTime.of(8,  0),
        SLOT_ALMOCO to LocalTime.of(12, 0),
        SLOT_LANCHE to LocalTime.of(15, 51),
        SLOT_JANTAR to LocalTime.of(19, 0),
        SLOT_NOITE  to LocalTime.of(21, 30),
    )

    // ── Ativar todos os slots ─────────────────────────────────────────────────

    fun ativar(context: Context, policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE) {
        val wm = WorkManager.getInstance(context)
        horarios.forEach { (slot, horario) ->
            val atraso = calcularAtraso(horario)
            val request = PeriodicWorkRequestBuilder<NutriCheckWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(atraso.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putString(NutriCheckWorker.KEY_SLOT, slot).build())
                .addTag(tagDo(slot))
                .build()

            wm.enqueueUniquePeriodicWork(
                tagDo(slot),
                policy,
                request,
            )
        }
    }

    // ── Desativar todos os slots ──────────────────────────────────────────────

    fun desativar(context: Context) {
        val wm = WorkManager.getInstance(context)
        horarios.keys.forEach { slot -> wm.cancelUniqueWork(tagDo(slot)) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun tagDo(slot: String) = "nutri_notif_$slot"

    /**
     * Calcula quanto tempo falta até o próximo [horarioAlvo].
     * Se o horário já passou hoje, retorna o tempo até amanhã no mesmo horário.
     */
    private fun calcularAtraso(horarioAlvo: LocalTime): Duration {
        val agora       = LocalDateTime.now()
        var proxExec    = agora.toLocalDate().atTime(horarioAlvo)
        if (!proxExec.isAfter(agora)) proxExec = proxExec.plusDays(1)
        return Duration.between(agora, proxExec)
    }
}