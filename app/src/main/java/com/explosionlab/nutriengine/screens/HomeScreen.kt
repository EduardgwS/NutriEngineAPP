package com.explosionlab.nutriengine.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext as CoilContext
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KebabDining
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.explosionlab.nutriengine.repository.Parceiro
import com.explosionlab.nutriengine.repository.RecomendacaoProduto
import com.explosionlab.nutriengine.ui.theme.NutriGreen
import com.explosionlab.nutriengine.viewmodel.HomeViewModel
import com.explosionlab.nutriengine.viewmodel.MacroState
import com.explosionlab.nutriengine.viewmodel.MercadoViewModel
import com.explosionlab.nutriengine.viewmodel.RecomendacaoReceita
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerPadding:    PaddingValues  = PaddingValues(),
    viewModel:       HomeViewModel,
    mercadoViewModel: MercadoViewModel,
) {
    val caloriasHoje         by viewModel.caloriasHoje.collectAsStateWithLifecycle()
    val caloriasRecomendadas by viewModel.caloriasRecomendadas.collectAsStateWithLifecycle()
    val recomendacaoReceita  by viewModel.recomendacaoReceita.collectAsStateWithLifecycle()
    val macroState           by viewModel.macroState.collectAsStateWithLifecycle()

    val recomendacoes by mercadoViewModel.recomendacoes.collectAsStateWithLifecycle()
    val parceiros     by mercadoViewModel.parceiros.collectAsStateWithLifecycle()
    val carregando    by mercadoViewModel.carregando.collectAsStateWithLifecycle()

    var receitaDetalhe  by remember { mutableStateOf<RecomendacaoReceita?>(null) }
    var produtoDetalhe  by remember { mutableStateOf<RecomendacaoProduto?>(null) }
    val receitaSheet    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val produtoSheet    = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.recarregarCaloriasHome()
                mercadoViewModel.carregarDados()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Barra de calorias ──────────────────────────────────────────────────
        CaloriasProgressBar(
            caloriasHoje         = caloriasHoje,
            caloriasRecomendadas = caloriasRecomendadas,
        )

        // ── Meta do dia — gamificação ──────────────────────────────────────────
        if (macroState.proteinaMeta > 0) {
            MetaDoDiaCard(macroState = macroState)
        }

        // ── Receita do dia ─────────────────────────────────────────────────────
        recomendacaoReceita?.let { receita ->
            ReceitaRecomendadaCard(
                receita = receita,
                onClick = { receitaDetalhe = receita },
            )
        }

        // ── Recomendações de compras ───────────────────────────────────────────
        SecaoMercado(
            recomendacoes = recomendacoes,
            parceiros     = parceiros,
            carregando    = carregando,
            onVerProduto  = { produtoDetalhe = it },
            onAtualizar   = { mercadoViewModel.carregarDados(force = true) },
        )
    }

    // ── BottomSheet da receita ─────────────────────────────────────────────────
    receitaDetalhe?.let { receita ->
        ModalBottomSheet(
            onDismissRequest = { receitaDetalhe = null },
            sheetState       = receitaSheet,
        ) {
            ReceitaDetalheSheet(receita = receita, onFechar = { receitaDetalhe = null })
        }
    }

    // ── BottomSheet do produto ─────────────────────────────────────────────────
    produtoDetalhe?.let { produto ->
        val context = LocalContext.current
        ModalBottomSheet(
            onDismissRequest = { produtoDetalhe = null },
            sheetState       = produtoSheet,
        ) {
            ProdutoDetalheSheet(
                produto  = produto,
                onFechar = { produtoDetalhe = null },
                onComprar = {
                    mercadoViewModel.abrirProduto(produto.urlCompra, context)
                    produtoDetalhe = null
                },
            )
        }
    }
}

// ── Seção de mercado ──────────────────────────────────────────────────────────

