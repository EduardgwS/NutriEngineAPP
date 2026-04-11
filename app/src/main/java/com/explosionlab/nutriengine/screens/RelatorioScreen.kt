package com.explosionlab.nutriengine.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.explosionlab.nutriengine.repository.ConsumoRepository
import com.explosionlab.nutriengine.repository.Objetivo
import com.explosionlab.nutriengine.ui.theme.ErrorRed
import com.explosionlab.nutriengine.ui.theme.InfoBlue
import com.explosionlab.nutriengine.ui.theme.NutriGreen
import com.explosionlab.nutriengine.ui.theme.WarningOrange
import com.explosionlab.nutriengine.viewmodel.RelatorioViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import java.time.LocalDate
import java.time.format.TextStyle as JTimeTextStyle
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioScreen(
    innerPadding: PaddingValues      = PaddingValues(),
    viewModel:    RelatorioViewModel = viewModel()
) {
    val state     = viewModel.state
    val pullState = rememberPullToRefreshState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.carregarDados()
    }

    PullToRefreshBox(
        isRefreshing = state.carregando,
        onRefresh    = { viewModel.carregarDados() },
        state        = pullState,
        modifier     = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        if (state.carregando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NutriGreen)
            }
            return@PullToRefreshBox
        }

        val p = state.perfil ?: return@PullToRefreshBox

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                "Relatório",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )


            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = NutriGreen)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        p.nome.ifBlank { "Usuário" },
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        p.objetivo.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        "${p.sexo.label} · ${p.idade} anos · ${p.nivelAtividade.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }


            SecaoTitulo("Dados físicos")

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DadoCard("Peso",   if (p.peso   > 0) "%.1f kg".format(p.peso)   else "—", Modifier.weight(1f))
                DadoCard("Altura", if (p.altura > 0) "%.2f m".format(p.altura)  else "—", Modifier.weight(1f))
                DadoCard("Idade",  if (p.idade  > 0) "${p.idade} anos"           else "—", Modifier.weight(1f))
            }

            if (p.imc > 0) {
                SecaoTitulo("Índice de Massa Corporal (IMC)")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = imcCor(p.imc).copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "%.1f".format(p.imc),
                                fontSize   = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = imcCor(p.imc)
                            )
                            Text(
                                p.imcDescricao,
                                color      = imcCor(p.imc),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        ImcEscala(p.imc)
                    }
                }
            }


            if (p.gastoEnergeticoTotal > 0) {
                SecaoTitulo("Meta calórica diária")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Contas q eu peguei da net pra calcular as calorias:",
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LinhaCalculo(
                            rotulo = "TMB (Mifflin-St Jeor)",
                            valor  = "%.0f kcal/dia".format(p.tmb),
                            nota   = "Calorias em repouso absoluto"
                        )
                        LinhaCalculo(
                            rotulo = "Fator de atividade",
                            valor  = "× %.3f".format(p.nivelAtividade.fator),
                            nota   = p.nivelAtividade.label
                        )

                        HorizontalDivider()

                        LinhaCalculo(
                            rotulo   = "GET (manutenção)",
                            valor    = "${p.gastoEnergeticoTotal} kcal/dia",
                            nota     = "TMB × fator — para manter o peso atual",
                            destaque = true,
                            corValor = MaterialTheme.colorScheme.onSurface
                        )

                        if (p.ajusteKcal != 0) {
                            val sinal     = if (p.ajusteKcal > 0) "+" else ""
                            val corAjuste = if (p.ajusteKcal > 0) NutriGreen else InfoBlue
                            val textoNota = when (p.objetivo) {
                                Objetivo.PERDER_PESO     -> "Déficit para ≈ 0,5 kg/semana"
                                Objetivo.GANHAR_MUSCULOS -> "Superávit para ganho muscular"
                                else                     -> ""
                            }
                            LinhaCalculo(
                                rotulo   = "Ajuste (${p.objetivo.label})",
                                valor    = "$sinal${p.ajusteKcal} kcal",
                                nota     = textoNota,
                                corValor = corAjuste
                            )
                            HorizontalDivider()
                        }

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "A meta diária ideal",
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${p.caloriasRecomendadas} kcal",
                                fontSize   = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = NutriGreen
                            )
                        }
                    }
                }

                // ── Distribuição de macros ─────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Distribuição sugerida dos macronutrientes",
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val kcal                   = p.caloriasRecomendadas.toDouble()
                        val (pCarbo, pProt, pGord) = when (p.objetivo) {
                            Objetivo.GANHAR_MUSCULOS      -> Triple(0.45, 0.30, 0.25)
                            Objetivo.PERDER_PESO          -> Triple(0.40, 0.35, 0.25)
                            Objetivo.MELHORAR_ALIMENTACAO -> Triple(0.50, 0.25, 0.25)
                        }

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MacroCard("Carbo",    "%.0fg".format(kcal * pCarbo / 4), "%.0f%%".format(pCarbo * 100), Modifier.weight(1f))
                            MacroCard("Proteína", "%.0fg".format(kcal * pProt  / 4), "%.0f%%".format(pProt  * 100), Modifier.weight(1f))
                            MacroCard("Gordura",  "%.0fg".format(kcal * pGord  / 9), "%.0f%%".format(pGord  * 100), Modifier.weight(1f))
                        }

                        Text(
                            "Proporções otimizadas para: ${p.objetivo.label.lowercase()}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Resumo da semana ───────────────────────────────────────────────
            SecaoTitulo("Resumo da semana")

            val historico      = state.historico7Dias
            val temDadosSemana = historico.any { it.kcal > 0 }

            if (!temDadosSemana) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier            = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Nenhum consumo registrado ainda.\nCrie uma lista na aba Pesquisar para começar.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                GraficoSemanal(
                    historico            = historico,
                    caloriasRecomendadas = p.caloriasRecomendadas
                )
            }

            // ── Perfil incompleto ──────────────────────────────────────────────
            if (p.peso == 0.0 && p.altura == 0.0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier            = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📊", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Complete seu perfil para ver seus dados aqui.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Gráfico de barras semanal ─────────────────────────────────────────────────

@Composable
fun GraficoSemanal(
    historico:            List<ConsumoRepository.ConsumoLocal>,
    caloriasRecomendadas: Int,
) {
    val maxKcal    = maxOf(historico.maxOf { it.kcal }, caloriasRecomendadas.toDouble(), 1.0)
    val barColor   = NutriGreen
    val metaColor  = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textColor  = MaterialTheme.colorScheme.onSurfaceVariant
    val density    = LocalDensity.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Legenda
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendaItem(cor = barColor,  rotulo = "Consumo (kcal)")
                if (caloriasRecomendadas > 0) {
                    LegendaItem(cor = metaColor, rotulo = "Meta diária")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Gráfico de barras via Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val n          = historico.size
                val barAreaW   = size.width
                val barAreaH   = size.height - with(density) { 28.dp.toPx() } // reserva espaço p/ rótulos
                val slotW      = barAreaW / n
                val barW       = slotW * 0.55f
                val gap        = (slotW - barW) / 2f
                val cornerR    = with(density) { 4.dp.toPx() }

                historico.forEachIndexed { i, dia ->
                    val x       = i * slotW + gap
                    val ratio   = (dia.kcal / maxKcal).toFloat().coerceIn(0f, 1f)
                    val barH    = barAreaH * ratio
                    val top     = barAreaH - barH

                    // Barra de fundo (slot vazio)
                    drawRoundRect(
                        color        = emptyColor,
                        topLeft      = Offset(x, 0f),
                        size         = Size(barW, barAreaH),
                        cornerRadius = CornerRadius(cornerR)
                    )

                    // Barra de consumo
                    if (dia.kcal > 0) {
                        drawRoundRect(
                            color        = barColor,
                            topLeft      = Offset(x, top),
                            size         = Size(barW, barH),
                            cornerRadius = CornerRadius(cornerR)
                        )
                    }
                }

                // Linha da meta calórica
                if (caloriasRecomendadas > 0) {
                    val metaRatio = (caloriasRecomendadas / maxKcal).toFloat().coerceIn(0f, 1f)
                    val metaY     = barAreaH * (1f - metaRatio)
                    val dashLen   = with(density) { 6.dp.toPx() }
                    val gapLen    = with(density) { 4.dp.toPx() }
                    var x         = 0f
                    while (x < barAreaW) {
                        drawLine(
                            color       = metaColor,
                            start       = Offset(x, metaY),
                            end         = Offset(minOf(x + dashLen, barAreaW), metaY),
                            strokeWidth = with(density) { 1.5f.dp.toPx() }
                        )
                        x += dashLen + gapLen
                    }
                }
            }

            // Rótulos dos dias abaixo das barras
            Row(modifier = Modifier.fillMaxWidth()) {
                historico.forEach { dia ->
                    val data    = LocalDate.parse(dia.data)
                    val ehHoje  = data == LocalDate.now()
                    val nomeDia = data.dayOfWeek
                        .getDisplayName(JTimeTextStyle.NARROW, Locale("pt", "BR"))
                        .replaceFirstChar { it.uppercase() }
                    Column(
                        modifier            = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            nomeDia,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = if (ehHoje) FontWeight.Bold else FontWeight.Normal,
                            color      = if (ehHoje) barColor else textColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Totais da semana em chips
            val totalKcal  = historico.sumOf { it.kcal }
            val totalProt  = historico.sumOf { it.proteinaG }
            val totalCarbo = historico.sumOf { it.carboG }
            val totalGord  = historico.sumOf { it.gorduraG }

            Text(
                "Totais da semana",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TotalChip("%.0f kcal".format(totalKcal),  NutriGreen, Modifier.weight(1f))
                TotalChip("%.0fg proteína".format(totalProt), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TotalChip("%.0fg carboidratos".format(totalCarbo), MaterialTheme.colorScheme.tertiary,   Modifier.weight(1f))
                TotalChip("%.0fg gorduras".format(totalGord),  MaterialTheme.colorScheme.secondary,  Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendaItem(cor: Color, rotulo: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape    = MaterialTheme.shapes.extraSmall,
            color    = cor
        ) {}
        Spacer(Modifier.width(4.dp))
        Text(
            rotulo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TotalChip(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape    = MaterialTheme.shapes.small,
        color    = cor.copy(alpha = 0.12f)
    ) {
        Text(
            texto,
            style     = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color     = cor,
            modifier  = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines  = 1
        )
    }
}

// ── Componentes auxiliares ─────────────────────────────────────────────────────

@Composable
fun LinhaCalculo(
    rotulo:   String,
    valor:    String,
    nota:     String  = "",
    destaque: Boolean = false,
    corValor: Color   = NutriGreen,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                rotulo,
                style      = if (destaque) MaterialTheme.typography.bodyMedium
                else MaterialTheme.typography.bodySmall,
                fontWeight = if (destaque) FontWeight.SemiBold else FontWeight.Normal,
                color      = MaterialTheme.colorScheme.onSurface
            )
            if (nota.isNotEmpty()) {
                Text(
                    nota,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            valor,
            style      = if (destaque) MaterialTheme.typography.bodyMedium
            else MaterialTheme.typography.bodySmall,
            fontWeight = if (destaque) FontWeight.Bold else FontWeight.Normal,
            color      = corValor
        )
    }
}

@Composable
fun MacroCard(
    nome:     String,
    gramas:   String,
    porcento: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(nome,     style = MaterialTheme.typography.labelSmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(gramas,   style = MaterialTheme.typography.titleSmall,  fontWeight = FontWeight.ExtraBold)
            Text(porcento, style = MaterialTheme.typography.labelSmall,  color = NutriGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}


@Composable
fun imcCor(imc: Double): Color = when {
    imc < 18.5 -> InfoBlue
    imc < 25.0 -> NutriGreen
    imc < 30.0 -> WarningOrange
    else       -> ErrorRed
}

@Composable
fun ImcEscala(imc: Double) {
    Column(horizontalAlignment = Alignment.End) {
        Text("< 18.5",    color = InfoBlue,       style = MaterialTheme.typography.labelSmall)
        Text("18.5–24.9", color = NutriGreen,     style = MaterialTheme.typography.labelSmall)
        Text("25–29.9",   color = WarningOrange,  style = MaterialTheme.typography.labelSmall)
        Text("≥ 30",      color = ErrorRed,        style = MaterialTheme.typography.labelSmall)
    }
}