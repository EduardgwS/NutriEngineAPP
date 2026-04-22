package com.explosionlab.nutriengine.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KebabDining
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTimeTextStyle
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioScreen(
    innerPadding: PaddingValues      = PaddingValues(),
    viewModel:    RelatorioViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.recarregarRelatorio()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier     = Modifier.fillMaxSize().padding(innerPadding),
    ) {
        if (state.carregando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NutriGreen)
            }
            return@Box
        }

        val p = state.perfil ?: return@Box

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Relatório", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            // ── Card de perfil ─────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = NutriGreen),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(p.nome.ifBlank { "Usuário" }, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(p.objetivo.label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                    Text("${p.sexo.label} · ${p.idade} anos · ${p.nivelAtividade.label}",
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                }
            }

            // ── Dados físicos ──────────────────────────────────────────────────
            SecaoTitulo("Dados físicos")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DadoCard("Peso",   if (p.peso   > 0) "%.1f kg".format(p.peso)  else "—", Modifier.weight(1f))
                DadoCard("Altura", if (p.altura > 0) "%.2f m".format(p.altura) else "—", Modifier.weight(1f))
                DadoCard("Idade",  if (p.idade  > 0) "${p.idade} anos"          else "—", Modifier.weight(1f))
            }

            // ── IMC ────────────────────────────────────────────────────────────
            if (p.imc > 0) {
                SecaoTitulo("Índice de Massa Corporal (IMC)")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = imcCor(p.imc).copy(alpha = 0.15f)),
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("%.1f".format(p.imc), fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold, color = imcCor(p.imc))
                            Text(p.imcDescricao, color = imcCor(p.imc), fontWeight = FontWeight.Bold)
                        }
                        ImcEscala(p.imc)
                    }
                }
            }

            // ── Meta calórica ──────────────────────────────────────────────────
            if (p.gastoEnergeticoTotal > 0) {
                SecaoTitulo("Meta calórica diária")
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Contas utilizadas no cálculo de calorias diárias:", style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinhaCalculo("TMB (Mifflin-St Jeor)", "%.0f kcal/dia".format(p.tmb), "Calorias em repouso absoluto")
                        LinhaCalculo("Fator de atividade", "× %.3f".format(p.nivelAtividade.fator), p.nivelAtividade.label)
                        HorizontalDivider()
                        LinhaCalculo("GET (manutenção)", "${p.gastoEnergeticoTotal} kcal/dia",
                            "TMB × fator — para manter o peso atual", destaque = true,
                            corValor = MaterialTheme.colorScheme.onSurface)
                        if (p.ajusteKcal != 0) {
                            val sinal     = if (p.ajusteKcal > 0) "+" else ""
                            val corAjuste = if (p.ajusteKcal > 0) NutriGreen else InfoBlue
                            val textoNota = when (p.objetivo) {
                                Objetivo.PERDER_PESO     -> "Déficit para ≈ 0,5 kg/semana"
                                Objetivo.GANHAR_MUSCULOS -> "Superávit para ganho muscular"
                                else                     -> ""
                            }
                            LinhaCalculo("Ajuste (${p.objetivo.label})", "$sinal${p.ajusteKcal} kcal",
                                textoNota, corValor = corAjuste)
                            HorizontalDivider()
                        }
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("A meta diária ideal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${p.caloriasRecomendadas} kcal", fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold, color = NutriGreen)
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Distribuição sugerida dos macronutrientes", style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val kcal = p.caloriasRecomendadas.toDouble()
                        val (pCarbo, pProt, pGord) = when (p.objetivo) {
                            Objetivo.GANHAR_MUSCULOS      -> Triple(0.45, 0.30, 0.25)
                            Objetivo.PERDER_PESO          -> Triple(0.40, 0.35, 0.25)
                            Objetivo.MELHORAR_ALIMENTACAO -> Triple(0.50, 0.25, 0.25)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MacroCard("Carbo",    "%.0fg".format(kcal * pCarbo / 4), "%.0f%%".format(pCarbo * 100), Modifier.weight(1f), icon = Icons.Default.BakeryDining)
                            MacroCard("Proteína", "%.0fg".format(kcal * pProt  / 4), "%.0f%%".format(pProt  * 100), Modifier.weight(1f), icon = Icons.Default.KebabDining)
                            MacroCard("Gordura",  "%.0fg".format(kcal * pGord  / 9), "%.0f%%".format(pGord  * 100), Modifier.weight(1f), icon = Icons.Default.WaterDrop)
                        }
                        Text("Proporções otimizadas para: ${p.objetivo.label.lowercase()}.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Resumo da semana ───────────────────────────────────────────────
            SecaoTitulo("Resumo da semana")
            val historico      = state.historico7Dias
            val temDadosSemana = historico.any { it.kcal > 0 }
            if (!temDadosSemana) {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(8.dp))
                        Text("Nenhum consumo registrado ainda.\nCrie uma lista na aba Pesquisar para começar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                GraficoSemanal(historico = historico, caloriasRecomendadas = p.caloriasRecomendadas)
            }

            // ── Histórico de alimentos ─────────────────────────────────────────
            HistoricoAlimentosSection(
                historico  = state.historicoCompleto7Dias,
                onEditar   = { data, listaId, alimentoId, g -> viewModel.editarAlimento(data, listaId, alimentoId, g) },
                onRemover  = { data, listaId, alimentoId    -> viewModel.removerAlimento(data, listaId, alimentoId) },
                onRemoverLista = { data, listaId     -> viewModel.removerLista(data, listaId) },
            )

            if (p.peso == 0.0 && p.altura == 0.0) {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally) {
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

// ── Histórico de alimentos — seção principal ──────────────────────────────────

@Composable
private fun HistoricoAlimentosSection(
    historico:     List<ConsumoRepository.ConsumoCompleto>,
    onEditar:      (data: String, listaId: String, alimentoId: String, novaQuantidadeG: Double) -> Unit,
    onRemover:     (data: String, listaId: String, alimentoId: String) -> Unit,
    onRemoverLista:(data: String, listaId: String) -> Unit,
) {
    val diasComListas = historico.filter { it.listas.isNotEmpty() }
    var modoEdicao by remember { mutableStateOf(false) }

    // Cabeçalho com título e botão de modo de edição
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            "Histórico de Alimentos",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (diasComListas.isNotEmpty()) {
            FilledTonalButton(
                onClick = { modoEdicao = !modoEdicao },
                colors  = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (modoEdicao) MaterialTheme.colorScheme.errorContainer
                    else            MaterialTheme.colorScheme.secondaryContainer,
                    contentColor   = if (modoEdicao) MaterialTheme.colorScheme.onErrorContainer
                    else            MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector        = if (modoEdicao) Icons.Default.Edit else Icons.Default.Edit,
                    contentDescription = null,
                    modifier           = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (modoEdicao) "Concluir" else "Editar",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }

    if (diasComListas.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(8.dp))
                Text("Nenhum alimento registrado nos últimos 7 dias.\nUse a aba Pesquisar para adicionar refeições.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        diasComListas.forEach { consumoCompleto ->
            DiaCard(
                consumoCompleto = consumoCompleto,
                modoEdicao      = modoEdicao,
                onEditar        = onEditar,
                onRemover       = onRemover,
                onRemoverLista  = onRemoverLista,
            )
        }
    }
}

// ── Card do dia (expansível, agrupa listas) ───────────────────────────────────

@Composable
private fun DiaCard(
    consumoCompleto: ConsumoRepository.ConsumoCompleto,
    modoEdicao:      Boolean,
    onEditar:        (String, String, String, Double) -> Unit,
    onRemover:       (String, String, String) -> Unit,
    onRemoverLista:  (String, String) -> Unit,
) {
    var diaExpandido by remember(consumoCompleto.consumo.data) {
        // hoje começa expandido
        mutableStateOf(consumoCompleto.consumo.data == LocalDate.now().toString())
    }
    val rotacao by animateFloatAsState(if (diaExpandido) 180f else 0f, label = "dia")

    val data    = LocalDate.parse(consumoCompleto.consumo.data)
    val ehHoje  = data == LocalDate.now()
    val ehOntem = data == LocalDate.now().minusDays(1)
    val nomeDia = when {
        ehHoje  -> "Hoje"
        ehOntem -> "Ontem"
        else    -> data.dayOfWeek.getDisplayName(JTimeTextStyle.FULL, Locale("pt", "BR"))
            .replaceFirstChar { it.uppercase() }
    }
    val dataFormatada = data.format(DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR")))
    val totalKcalDia  = consumoCompleto.listas.sumOf { it.totalKcal }
    val nListas       = consumoCompleto.listas.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = if (ehHoje) NutriGreen.copy(alpha = 0.07f)
            else        MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column {
            // Cabeçalho do dia
            Row(
                modifier              = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(nomeDia, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (ehHoje) NutriGreen else MaterialTheme.colorScheme.onSurface)
                        Text(dataFormatada, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "$nListas ${if (nListas == 1) "lista" else "listas"} · %.0f kcal total".format(totalKcalDia),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { diaExpandido = !diaExpandido }) {
                    Icon(Icons.Default.ExpandMore, if (diaExpandido) "Recolher" else "Expandir",
                        modifier = Modifier.rotate(rotacao),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Listas do dia
            AnimatedVisibility(visible = diaExpandido) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    consumoCompleto.listas.forEachIndexed { _, lista ->
                        ListaCard(
                            data           = consumoCompleto.consumo.data,
                            lista          = lista,
                            modoEdicao     = modoEdicao,
                            onEditar       = onEditar,
                            onRemover      = onRemover,
                            onRemoverLista = onRemoverLista,
                        )
                    }
                }
            }
        }
    }
}

// ── Card de lista (expansível, mostra alimentos) ──────────────────────────────

@Composable
private fun ListaCard(
    data:           String,
    lista:          ConsumoRepository.ListaSalva,
    modoEdicao:     Boolean,
    onEditar:       (String, String, String, Double) -> Unit,
    onRemover:      (String, String, String) -> Unit,
    onRemoverLista: (String, String) -> Unit,
) {
    var expandida   by remember(lista.id) { mutableStateOf(true) }
    var confirmarEx by remember { mutableStateOf(false) }
    val rotacao     by animateFloatAsState(if (expandida) 180f else 0f, label = "lista")

    val nItens    = lista.alimentos.size
    val horaLabel = lista.horaTexto.ifBlank { "—" }

    // Diálogo de confirmação para excluir lista inteira
    if (confirmarEx) {
        AlertDialog(
            onDismissRequest = { confirmarEx = false },
            title            = { Text("Excluir lista?") },
            text             = { Text("Todos os $nItens itens desta lista serão removidos.") },
            confirmButton    = {
                TextButton(onClick = { onRemoverLista(data, lista.id); confirmarEx = false }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton    = {
                TextButton(onClick = { confirmarEx = false }) { Text("Cancelar") }
            },
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            // Cabeçalho da lista
            Row(
                modifier              = Modifier.fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint               = NutriGreen,
                            modifier           = Modifier.size(14.dp)
                        )
                        Text(
                            horaLabel,
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = NutriGreen,
                        )
                    }
                    Text(
                        "$nItens ${if (nItens == 1) "item" else "itens"} · %.0f kcal".format(lista.totalKcal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Excluir lista inteira — só visível em modo de edição
                    if (modoEdicao) {
                        IconButton(onClick = { confirmarEx = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, "Excluir lista",
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                    // Expandir / recolher — sempre visível
                    IconButton(onClick = { expandida = !expandida }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ExpandMore, if (expandida) "Recolher" else "Expandir",
                            modifier = Modifier.rotate(rotacao),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Itens da lista
            AnimatedVisibility(visible = expandida) {
                Column(
                    modifier            = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))

                    lista.alimentos.forEach { alimento ->
                        AlimentoEditavelRow(
                            alimento       = alimento,
                            modoEdicao     = modoEdicao,
                            onSalvarGramas = { g -> onEditar(data, lista.id, alimento.id, g) },
                            onRemover      = { onRemover(data, lista.id, alimento.id) },
                        )
                    }

                    // Linha de totais de macros
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MacroChipPequeno("P: %.0fg".format(lista.totalProteinas),    MaterialTheme.colorScheme.primary,   Modifier.weight(1f))
                        MacroChipPequeno("C: %.0fg".format(lista.totalCarboidratos), MaterialTheme.colorScheme.tertiary,  Modifier.weight(1f))
                        MacroChipPequeno("G: %.0fg".format(lista.totalGorduras),     MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Linha de alimento editável ────────────────────────────────────────────────

@Composable
private fun AlimentoEditavelRow(
    alimento:       ConsumoRepository.AlimentoSalvo,
    modoEdicao:     Boolean,
    onSalvarGramas: (Double) -> Unit,
    onRemover:      () -> Unit,
) {
    var editando    by remember { mutableStateOf(false) }
    var gramasInput by remember(alimento.quantidadeG) {
        mutableStateOf("%.0f".format(alimento.quantidadeG))
    }

    // Diálogo de edição de gramas
    if (editando) {
        val gramasValidas = gramasInput.replace(",", ".").toDoubleOrNull()

        AlertDialog(
            onDismissRequest = { editando = false; gramasInput = "%.0f".format(alimento.quantidadeG) },
            title            = { Text("Editar quantidade") },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(alimento.descricao, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value           = gramasInput,
                        onValueChange   = { if (it.length <= 6) gramasInput = it },
                        label           = { Text("Gramas") },
                        suffix          = { Text("g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine      = true,
                        isError         = gramasValidas == null || gramasValidas <= 0,
                    )
                    // Preview ao vivo
                    gramasValidas?.takeIf { it > 0 }?.let { g ->
                        val fator = g / 100.0
                        Text(
                            "≈ %.0f kcal · %.1fg prot · %.1fg carbo · %.1fg gord".format(
                                alimento.kcalPer100g         * fator,
                                alimento.proteinasPer100g    * fator,
                                alimento.carboidratosPer100g * fator,
                                alimento.gordurasPer100g     * fator,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = NutriGreen,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick  = {
                        gramasValidas?.takeIf { it > 0 }?.let { onSalvarGramas(it) }
                        editando = false
                    },
                    enabled = gramasValidas != null && gramasValidas > 0,
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { editando = false; gramasInput = "%.0f".format(alimento.quantidadeG) }) {
                    Text("Cancelar")
                }
            },
        )
    }

    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Gramas em destaque (substituem a descrição como elemento principal)
        Surface(
            shape = MaterialTheme.shapes.small,
            color = NutriGreen.copy(alpha = 0.12f),
        ) {
            Text(
                text       = "%.0fg".format(alimento.quantidadeG),
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = NutriGreen,
                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        // Nome do alimento (secundário)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                alimento.descricao,
                style     = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines  = 1,
            )
            Text(
                "%.0f kcal".format(alimento.kcal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Editar gramas — só visível em modo de edição
        if (modoEdicao) {
            IconButton(onClick = { editando = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, "Editar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            // Remover alimento — só visível em modo de edição
            IconButton(onClick = onRemover, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "Remover",
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MacroChipPequeno(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraSmall, color = cor.copy(alpha = 0.10f)) {
        Text(texto, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            color = cor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), maxLines = 1)
    }
}

// ── Gráfico semanal ───────────────────────────────────────────────────────────

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

    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendaItem(cor = barColor, rotulo = "Consumo (kcal)")
                if (caloriasRecomendadas > 0) LegendaItem(cor = metaColor, rotulo = "Meta diária")
            }
            Spacer(Modifier.height(16.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val n        = historico.size
                val barAreaW = size.width
                val barAreaH = size.height - with(density) { 28.dp.toPx() }
                val slotW    = barAreaW / n
                val barW     = slotW * 0.55f
                val gap      = (slotW - barW) / 2f
                val cornerR  = with(density) { 4.dp.toPx() }

                historico.forEachIndexed { i, dia ->
                    val x     = i * slotW + gap
                    val ratio = (dia.kcal / maxKcal).toFloat().coerceIn(0f, 1f)
                    val barH  = barAreaH * ratio
                    drawRoundRect(color = emptyColor, topLeft = Offset(x, 0f),
                        size = Size(barW, barAreaH), cornerRadius = CornerRadius(cornerR))
                    if (dia.kcal > 0) drawRoundRect(color = barColor, topLeft = Offset(x, barAreaH - barH),
                        size = Size(barW, barH), cornerRadius = CornerRadius(cornerR))
                }

                if (caloriasRecomendadas > 0) {
                    val metaY   = barAreaH * (1f - (caloriasRecomendadas / maxKcal).toFloat().coerceIn(0f, 1f))
                    val dashLen = with(density) { 6.dp.toPx() }
                    val gapLen  = with(density) { 4.dp.toPx() }
                    var x       = 0f
                    while (x < barAreaW) {
                        drawLine(color = metaColor, start = Offset(x, metaY),
                            end = Offset(minOf(x + dashLen, barAreaW), metaY),
                            strokeWidth = with(density) { 1.5f.dp.toPx() })
                        x += dashLen + gapLen
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                historico.forEach { dia ->
                    val d      = LocalDate.parse(dia.data)
                    val ehHoje = d == LocalDate.now()
                    val nome   = d.dayOfWeek.getDisplayName(JTimeTextStyle.NARROW, Locale("pt", "BR"))
                        .replaceFirstChar { it.uppercase() }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(nome, style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (ehHoje) FontWeight.Bold else FontWeight.Normal,
                            color = if (ehHoje) barColor else textColor)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("Totais da semana", style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TotalChip("%.0f kcal".format(historico.sumOf { it.kcal }),      NutriGreen,                               Modifier.weight(1f))
                TotalChip("%.0fg proteína".format(historico.sumOf { it.proteinaG }), MaterialTheme.colorScheme.primary,    Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TotalChip("%.0fg carboidratos".format(historico.sumOf { it.carboG }),   MaterialTheme.colorScheme.tertiary,  Modifier.weight(1f))
                TotalChip("%.0fg gorduras".format(historico.sumOf { it.gorduraG }), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            }
        }
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
private fun LegendaItem(cor: Color, rotulo: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(10.dp), shape = MaterialTheme.shapes.extraSmall, color = cor) {}
        Spacer(Modifier.width(4.dp))
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TotalChip(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = cor.copy(alpha = 0.12f)) {
        Text(texto, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            color = cor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), maxLines = 1)
    }
}

@Composable
fun LinhaCalculo(
    rotulo:   String,
    valor:    String,
    nota:     String  = "",
    destaque: Boolean = false,
    corValor: Color   = NutriGreen,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(rotulo,
                style      = if (destaque) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (destaque) FontWeight.SemiBold else FontWeight.Normal,
                color      = MaterialTheme.colorScheme.onSurface)
            if (nota.isNotEmpty()) Text(nota, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(valor,
            style      = if (destaque) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (destaque) FontWeight.Bold else FontWeight.Normal,
            color      = corValor)
    }
}

@Composable
fun MacroCard(
    nome:     String,
    gramas:   String,
    porcento: String,
    modifier: Modifier     = Modifier,
    icon:     ImageVector? = null
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = NutriGreen,
                        modifier           = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(nome, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(gramas,   style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
            Text(porcento, style = MaterialTheme.typography.labelSmall, color = NutriGreen, fontWeight = FontWeight.SemiBold)
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
        Text("< 18.5",    color = InfoBlue,      style = MaterialTheme.typography.labelSmall)
        Text("18.5–24.9", color = NutriGreen,    style = MaterialTheme.typography.labelSmall)
        Text("25–29.9",   color = WarningOrange, style = MaterialTheme.typography.labelSmall)
        Text("≥ 30",      color = ErrorRed,       style = MaterialTheme.typography.labelSmall)
    }
}