@Composable
private fun SecaoMercado(
    recomendacoes: List<RecomendacaoProduto>,
    parceiros:     List<Parceiro>,
    carregando:    Boolean,
    onVerProduto:  (RecomendacaoProduto) -> Unit,
    onAtualizar:   () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Cabeçalho
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.ShoppingCart, null,
                    tint = NutriGreen, modifier = Modifier.size(18.dp))
                Text("Compras recomendadas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick  = onAtualizar,
                enabled  = !carregando,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Default.Refresh, "Atualizar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
        }

        when {
            carregando -> {
                Box(
                    modifier         = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NutriGreen, modifier = Modifier.size(32.dp))
                }
            }

            recomendacoes.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                ) {
                    Column(
                        modifier            = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Nenhuma recomendação disponível no momento.",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            else -> {
                // Motivo da recomendação (vem do primeiro item como texto geral)
                val motivo = recomendacoes.firstOrNull()?.motivo.orEmpty()
                if (motivo.isNotBlank()) {
                    Text(
                        motivo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Carrossel horizontal de produtos
                Row(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    recomendacoes.forEach { produto ->
                        CardProduto(
                            produto      = produto,
                            onVerDetalhe = { onVerProduto(produto) },
                        )
                    }
                }
            }
        }

        // Chips de parceiros
        if (parceiros.isNotEmpty()) {
            Text(
                "Mercados parceiros",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val context = LocalContext.current
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                parceiros.forEach { parceiro ->
                    AssistChip(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(parceiro.siteUrl))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        label = { Text(parceiro.nome, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
    }
}

// ── Card de produto no carrossel ──────────────────────────────────────────────

@Composable
private fun CardProduto(
    produto:      RecomendacaoProduto,
    onVerDetalhe: () -> Unit,
) {
    val temDesconto = produto.precoAntigo != null
    val context     = CoilContext.current

    Card(
        modifier = Modifier
            .width(148.dp)
            .clickable { onVerDetalhe() },
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // Imagem do produto
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (produto.imagemUrl.isNotBlank()) {
                    AsyncImage(
                        model             = ImageRequest.Builder(context)
                            .data(produto.imagemUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = produto.nome,
                        modifier           = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                }

                // Badge de desconto sobre a imagem
                if (temDesconto) {
                    val pct = ((1.0 - produto.precoAtual / produto.precoAntigo!!) * 100).toInt()
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.error)
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text("-$pct%", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Informações textuais
            Column(modifier = Modifier.padding(10.dp)) {

                // Nome do produto
                Text(
                    produto.nome,
                    style     = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis,
                )

                // Marca + quantidade
                Text(
                    buildString {
                        if (produto.marca.isNotBlank()) append("${produto.marca} · ")
                        append("%.0fg".format(produto.quantidadeG))
                    },
                    style   = MaterialTheme.typography.labelSmall,
                    color   = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )

                Spacer(Modifier.height(6.dp))

                // Preço
                if (temDesconto) {
                    Text(
                        "R$ %.2f".format(produto.precoAntigo!!).replace(".", ","),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                    )
                }
                Text(
                    "R$ %.2f".format(produto.precoAtual).replace(".", ","),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color      = NutriGreen,
                )

                Spacer(Modifier.height(4.dp))

                // Mercado
                if (produto.nomeMercado.isNotBlank()) {
                    Text(
                        produto.nomeMercado,
                        style   = MaterialTheme.typography.labelSmall,
                        color   = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            } // fecha Column de informações textuais
        } // fecha Column externo
    } // fecha Card
}

// ── Meta do dia — gamificação ─────────────────────────────────────────────────

private data class DicaNutricional(
    val icone:    ImageVector,
    val titulo:   String,
    val corpo:    String,
    val corTema:  androidx.compose.ui.graphics.Color? = null,
)

@Composable
private fun MetaDoDiaCard(macroState: MacroState) {

    // Sugestões por macro deficitário
    val dicasProteina = listOf(
        DicaNutricional(Icons.Default.Restaurant, "Falta proteína!", "Que tal um bife acebolado no almoço?\nSeu músculo vai agradecer."),
        DicaNutricional(Icons.Default.KebabDining, "Proteína em falta!", "Uns ovos mexidos agora resolvem o problema e ficam prontos em 5 minutos."),
        DicaNutricional(Icons.Default.SetMeal, "Tá faltando proteína!", "Um atum com torrada é rápido, barato e resolve o déficit do dia."),
    )

    val dicasCarbo = listOf(
        DicaNutricional(Icons.Default.Grain, "Energia no chinelo!", "Você tá devendo carboidrato. Uma porção de arroz ou macarrão resolve."),
        DicaNutricional(Icons.Default.BakeryDining, "Carboidrato em falta.", "Batata doce assada é uma boa opção — saudável e muito fácil de fazer."),
    )

    val dicasGordura = listOf(
        DicaNutricional(Icons.Default.Opacity, "Gordura do bem em falta!", "Um abacate no lanche ou salada resolve. E é gostoso demais."),
        DicaNutricional(Icons.Default.WaterDrop, "Falta gordura na dieta!", "Drizzle de azeite na comida ou umas nozes no lanche já resolvem."),
    )

    val dicaMetaBatida = listOf(
        DicaNutricional(Icons.Default.CheckCircle, "Missão cumprida!", "Você bateu a meta de macros de hoje. Bora repetir amanhã?"),
        DicaNutricional(Icons.Default.Stars, "Parabéns!", "Macros no verde hoje. Sua alimentação tá no caminho certo!"),
    )

    val dica: DicaNutricional = remember(macroState.maiorDeficit, macroState.proteinaConsumida) {
        val seed = (macroState.proteinaConsumida * 10).toInt()
        when (macroState.maiorDeficit) {
            0    -> dicasProteina[seed % dicasProteina.size]
            1    -> dicasCarbo[seed    % dicasCarbo.size]
            2    -> dicasGordura[seed  % dicasGordura.size]
            else -> dicaMetaBatida[seed % dicaMetaBatida.size]
        }
    }

    val isMetaBatida = macroState.maiorDeficit == -1

    val corContainer = if (isMetaBatida)
        NutriGreen.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)

    val corTexto = if (isMetaBatida)
        NutriGreen
    else
        MaterialTheme.colorScheme.onTertiaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = corContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Título
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = dica.icone,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = corTexto
                )
                Text(
                    dica.titulo,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = corTexto,
                )
            }

            // Corpo
            Text(
                dica.corpo,
                style = MaterialTheme.typography.bodySmall,
                color = corTexto.copy(alpha = 0.85f),
            )

            // Barrinhas de macros
            MacroBarra(
                label    = "Proteína",
                consumido = macroState.proteinaConsumida,
                meta      = macroState.proteinaMeta,
                cor       = MaterialTheme.colorScheme.primary,
            )
            MacroBarra(
                label    = "Carboidrato",
                consumido = macroState.carboConsumido,
                meta      = macroState.carboMeta,
                cor       = MaterialTheme.colorScheme.tertiary,
            )
            MacroBarra(
                label    = "Gordura",
                consumido = macroState.gorduraConsumida,
                meta      = macroState.gorduraMeta,
                cor       = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun MacroBarra(
    label:    String,
    consumido: Double,
    meta:      Double,
    cor:       androidx.compose.ui.graphics.Color,
) {
    if (meta <= 0) return
    val progresso by animateFloatAsState(
        targetValue   = (consumido / meta).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label         = "macro_$label",
    )

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "%.0fg / %.0fg".format(consumido, meta),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                    .fillMaxHeight()
                    .fillMaxWidth(progresso)
                    .clip(RoundedCornerShape(50))
                    .background(if (progresso >= 1f) NutriGreen else cor),
            )
        }
    }
}

// ── BottomSheet de detalhe do produto ────────────────────────────────────────

@Composable
fun ProdutoDetalheSheet(
    produto:  RecomendacaoProduto,
    onFechar: () -> Unit,
    onComprar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Cabeçalho
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(produto.nome, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                if (produto.marca.isNotBlank()) {
                    Text(produto.marca, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onFechar) {
                Icon(Icons.Default.Close, "Fechar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Imagem do produto
        if (produto.imagemUrl.isNotBlank()) {
            val context = CoilContext.current
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model              = ImageRequest.Builder(context)
                        .data(produto.imagemUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = produto.nome,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = androidx.compose.ui.layout.ContentScale.Fit,
                )
            }
        }

        // Motivo da recomendação
        if (produto.motivo.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NutriGreen.copy(alpha = 0.10f),
            ) {
                Text(
                    produto.motivo,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = NutriGreen,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        HorizontalDivider()

        // Macros da embalagem
        Text("Informação nutricional (%.0fg)".format(produto.quantidadeG),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DadoCard("Energia",  "%.0f kcal".format(produto.kcal),         Modifier.weight(1f), icon = Icons.Default.Bolt)
            DadoCard("Proteína", "%.1f g".format(produto.proteinas),       Modifier.weight(1f), icon = Icons.Default.KebabDining)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DadoCard("Carbo",    "%.1f g".format(produto.carboidratos),    Modifier.weight(1f), icon = Icons.Default.BakeryDining)
            DadoCard("Gordura",  "%.1f g".format(produto.gorduras),        Modifier.weight(1f), icon = Icons.Default.WaterDrop)
        }

        HorizontalDivider()

        // Preço + mercado
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                if (produto.precoAntigo != null) {
                    Text(
                        "R$ %.2f".format(produto.precoAntigo).replace(".", ","),
                        style          = MaterialTheme.typography.bodySmall,
                        color          = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                    )
                }
                Text(
                    "R$ %.2f".format(produto.precoAtual).replace(".", ","),
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color      = NutriGreen,
                )
            }
            if (produto.nomeMercado.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        produto.nomeMercado,
                        style    = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color    = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        // Botão de compra
        Button(
            onClick   = onComprar,
            modifier  = Modifier.fillMaxWidth().height(52.dp),
            shape     = RoundedCornerShape(28.dp),
            colors    = ButtonDefaults.buttonColors(containerColor = NutriGreen),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        ) {
            Icon(Icons.Default.OpenInNew, null, tint = Color.White,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ver no mercado", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Componentes existentes mantidos ──────────────────────────────────────────

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
            Text("Calorias de hoje", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text("${(progresso * 100).toInt()}%", style = MaterialTheme.typography.labelLarge,
                color = corBarra, fontWeight = FontWeight.Bold)
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
            Text("${caloriasHoje.toInt()} kcal ingeridas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Meta: $caloriasRecomendadas kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Receita do dia", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                Text("Ver detalhes/", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
            }
            Text(receita.titulo, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(receita.descricao, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f))
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
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Recomendação da Receita do Dia",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(receita.titulo, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onFechar) {
                Icon(Icons.Default.Close, "Fechar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(receita.descricao, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider()

        Text("Ingredientes", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            receita.ingredientes.forEach { ingrediente ->
                Row(verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.padding(top = 6.dp).size(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary))
                    Text(ingrediente, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        HorizontalDivider()

        Text("Modo de fazer", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            receita.modoPreparo.forEachIndexed { index, passo ->
                Row(verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text(passo, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f))
                }
            }
        }
    }
}