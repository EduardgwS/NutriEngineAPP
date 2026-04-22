package com.explosionlab.nutriengine.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.explosionlab.nutriengine.ui.theme.NutriGreen
import com.explosionlab.nutriengine.viewmodel.AppViewModel
import com.explosionlab.nutriengine.viewmodel.ConfiguracoesViewModel
import com.explosionlab.nutriengine.viewmodel.TemaApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onVoltar:         () -> Unit,
    onEditarPerfil:   () -> Unit,
    appViewModel:     AppViewModel,
    viewModel:        ConfiguracoesViewModel = viewModel(),
) {
    val state   = viewModel.state
    val context = LocalContext.current
    val version  = remember {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName
    }

    // ── Launchers de permissão ─────────────────────────────────────────────────

    val notificacaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida -> viewModel.onResultadoPermissaoNotificacao(concedida) }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Configurações", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = NutriGreen,
                    titleContentColor          = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White,
                ),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            // ── Aparência ──────────────────────────────────────────────────────
            GrupoTitulo("Aparência")

            SeletorTema(
                temaSelecionado = appViewModel.tema,
                onSelecionar    = { appViewModel.atualizarTema(it) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Perfil ─────────────────────────────────────────────────────────
            GrupoTitulo("Perfil")

            ItemBotao(
                icone     = Icons.Default.Person,
                titulo    = "Editar perfil",
                descricao = "Alterar nome, peso, altura e objetivo",
                onClick   = onEditarPerfil,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Notificações ───────────────────────────────────────────────────
            GrupoTitulo("Notificações")

            ItemSwitch(
                icone     = if (state.notificacoesAtivas) Icons.Default.Notifications
                else                           Icons.Default.NotificationsOff,
                titulo    = "Notificações",
                descricao = if (state.notificacoesAtivas) "Notificações ativadas"
                else                           "Notificações desativadas",
                checked   = state.notificacoesAtivas,
                onCheckedChange = { ativar ->
                    if (ativar) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificacaoLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.onResultadoPermissaoNotificacao(true)
                        }
                    } else {
                        viewModel.desativarNotificacoes()
                    }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Versão $version",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// ── Seletor de tema (3 opções) ────────────────────────────────────────────────

@Composable
private fun SeletorTema(
    temaSelecionado: TemaApp,
    onSelecionar:    (TemaApp) -> Unit,
) {
    data class OpcaoTema(val tema: TemaApp, val label: String, val icone: ImageVector)

    val opcoes = listOf(
        OpcaoTema(TemaApp.CLARO,   "Claro",     Icons.Default.LightMode),
        OpcaoTema(TemaApp.ESCURO,  "Escuro",    Icons.Default.DarkMode),
        OpcaoTema(TemaApp.SISTEMA, "Automático", Icons.Default.SettingsBrightness),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {

            Text("Tema", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            Text("Escolha como o app deve aparecer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                opcoes.forEach { opcao ->
                    val selecionado = temaSelecionado == opcao.tema
                    OutlinedButton(
                        onClick  = { onSelecionar(opcao.tema) },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selecionado) NutriGreen else androidx.compose.ui.graphics.Color.Transparent,
                            contentColor   = if (selecionado) androidx.compose.ui.graphics.Color.White
                            else             MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            width = if (selecionado) 0.dp else 1.dp,
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(opcao.icone, null, modifier = Modifier.size(18.dp))
                            Text(opcao.label, style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
private fun GrupoTitulo(texto: String) {
    Text(
        texto,
        style      = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color      = NutriGreen,
        modifier   = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun ItemSwitch(
    icone:           ImageVector,
    titulo:          String,
    descricao:       String,
    checked:         Boolean,
    onCheckedChange: (Boolean) -> Unit,
    carregando:      Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icone, null,
                tint     = NutriGreen,
                modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(descricao, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (carregando) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color       = NutriGreen,
                )
            } else {
                Switch(
                    checked         = checked,
                    onCheckedChange = onCheckedChange,
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor  = androidx.compose.ui.graphics.Color.White,
                        checkedTrackColor  = NutriGreen,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ItemBotao(
    icone:    ImageVector,
    titulo:   String,
    descricao: String,
    onClick:  () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick  = onClick,
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icone, null,
                tint     = NutriGreen,
                modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(descricao, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp))
        }
    }
}