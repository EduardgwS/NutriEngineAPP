package com.explosionlab.nutriengine.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.explosionlab.nutriengine.viewmodel.HomeViewModel
import com.explosionlab.nutriengine.viewmodel.RecomendacaoReceita
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel,
) {
    val caloriasHoje         by viewModel.caloriasHoje.collectAsStateWithLifecycle()
    val caloriasRecomendadas by viewModel.caloriasRecomendadas.collectAsStateWithLifecycle()
    val recomendacaoReceita  by viewModel.recomendacaoReceita.collectAsStateWithLifecycle()


    var receitaDetalhe by remember { mutableStateOf<RecomendacaoReceita?>(null) }
    val sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)


    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.recarregarCaloriasHome()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CaloriasProgressBar(
            caloriasHoje         = caloriasHoje,
            caloriasRecomendadas = caloriasRecomendadas,
        )

        recomendacaoReceita?.let { receita ->
            ReceitaRecomendadaCard(
                receita   = receita,
                onClick   = { receitaDetalhe = receita },
            )
        }
    }


    receitaDetalhe?.let { receita ->
        ModalBottomSheet(
            onDismissRequest = { receitaDetalhe = null },
            sheetState       = sheetState,
        ) {
            ReceitaDetalheSheet(
                receita   = receita,
                onFechar  = { receitaDetalhe = null },
            )
        }
    }
}



@Composable
private fun CaloriasProgressBar(
    caloriasHoje: Double,
    caloriasRecomendadas: Int,
) {
    val progresso = if (caloriasRecomendadas > 0)
        min(1f, (caloriasHoje / caloriasRecomendadas).toFloat())
    else 0f

    val progressoAnimado by animateFloatAsState(
        targetValue   = progresso,
        animationSpec = tween(durationMillis = 800),
        label         = "caloriasProgresso",
    )

    val corBarra = when {
        progresso >= 1f   -> MaterialTheme.colorScheme.error
        progresso >= 0.8f -> Color(0xFFF59E0B)
        else              -> MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text       = "Calorias de hoje",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text       = "${(progresso * 100).toInt()}%",
                style      = MaterialTheme.typography.labelLarge,
                color      = corBarra,
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressoAnimado)
                    .clip(RoundedCornerShape(50))
                    .background(corBarra),
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = "${caloriasHoje.toInt()} kcal ingeridas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text  = "Meta: $caloriasRecomendadas kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}



@Composable
private fun ReceitaRecomendadaCard(
    receita: RecomendacaoReceita,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text       = "Receita do dia",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                }
                Text(
                    text  = "Ver detalhes →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                )
            }

            Text(
                text       = "receita  ${receita.titulo}",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            Text(
                text  = receita.descricao,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
            )
        }
    }
}



@Composable
private fun ReceitaDetalheSheet(
    receita:  RecomendacaoReceita,
    onFechar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Cabeçalho
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Recomendação da Receita do Dia",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = "receita  ${receita.titulo}",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(onClick = onFechar) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text  = receita.descricao,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        // Ingredientes
        Text(
            text       = "Ingredientes",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            receita.ingredientes.forEachIndexed { _, ingrediente ->
                Row(
                    verticalAlignment     = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text  = ingrediente,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        HorizontalDivider()

        // Modo de preparo
        Text(
            text       = "Modo de fazer",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            receita.modoPreparo.forEachIndexed { index, passo ->
                Row(
                    verticalAlignment     = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier          = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment  = Alignment.Center,
                    ) {
                        Text(
                            text       = "${index + 1}",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        text     = passo,
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
