package com.explosionlab.nutriengine.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.explosionlab.nutriengine.model.Mensagem
import com.explosionlab.nutriengine.ui.theme.NutriGreen
import com.explosionlab.nutriengine.viewmodel.MegumiViewModel
import kotlinx.coroutines.launch

@Composable
fun MegumiScreen(
    innerPadding: PaddingValues   = PaddingValues(),
    viewModel:    MegumiViewModel = viewModel()
) {
    val mensagens   by viewModel.mensagens.collectAsStateWithLifecycle()
    val listState   = rememberLazyListState()
    var inputTexto  by remember { mutableStateOf("") }
    val snackbarHost = remember { SnackbarHostState() }
    val scope        = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.recarregar() }

    LaunchedEffect(mensagens.size) {
        if (mensagens.isNotEmpty())
            listState.animateScrollToItem(mensagens.size - 1)
    }


    LaunchedEffect(viewModel.semConexao) {
        if (viewModel.semConexao) {
            scope.launch {
                snackbarHost.showSnackbar(
                    message     = "Sem conexão com a internet. Verifique sua rede e tente novamente.",
                    duration    = SnackbarDuration.Long,
                )
            }
            viewModel.descartarAvisoSemConexao()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        modifier     = Modifier.padding(innerPadding),
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (mensagens.isEmpty()) {
                    if (viewModel.carregando) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = NutriGreen)
                        }
                    } else {
                        Box(
                            modifier         = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text       = "Olá, ${viewModel.nomeUsuario}!",
                                    style      = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = NutriGreen,
                                    textAlign  = TextAlign.Center,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text      = "Eu sou a Megumi, sua assistente nutricional. Pergunte-me qualquer coisa sobre alimentos!",
                                    style     = MaterialTheme.typography.bodyLarge,
                                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state               = listState,
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding      = PaddingValues(vertical = 12.dp),
                    ) {
                        items(mensagens, key = { it.id }) { msg -> BolhaMensagem(msg) }

                        if (viewModel.carregando) {
                            item { BolhaMensagem(Mensagem("...", ehUsuario = false)) }
                        }
                    }
                }
            }

            // ── Campo de entrada ──────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value         = inputTexto,
                    onValueChange = { inputTexto = it },
                    placeholder   = { Text("Pergunte algo sobre nutrição…") },
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(24.dp),
                    enabled       = !viewModel.carregando,
                    maxLines      = 3,
                )

                Spacer(Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (inputTexto.isNotBlank() && !viewModel.carregando) {
                            viewModel.enviarMensagem(inputTexto)
                            inputTexto = ""
                        }
                    },
                    containerColor = if (inputTexto.isNotBlank() && !viewModel.carregando) NutriGreen else Color.Gray,
                    modifier       = Modifier.size(52.dp),
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar mensagem",
                        tint               = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
fun BolhaMensagem(msg: Mensagem) {
    val corFundo    = if (msg.ehUsuario) NutriGreen else MaterialTheme.colorScheme.surfaceVariant
    val corTexto    = if (msg.ehUsuario) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val alinhamento = if (msg.ehUsuario) Alignment.End else Alignment.Start
    val formato     = if (msg.ehUsuario)
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp,  bottomStart = 16.dp, bottomEnd = 16.dp)
    else
        RoundedCornerShape(topStart = 4.dp,  topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = alinhamento,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(corFundo, formato)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text  = parseMarkdown(msg.texto),
                color = corTexto,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Converte padrões básicos de Markdown (**negrito**, *itálico*) em AnnotatedString. */
fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val pattern = Regex("""(\*\*|__)(.*?)\1|(\*|_)(.*?)\3""")

        for (match in pattern.findAll(text)) {
            append(text.substring(cursor, match.range.first))

            val boldText   = match.groupValues[2].ifEmpty { null }
            val italicText = match.groupValues[4].ifEmpty { null }

            when {
                boldText   != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold))  { append(boldText) }
                italicText != null -> withStyle(SpanStyle(fontStyle  = FontStyle.Italic)) { append(italicText) }
            }

            cursor = match.range.last + 1
        }

        if (cursor < text.length) append(text.substring(cursor))
    }
}
