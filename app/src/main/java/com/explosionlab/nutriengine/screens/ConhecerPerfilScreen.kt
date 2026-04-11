package com.explosionlab.nutriengine.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.explosionlab.nutriengine.repository.NivelAtividade
import com.explosionlab.nutriengine.repository.Objetivo
import com.explosionlab.nutriengine.repository.Sexo
import com.explosionlab.nutriengine.ui.theme.LightGreenContainer
import com.explosionlab.nutriengine.ui.theme.NutriGreen
import com.explosionlab.nutriengine.ui.theme.SuccessGreen
import com.explosionlab.nutriengine.viewmodel.PerfilViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConhecerPerfilScreen(
    onConcluido: () -> Unit,
    isEdicao:    Boolean        = false,
    onVoltar:    () -> Unit     = onConcluido,
    viewModel:   PerfilViewModel = viewModel()
) {
    val state = viewModel.state

    LaunchedEffect(state.concluido) { if (state.concluido) onConcluido() }

    var mostrarDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.dataNascimento
            ?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli()
            ?: LocalDate.now().minusYears(25)
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )

    if (mostrarDatePicker) {
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val data = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        viewModel.onDataNascimentoChange(data)
                    }
                    mostrarDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }


    if (isEdicao) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Editar Perfil", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onVoltar) {
                            Icon(
                                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NutriGreen,
                        titleContentColor        = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                )
            }
        ) { innerPadding ->
            PerfilFormulario(
                state             = state,
                viewModel         = viewModel,
                mostrarDatePicker = { mostrarDatePicker = true },
                isEdicao          = true,
                modifier          = Modifier.padding(innerPadding),
            )
        }
    } else {
        // ── Loading enquanto lê o Health Connect ───────────────────────────────
        if (state.isCarregando) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NutriGreen)
                    Spacer(Modifier.height(12.dp))
                    Text("Buscando dados do Health Connect…", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            PerfilFormulario(
                state             = state,
                viewModel         = viewModel,
                mostrarDatePicker = { mostrarDatePicker = true },
                isEdicao          = false,
            )
        }
    }
}

// ── Formulário compartilhado ───────────────────────────────────────────────

@Composable
private fun PerfilFormulario(
    state:             com.explosionlab.nutriengine.viewmodel.PerfilUiState,
    viewModel:         PerfilViewModel,
    mostrarDatePicker: () -> Unit,
    isEdicao:          Boolean,
    modifier:          Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!isEdicao) {
            Text(
                "Vamos conhecer você",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color      = NutriGreen,
            )
            Text(
                "Preencha seus dados para calcularmos suas metas personalizadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text("Nome", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value           = state.nomeInput,
            onValueChange   = { viewModel.onNomeChange(it) },
            label           = { Text("Nome completo") },
            placeholder     = { Text("Seu nome") },
            modifier        = Modifier.fillMaxWidth(),
            leadingIcon     = { Icon(Icons.Default.Person, contentDescription = "Nome") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine      = true,
        )

        HorizontalDivider()

        Text("Medidas corporais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (state.preenchidoPeloHC) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LightGreenContainer,
            ) {
                Text(
                    "✅ Dados importados do Health Connect. Confirme ou ajuste se necessário.",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = SuccessGreen,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value           = state.alturaInput,
                onValueChange   = { viewModel.onAlturaChange(it) },
                label           = { Text("Altura (m)") },
                placeholder     = { Text("1.75") },
                modifier        = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine      = true,
            )
            OutlinedTextField(
                value           = state.pesoInput,
                onValueChange   = { viewModel.onPesoChange(it) },
                label           = { Text("Peso (kg)") },
                placeholder     = { Text("70.0") },
                modifier        = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine      = true,
            )
        }

        // ── Data de nascimento ─────────────────────────────────────────────────
        Text("Data de nascimento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value         = state.dataNascimento
                ?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
            onValueChange = { },
            label         = { Text("Data de nascimento") },
            placeholder   = { Text("dd/mm/aaaa") },
            modifier      = Modifier.fillMaxWidth(),
            readOnly      = true,
            trailingIcon  = {
                IconButton(onClick = mostrarDatePicker) {
                    Icon(Icons.Default.CalendarMonth, "Selecionar data")
                }
            },
        )

        state.dataNascimento?.let { data ->
            val idade = Period.between(data, LocalDate.now()).years
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Idade calculada: $idade anos",
                    style      = MaterialTheme.typography.bodySmall,
                    color      = NutriGreen,
                    fontWeight = FontWeight.Bold,
                )
                if (state.dataVeioDoGoogle) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            "🔵 Google",
                            style    = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color    = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Text("Sexo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sexo.entries.forEach { sexo ->
                FilterChip(
                    selected = state.sexo == sexo,
                    onClick  = { viewModel.onSexoChange(sexo) },
                    label    = { Text(sexo.label) },
                    modifier = Modifier.weight(1f),
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NutriGreen,
                        selectedLabelColor     = Color.White,
                    ),
                )
            }
        }

        HorizontalDivider()

        // ── Nível de atividade ─────────────────────────────────────────────────
        Text("Nível de atividade física", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Usado para calcular suas calorias diárias com a fórmula de Mifflin-St Jeor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NivelAtividade.entries.forEach { nivel ->
                val selecionado = state.nivelAtividade == nivel
                Card(
                    onClick  = { viewModel.onNivelAtividadeChange(nivel) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (selecionado) NutriGreen
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                nivel.label,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (selecionado) Color.White
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                nivel.descricao,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selecionado) Color.White.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "×%.3f".format(nivel.fator),
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color      = if (selecionado) Color.White.copy(alpha = 0.9f) else NutriGreen,
                        )
                        if (selecionado) {
                            Spacer(Modifier.width(8.dp))
                            Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Objetivo ───────────────────────────────────────────────────────────
        Text("Qual é o seu objetivo?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Objetivo.entries.forEach { objetivo ->
                val selecionado = state.objetivo == objetivo
                Card(
                    onClick  = { viewModel.onObjetivoChange(objetivo) },
                    colors   = CardDefaults.cardColors(
                        containerColor = if (selecionado) NutriGreen
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically,
                    ) {
                        Text(
                            text = when (objetivo) {
                                Objetivo.GANHAR_MUSCULOS      -> objetivo.label
                                Objetivo.PERDER_PESO          -> objetivo.label
                                Objetivo.MELHORAR_ALIMENTACAO -> objetivo.label
                            },
                            color      = if (selecionado) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                        )
                        if (selecionado) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (state.erro.isNotEmpty()) {
            Text(
                state.erro,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick  = { viewModel.salvar() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled  = !state.isCarregando,
            colors   = ButtonDefaults.buttonColors(containerColor = NutriGreen),
        ) {
            if (state.isCarregando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text(
                    text       = if (isEdicao) "Salvar alterações" else "Salvar e continuar",
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
