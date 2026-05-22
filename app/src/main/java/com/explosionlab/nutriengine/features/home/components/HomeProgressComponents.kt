package com.explosionlab.nutriengine.features.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.explosionlab.nutriengine.R
import com.explosionlab.nutriengine.core.designsystem.NutriGreen

@Composable
fun ControleCard(streak: Int, semanaStatus: List<Boolean>) {
    val diasSemana = listOf("S", "T", "Q", "Q", "S", "S", "D")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF2A2A2A), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.apple_streak),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified,
                    )
                }
                Column {
                    val (label, subLabel) = when (streak) {
                        0 -> "Nenhum dia no controle" to "Que tal começar hoje?"
                        1 -> "$streak dia no controle!" to "O primeiro passo foi dado!"
                        else -> "$streak dias no controle!" to "Você está imparável!"
                    }

                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2A2A2A),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    semanaStatus.forEachIndexed { index, completo ->
                        DiaSemanaIndicador(
                            letra = diasSemana.getOrElse(index) { "" },
                            completo = completo,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaSemanaIndicador(letra: String, completo: Boolean) {
    val cor = if (completo) Color(0xFFFFD700) else Color.White.copy(alpha = 0.4f)
    val icone = if (completo) Icons.Default.CheckCircle else Icons.Default.Grain
    val tintIcone = if (completo) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(letra, style = MaterialTheme.typography.labelSmall, color = cor)
        Icon(icone, contentDescription = null, modifier = Modifier.size(18.dp), tint = tintIcone)
    }
}

@Composable
fun CaloriasProgressBar(consumido: Double, meta: Int) {
    val progresso = if (meta > 0) (consumido / meta).toFloat().coerceIn(0f, 1.2f) else 0f
    val progressoAnimado by animateFloatAsState(
        targetValue = progresso.coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "calorias",
    )
    val cor = when {
        progresso >= 1f   -> MaterialTheme.colorScheme.error
        progresso >= 0.8f -> Color(0xFFF59E0B)
        else              -> NutriGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
                    Text("Energia do Dia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "${(progresso * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = cor,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressoAnimado)
                        .fillMaxHeight()
                        .background(cor),
                )
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Consumido", style = MaterialTheme.typography.labelSmall)
                    Text("${consumido.toInt()} kcal", fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Meta Diária", style = MaterialTheme.typography.labelSmall)
                    Text("$meta kcal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
