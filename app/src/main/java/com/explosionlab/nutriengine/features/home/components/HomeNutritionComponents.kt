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
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.KebabDining
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.explosionlab.nutriengine.core.designsystem.NutriGreen
import com.explosionlab.nutriengine.core.model.DicaMacro
import com.explosionlab.nutriengine.core.model.RecomendacaoReceita
import com.explosionlab.nutriengine.features.home.MacroState

@Composable
fun MetaDoDiaCard(macroState: MacroState, dicaMacro: DicaMacro?) {
    val metaBatida = macroState.maiorDeficit == -1
    val cor = if (metaBatida) NutriGreen else MaterialTheme.colorScheme.onTertiaryContainer
    val corFundo = if (metaBatida) {
        NutriGreen.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = corFundo),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = dicaMacro?.icone?.resolverIcone() ?: defaultIcone(metaBatida),
                    contentDescription = null,
                    tint = cor,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    dicaMacro?.titulo ?: "Calculando...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = cor,
                )
            }

            Text(
                dicaMacro?.corpo ?: "Analisando seu consumo...",
                style = MaterialTheme.typography.bodySmall,
                color = cor.copy(alpha = 0.85f),
            )

            MacroBarra("Proteína", macroState.proteinaConsumida, macroState.proteinaMeta, MaterialTheme.colorScheme.primary)
            MacroBarra("Carboidrato", macroState.carboConsumido, macroState.carboMeta, MaterialTheme.colorScheme.tertiary)
            MacroBarra("Gordura", macroState.gorduraConsumida, macroState.gorduraMeta, MaterialTheme.colorScheme.secondary)
        }
    }
}

private fun String.resolverIcone(): ImageVector? = when (this) {
    "restaurant"    -> Icons.Default.Restaurant
    "kebab_dining"  -> Icons.Default.KebabDining
    "set_meal"      -> Icons.Default.SetMeal
    "grain"         -> Icons.Default.Grain
    "bakery_dining" -> Icons.Default.BakeryDining
    "opacity"       -> Icons.Default.Opacity
    "water_drop"    -> Icons.Default.WaterDrop
    "check_circle"  -> Icons.Default.CheckCircle
    "stars"         -> Icons.Default.Stars
    else            -> null
}

private fun defaultIcone(metaBatida: Boolean): ImageVector =
    if (metaBatida) Icons.Default.CheckCircle else Icons.Default.Lightbulb

@Composable
private fun MacroBarra(label: String, consumido: Double, meta: Double, cor: Color) {
    if (meta <= 0) return
    val progresso by animateFloatAsState(
        targetValue = (consumido / meta).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = label,
    )
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("%.0fg / %.0fg".format(consumido, meta), style = MaterialTheme.typography.labelSmall)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progresso)
                    .fillMaxHeight()
                    .background(if (progresso >= 1f) NutriGreen else cor),
            )
        }
    }
}

@Composable
fun ReceitaRecomendadaCard(receita: RecomendacaoReceita, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Receita do dia",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
            Text(
                receita.titulo.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                receita.descricao.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
