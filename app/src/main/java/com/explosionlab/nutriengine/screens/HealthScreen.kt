package com.explosionlab.nutriengine.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.explosionlab.nutriengine.repository.HealthRepository
import com.explosionlab.nutriengine.ui.theme.LightGreenContainer
import com.explosionlab.nutriengine.ui.theme.NutriGreen
import com.explosionlab.nutriengine.ui.theme.SuccessGreen
import com.explosionlab.nutriengine.viewmodel.HealthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    onVoltar:  () -> Unit,
    viewModel: HealthViewModel = viewModel()
) {
    val state = viewModel.state

    val permissaoLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.verificarPermissoes()
        if (viewModel.state.temPermissoes) viewModel.carregarDados()
    }

    LaunchedEffect(Unit) {
        viewModel.verificarPermissoes()
        if (viewModel.state.temPermissoes) viewModel.carregarDados()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Testes do Health Connect") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = NutriGreen,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            if (!state.hcDisponivel) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Health Connect não encontrado",
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Instale o app Health Connect da Google Play.",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                return@Column
            }


            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.temPermissoes) LightGreenContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (state.temPermissoes) "Permissões corretas"
                            else "Permissões não concedidas",
                            fontWeight = FontWeight.Bold
                        )
                        if (!state.temPermissoes) {
                            Text(
                                "Peso, altura, calorias e nutrição",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!state.temPermissoes) {
                        Button(onClick = { permissaoLauncher.launch(viewModel.permissions) }) {
                            Text("Permitir")
                        }
                    }
                }
            }

            if (!state.temPermissoes) return@Column

            // ── Resumo do dia ──────────────────────────────────────────────────
            SecaoTitulo("Resumo de hoje")

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DadoCard("Cal. ativas", "%.0f kcal".format(state.caloriasAtivas), Modifier.weight(1f))
                DadoCard("Peso",  state.peso?.let   { "%.1f kg".format(it) } ?: "—", Modifier.weight(1f))
                DadoCard("Altura",state.altura?.let { "%.2f m".format(it)  } ?: "—", Modifier.weight(1f))
            }

            // ── Macros do dia ──────────────────────────────────────────────────
            SecaoTitulo("Macronutrientes hoje")

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DadoCard("Carbo",  "%.1fg".format(state.nutricao.carboidratos), Modifier.weight(1f))
                DadoCard("Prot.",  "%.1fg".format(state.nutricao.proteinas),    Modifier.weight(1f))
                DadoCard("Gord.",  "%.1fg".format(state.nutricao.gorduras),     Modifier.weight(1f))
                DadoCard("⚡ Kcal",   "%.0f".format(state.nutricao.calorias),   Modifier.weight(1f))
            }

            // ── Vitaminas do dia ───────────────────────────────────────────────
            SecaoTitulo("Vitaminas hoje")

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DadoCard("C",   "%.1fmg".format(state.nutricao.vitaminaC),    Modifier.weight(1f))
                DadoCard("D",   "%.1fmcg".format(state.nutricao.vitaminaD),   Modifier.weight(1f))
                DadoCard("A",   "%.1fmcg".format(state.nutricao.vitaminaA),   Modifier.weight(1f))
                DadoCard("B12", "%.1fmcg".format(state.nutricao.vitaminaB12), Modifier.weight(1f))
            }

            // ── Minerais do dia ────────────────────────────────────────────────
            SecaoTitulo("Minerais hoje")

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DadoCard("Ca",  "%.0fmg".format(state.nutricao.calcio),   Modifier.weight(1f))
                DadoCard("Fe",  "%.1fmg".format(state.nutricao.ferro),    Modifier.weight(1f))
                DadoCard("Na",  "%.0fmg".format(state.nutricao.sodio),    Modifier.weight(1f))
                DadoCard("K",   "%.0fmg".format(state.nutricao.potassio), Modifier.weight(1f))
            }

            Button(
                onClick  = { viewModel.carregarDados() },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !state.isCarregando,
                colors   = ButtonDefaults.buttonColors(containerColor = NutriGreen)
            ) {
                if (state.isCarregando)
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Atualizar dados")
            }

            HorizontalDivider()

            // ── Registrar corpo ────────────────────────────────────────────────
            SecaoRegistro(
                titulo      = "Registrar peso (kg)",
                placeholder = "Ex: 72.5",
                onSalvar    = { viewModel.salvarPeso(it) }
            )

            SecaoRegistro(
                titulo      = "Registrar altura (metros)",
                placeholder = "Ex: 1.75",
                onSalvar    = { viewModel.salvarAltura(it) }
            )

            HorizontalDivider()

            // ── Registrar nutrição ─────────────────────────────────────────────
            SecaoTitulo("Registrar nutrição")

            FormularioNutricao(
                isCarregando = state.isCarregando,
                onSalvar     = { viewModel.salvarNutricao(it) }
            )

            // ── Feedback ───────────────────────────────────────────────────────
            if (state.mensagemOk.isNotEmpty())
                Text(state.mensagemOk, color = SuccessGreen, fontWeight = FontWeight.Bold)
            if (state.mensagemErro.isNotEmpty())
                Text(state.mensagemErro, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Componentes auxiliares ─────────────────────────────────────────────────────

@Composable
fun SecaoTitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
fun SecaoRegistro(titulo: String, placeholder: String, onSalvar: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    Text(titulo, style = MaterialTheme.typography.titleSmall)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value           = input,
            onValueChange   = { input = it },
            label           = { Text(placeholder) },
            modifier        = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine      = true
        )
        Button(
            onClick  = { if (input.isNotBlank()) { onSalvar(input); input = "" } },
            colors   = ButtonDefaults.buttonColors(containerColor = NutriGreen),
            enabled  = input.isNotBlank()
        ) { Text("Salvar") }
    }
}

