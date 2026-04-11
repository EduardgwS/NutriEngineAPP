package com.explosionlab.nutriengine.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.explosionlab.nutriengine.repository.Alimento
import com.explosionlab.nutriengine.ui.theme.NutriGreen
import com.explosionlab.nutriengine.viewmodel.PesquisarViewModel
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesquisarScreen(
    innerPadding: PaddingValues     = PaddingValues(),
    viewModel:    PesquisarViewModel = viewModel()
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current


    var temPermissaoCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissaoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { temPermissaoCamera = it }

    LaunchedEffect(Unit) {
        if (!temPermissaoCamera) permissaoLauncher.launch(Manifest.permission.CAMERA)
    }


    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    var capturando      by remember { mutableStateOf(false) }


    val sheetState          = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sheetAberto         by remember { mutableStateOf(false) }
    var alimentoSelecionado by remember { mutableStateOf<Alimento?>(null) }


    val listaSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var listaAberta     by remember { mutableStateOf(false) }

    val listaEscolhidos      by viewModel.listaEscolhidos.collectAsState()


    val identificando        = viewModel.identificando
    val alimentoIdentificado = viewModel.alimentoIdentificado
    val erroIdentificacao    = viewModel.erroIdentificacao


    val resultadoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val resultadoSheetAberto = alimentoIdentificado != null || erroIdentificacao != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {


        if (temPermissaoCamera) {
            AndroidView(
                factory  = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }.also { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val resolutionSelector = ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        android.util.Size(1920, 1080),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                    )
                                )
                                .build()

                            val preview = Preview.Builder()
                                .setResolutionSelector(resolutionSelector)
                                .build()
                                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                                imageCaptureRef = imageCapture
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {

            Box(
                modifier         = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Permissão de câmera necessária",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { permissaoLauncher.launch(Manifest.permission.CAMERA) },
                        colors  = ButtonDefaults.buttonColors(containerColor = NutriGreen)
                    ) { Text("Conceder permissão") }
                }
            }
        }

        // ── Badge da lista no canto superior direito ───────────────────────────
        BadgedBox(
            badge = {
                if (listaEscolhidos.isNotEmpty()) {
                    Badge(
                        containerColor = NutriGreen,
                        contentColor   = Color.White
                    ) {
                        Text(
                            text  = listaEscolhidos.size.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
        ) {
            IconButton(
                onClick = { listaAberta = true },
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .size(48.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.List,
                    contentDescription = "Minha lista",
                    tint               = Color.White,
                    modifier           = Modifier.size(26.dp)
                )
            }
        }

        // ── Botões na parte inferior ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Botão "Identificar Alimento por Foto" ─────────────────────────
            Button(
                onClick = {
                    val ic = imageCaptureRef ?: return@Button
                    capturando = true
                    ic.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                try {
                                    val bytes = comprimirParaEnvio(image.toBitmap())
                                    viewModel.identificarPorImagem(bytes)
                                } catch (e: Exception) {
                                    Log.e("PesquisarScreen", "Erro ao processar imagem: ${e.message}")
                                } finally {
                                    image.close()
                                    capturando = false
                                }
                            }

                            override fun onError(exc: ImageCaptureException) {
                                Log.e("PesquisarScreen", "Erro ao capturar: ${exc.message}")
                                capturando = false
                            }
                        }
                    )
                },
                enabled   = temPermissaoCamera && !capturando && !identificando && imageCaptureRef != null,
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape     = RoundedCornerShape(28.dp),
                colors    = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF1565C0),
                    disabledContainerColor = Color(0xFF1565C0).copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                when {
                    capturando    -> {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Capturando...", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    identificando -> {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Identificando...", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    else -> {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Identificar Alimento por Foto", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Botão "Adicionar Manualmente" ──────────────────────────────────
            Button(
                onClick = {
                    viewModel.limpar()
                    sheetAberto = true
                },
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape     = RoundedCornerShape(28.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = NutriGreen),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Search, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar Manualmente", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    // ── Bottom Sheet de pesquisa ───────────────────────────────────────────────
    if (sheetAberto) {
        ModalBottomSheet(
            onDismissRequest = {
                sheetAberto = false
                viewModel.limpar()
                alimentoSelecionado = null
            },
            sheetState = sheetState
        ) {
            if (alimentoSelecionado != null) {
                DetalheAlimento(
                    alimento           = alimentoSelecionado!!,
                    onVoltar           = { alimentoSelecionado = null },
                    onFechar           = {
                        sheetAberto = false
                        viewModel.limpar()
                        alimentoSelecionado = null
                    },
                    onAdicionarNaLista = { alimento ->
                        viewModel.adicionarNaLista(alimento)
                        sheetAberto = false
                        viewModel.limpar()
                        alimentoSelecionado = null
                    }
                )
            } else {
                PesquisaManual(
                    viewModel     = viewModel,
                    onSelecionado = { alimentoSelecionado = it }
                )
            }
        }
    }

    // ── Bottom Sheet da lista de itens escolhidos ──────────────────────────────
    if (listaAberta) {
        var resumoVisivel  by remember { mutableStateOf(false) }
        var resumoSnapshot by remember { mutableStateOf<List<Alimento>>(emptyList()) }

        ModalBottomSheet(
            onDismissRequest = {
                listaAberta   = false
                resumoVisivel = false
            },
            sheetState = listaSheetState
        ) {
            if (resumoVisivel) {
                ResumoLista(
                    itens    = resumoSnapshot,
                    onFechar = {
                        viewModel.salvarLista(resumoSnapshot)
                        viewModel.limparLista()
                        listaAberta   = false
                        resumoVisivel = false
                    }
                )
            } else {
                ListaEscolhidos(
                    itens         = listaEscolhidos,
                    onRemover     = { viewModel.removerDaLista(it) },
                    onLimpar      = { viewModel.limparLista() },
                    onFechar      = { listaAberta = false },
                    onFecharLista = {
                        resumoSnapshot = listaEscolhidos.toList()
                        resumoVisivel  = true
                    }
                )
            }
        }
    }

    // ── Bottom Sheet de resultado da identificação por imagem ──────────────────
    if (resultadoSheetAberto) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.limparIdentificacao() },
            sheetState       = resultadoSheetState
        ) {
            if (alimentoIdentificado != null) {
                // Reutiliza o DetalheAlimento com fluxo idêntico à busca manual
                DetalheAlimento(
                    alimento           = alimentoIdentificado,
                    onVoltar           = { viewModel.limparIdentificacao() },
                    onFechar           = { viewModel.limparIdentificacao() },
                    onAdicionarNaLista = { alimento ->
                        viewModel.adicionarNaLista(alimento)
                        viewModel.limparIdentificacao()
                    }
                )
            } else if (erroIdentificacao != null) {
                // Mensagem de erro amigável
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Warning,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.error,
                        modifier           = Modifier.size(48.dp)
                    )
                    Text(
                        text       = "Não foi possível identificar",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center
                    )
                    Text(
                        text      = erroIdentificacao,
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick  = { viewModel.limparIdentificacao() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(28.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = NutriGreen)
                    ) {
                        Text("Tentar novamente", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ListaEscolhidos(
    itens:         List<Alimento>,
    onRemover:     (Alimento) -> Unit,
    onLimpar:      () -> Unit,
    onFechar:      () -> Unit,
    onFecharLista: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        // Cabeçalho
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text       = "Minha lista (${itens.size})",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (itens.isNotEmpty()) {
                    TextButton(onClick = onLimpar) {
                        Text("Limpar tudo", color = MaterialTheme.colorScheme.error)
                    }
                }
                IconButton(onClick = onFechar) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (itens.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector        = Icons.Default.List,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = "Nenhum item na lista ainda",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding      = PaddingValues(bottom = 12.dp)
            ) {
                items(itens) { alimento ->
                    CardItemLista(
                        alimento  = alimento,
                        onRemover = { onRemover(alimento) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = onFecharLista,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NutriGreen)
            ) {
                Icon(
                    imageVector        = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint               = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text("Fechar lista", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}



@Composable
fun ResumoLista(
    itens:    List<Alimento>,
    onFechar: () -> Unit
) {
    val totalKcal  = itens.sumOf { it.kcal }
    val totalProt  = itens.sumOf { it.proteinas }
    val totalCarbo = itens.sumOf { it.carboidratos }
    val totalGord  = itens.sumOf { it.gorduras }

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        Icon(
            imageVector        = Icons.Default.CheckCircle,
            contentDescription = null,
            tint               = NutriGreen,
            modifier           = Modifier.size(56.dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text       = "Resumo da lista",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = "${itens.size} item(ns) registrado(s)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        // Card de destaque — calorias totais
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            colors   = CardDefaults.cardColors(containerColor = NutriGreen)
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text       = "⚡ Total de calorias",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = "%.0f kcal".format(totalKcal),
                    style      = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    fontSize   = 42.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Cards de macros
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MacroCard("🥩 Proteínas",   "%.1f g".format(totalProt),  Modifier.weight(1f))
            MacroCard("🍞 Carboidratos","%.1f g".format(totalCarbo), Modifier.weight(1f))
            MacroCard("🧈 Gorduras",    "%.1f g".format(totalGord),  Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick  = onFechar,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NutriGreen)
        ) {
            Text("Concluir e limpar lista", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))
    }
}


@Composable
fun MacroCard(label: String, valor: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text      = label,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = valor,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
        }
    }
}

// ── Card de item na lista ──────────────────────────────────────────────────────

@Composable
fun CardItemLista(alimento: Alimento, onRemover: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alimento.descricao,
                    fontWeight = FontWeight.Bold,
                    style      = MaterialTheme.typography.bodyMedium
                )
                if (alimento.categoria.isNotBlank()) {
                    Text(
                        alimento.categoria,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "%.0f kcal".format(alimento.kcal),
                color      = NutriGreen,
                fontWeight = FontWeight.Bold,
                style      = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onRemover, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Remover",
                    tint               = MaterialTheme.colorScheme.error,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
    }
}



@Composable
fun PesquisaManual(
    viewModel:     PesquisarViewModel,
    onSelecionado: (Alimento) -> Unit
) {
    val resultados by viewModel.resultados.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            "Pesquisar alimento",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value         = viewModel.query,
            onValueChange = { viewModel.onQueryChange(it) },
            placeholder   = { Text("Digite o nome do alimento...") },
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(16.dp),
            leadingIcon   = { Icon(Icons.Default.Search, null) },
            trailingIcon  = {
                if (viewModel.query.isNotBlank()) {
                    IconButton(onClick = { viewModel.limpar() }) {
                        Icon(Icons.Default.Close, "Limpar")
                    }
                }
            },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        when {
            viewModel.carregando -> {
                Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                    CircularProgressIndicator(color = NutriGreen)
                }
            }
            viewModel.semResultados -> {
                Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                    Text(
                        "Nenhum resultado para \"${viewModel.query}\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding      = PaddingValues(bottom = 16.dp)
                ) {
                    items(resultados) { alimento ->
                        CardAlimento(alimento = alimento, onClick = { onSelecionado(alimento) })
                    }
                }
            }
        }
    }
}

// ── Card de alimento ───────────────────────────────────────────────────────────

@Composable
fun CardAlimento(alimento: Alimento, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alimento.descricao,
                    fontWeight = FontWeight.Bold,
                    style      = MaterialTheme.typography.bodyMedium
                )
                if (alimento.categoria.isNotBlank()) {
                    Text(
                        alimento.categoria,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "%.0f kcal".format(alimento.kcal),
                color      = NutriGreen,
                fontWeight = FontWeight.Bold,
                style      = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── Detalhe do alimento ────────────────────────────────────────────────────────

@Composable
fun DetalheAlimento(
    alimento:           Alimento,
    onVoltar:           () -> Unit,
    onFechar:           () -> Unit,
    onAdicionarNaLista: (Alimento) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            TextButton(onClick = onVoltar) { Text("← Voltar") }
            IconButton(onClick = onFechar) { Icon(Icons.Default.Close, "Fechar") }
        }

        Text(
            alimento.descricao,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (alimento.categoria.isNotBlank()) {
            Text(
                alimento.categoria,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Informação nutricional (por 100g)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DadoCard("⚡ Energia",  "%.0f kcal".format(alimento.kcal),   Modifier.weight(1f))
            DadoCard("🥩 Proteína", "%.1f g".format(alimento.proteinas), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DadoCard("🍞 Carbo",   "%.1f g".format(alimento.carboidratos), Modifier.weight(1f))
            DadoCard("🧈 Gordura", "%.1f g".format(alimento.gorduras),     Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick  = { onAdicionarNaLista(alimento) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(28.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = NutriGreen)
        ) {
            Text("Adicionar à lista", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ── Compressão de imagem para envio ───────────────────────────────────────────

/**
 * Reduz o bitmap para no máximo [maxDim]×[maxDim] px mantendo proporção,
 * depois comprime como JPEG com qualidade [qualidade].
 *
 * Uma foto de 1920×1080 (~500 KB) passa a ~25–40 KB com esses valores,
 * acelerando bastante o envio em conexões lentas sem comprometer o
 * reconhecimento pela IA.
 */
private fun comprimirParaEnvio(
    bitmap:    Bitmap,
    maxDim:    Int = 800,
    qualidade: Int = 72,
): ByteArray {
    val largura  = bitmap.width
    val altura   = bitmap.height
    val escala   = maxDim.toFloat() / maxOf(largura, altura)

    val reduzido = if (escala < 1f) {
        Bitmap.createScaledBitmap(
            bitmap,
            (largura * escala).toInt(),
            (altura  * escala).toInt(),
            /* filter = */ true
        )
    } else {
        bitmap
    }

    return ByteArrayOutputStream().also { stream ->
        reduzido.compress(Bitmap.CompressFormat.JPEG, qualidade, stream)
    }.toByteArray()
}