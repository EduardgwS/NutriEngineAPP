package com.explosionlab.nutriengine.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.explosionlab.nutriengine.MainActivity
import com.explosionlab.nutriengine.R

object NutriNotificationManager {

    private const val CHANNEL_REFEICOES = "nutri_refeicoes"
    private const val CHANNEL_METAS     = "nutri_metas"

    fun criarCanais(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REFEICOES,
                "Lembretes de refeição",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Lembretes de café da manhã, almoço, lanche e jantar." }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_METAS,
                "Metas e progresso",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Avisos sobre metas de proteína, carboidratos e calorias." }
        )
    }

    /**
     * Exibe uma notificação. [id] deve ser único por slot para que cada
     * horário atualize o próprio card em vez de empilhar notificações.
     */
    fun mostrar(
        context: Context,
        id:      Int,
        titulo:  String,
        corpo:   String,
        canal:   String = CHANNEL_METAS,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("NutriNotifManager", "Permissão POST_NOTIFICATIONS não concedida. Abortando.")
                return
            }
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(id, notif)
            Log.d("NutriNotifManager", "Notificação enviada: $titulo (ID: $id)")
        }.onFailure {
            Log.e("NutriNotifManager", "Erro ao enviar notificação: ${it.message}", it)
        }
    }

    // Canais públicos para uso no Worker
    val CANAL_REFEICOES get() = CHANNEL_REFEICOES
    val CANAL_METAS     get() = CHANNEL_METAS
}