@Composable
fun FormularioNutricao(
    isCarregando: Boolean,
    onSalvar:     (HealthRepository.EntradaNutricao) -> Unit
) {
    var carboidratos by remember { mutableStateOf("") }
    var proteinas    by remember { mutableStateOf("") }
    var gorduras     by remember { mutableStateOf("") }
    var calorias     by remember { mutableStateOf("") }
    var vitaminaC    by remember { mutableStateOf("") }
    var vitaminaD    by remember { mutableStateOf("") }
    var vitaminaA    by remember { mutableStateOf("") }
    var vitaminaB12  by remember { mutableStateOf("") }
    var calcio       by remember { mutableStateOf("") }
    var ferro        by remember { mutableStateOf("") }
    var sodio        by remember { mutableStateOf("") }
    var potassio     by remember { mutableStateOf("") }

    val decimalOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    Text("Macros (g)", style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoNutri("Carbo (g)", carboidratos, { carboidratos = it }, Modifier.weight(1f), decimalOptions)
        CampoNutri("Prot. (g)", proteinas,    { proteinas    = it }, Modifier.weight(1f), decimalOptions)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoNutri("Gord. (g)", gorduras,     { gorduras     = it }, Modifier.weight(1f), decimalOptions)
        CampoNutri("Kcal",      calorias,     { calorias     = it }, Modifier.weight(1f), decimalOptions)
    }

    Spacer(Modifier.height(4.dp))

    Text("Vitaminas", style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoNutri("Vit. C (mg)",    vitaminaC,   { vitaminaC   = it }, Modifier.weight(1f), decimalOptions)
        CampoNutri("Vit. D (mcg)",   vitaminaD,   { vitaminaD   = it }, Modifier.weight(1f), decimalOptions)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoNutri("Vit. A (mcg)",   vitaminaA,   { vitaminaA   = it }, Modifier.weight(1f), decimalOptions)
        CampoNutri("Vit. B12 (mcg)", vitaminaB12, { vitaminaB12 = it }, Modifier.weight(1f), decimalOptions)
    }

    Spacer(Modifier.height(4.dp))

    Text("Minerais", style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoNutri("Cálcio (mg)",   calcio,   { calcio   = it }, Modifier.weight(1f), decimalOptions)
        CampoNutri("Ferro (mg)",    ferro,    { ferro    = it }, Modifier.weight(1f), decimalOptions)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoNutri("Sódio (mg)",    sodio,    { sodio    = it }, Modifier.weight(1f), decimalOptions)
        CampoNutri("Potássio (mg)", potassio, { potassio = it }, Modifier.weight(1f), decimalOptions)
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick  = {
            onSalvar(
                HealthRepository.EntradaNutricao(
                    carboidratos = carboidratos.toDoubleOrNull(),
                    proteinas    = proteinas.toDoubleOrNull(),
                    gorduras     = gorduras.toDoubleOrNull(),
                    calorias     = calorias.toDoubleOrNull(),
                    vitaminaC    = vitaminaC.toDoubleOrNull(),
                    vitaminaD    = vitaminaD.toDoubleOrNull(),
                    vitaminaA    = vitaminaA.toDoubleOrNull(),
                    vitaminaB12  = vitaminaB12.toDoubleOrNull(),
                    calcio       = calcio.toDoubleOrNull(),
                    ferro        = ferro.toDoubleOrNull(),
                    sodio        = sodio.toDoubleOrNull(),
                    potassio     = potassio.toDoubleOrNull(),
                )
            )
        },
        modifier = Modifier.fillMaxWidth(),
        enabled  = !isCarregando,
        colors   = ButtonDefaults.buttonColors(containerColor = NutriGreen)
    ) {
        Text("Salvar nutrição")
    }
}

@Composable
fun DadoCard(label: String, valor: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CampoNutri(
    label:           String,
    value:           String,
    onChange:        (String) -> Unit,
    modifier:        Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onChange,
        label           = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier        = modifier,
        keyboardOptions = keyboardOptions,
        singleLine      = true
    )
}
