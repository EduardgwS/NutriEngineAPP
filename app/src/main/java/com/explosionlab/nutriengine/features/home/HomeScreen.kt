@file:Suppress("AssignedValueIsNeverRead")

package com.explosionlab.nutriengine.features.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.explosionlab.nutriengine.core.model.RecomendacaoReceita
import com.explosionlab.nutriengine.core.notifications.NotificationScheduler
import com.explosionlab.nutriengine.features.home.components.CaloriasProgressBar
import com.explosionlab.nutriengine.features.home.components.ControleCard
import com.explosionlab.nutriengine.features.home.components.MegumiInsightCard
import com.explosionlab.nutriengine.features.home.components.MetaDoDiaCard
import com.explosionlab.nutriengine.features.home.components.ProdutoDetalheSheet
import com.explosionlab.nutriengine.features.home.components.ReceitaDetalheSheet
import com.explosionlab.nutriengine.features.home.components.ReceitaRecomendadaCard
import com.explosionlab.nutriengine.features.home.components.SecaoMercado
import com.explosionlab.nutriengine.features.market.MarketViewModel
import com.explosionlab.nutriengine.features.market.RecomendacaoProduto

private const val PREFS_NAME = "nutriengine_prefs"
private const val PREF_NOTIF_SOLICITADA = "permissao_notificacao_solicitada"
private const val PREF_NOTIF_ATIVAS = "notificacoes_ativas"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel,
    marketViewModel: MarketViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val marketState by marketViewModel.recomendacoes.collectAsStateWithLifecycle()
    val parceiros by marketViewModel.parceiros.collectAsStateWithLifecycle()
    val carregandoMarket by marketViewModel.carregando.collectAsStateWithLifecycle()

    val context = LocalContext.current

    HomeObservers(viewModel, marketViewModel)
    RequestNotificationPermission(context)

    var receitaDetalhe by remember { mutableStateOf<RecomendacaoReceita?>(null) }
    var produtoDetalhe by remember { mutableStateOf<RecomendacaoProduto?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        ControleCard(streak = state.streak, semanaStatus = state.semanaStatus)


        CaloriasProgressBar(state.caloriasHoje, state.caloriasRecomendadas)

        if (state.macroState.proteinaMeta > 0) {
            MetaDoDiaCard(state.macroState, state.dicaMacro)
        }

        MegumiInsightCard(
            insight = state.insightMegumi,
            carregando = state.carregandoInsight
        )

        state.recomendacaoReceita?.let { receita ->
            ReceitaRecomendadaCard(receita, onClick = { receitaDetalhe = receita })
        }

        SecaoMercado(
            recomendacoes = marketState,
            parceiros = parceiros,
            carregando = carregandoMarket,
            onVerProduto = { produtoDetalhe = it },
            onAtualizar = { marketViewModel.carregarDados(force = true) },
        )
    }

    receitaDetalhe?.let { receita ->
        ModalBottomSheet(onDismissRequest = { receitaDetalhe = null }) {
            ReceitaDetalheSheet(receita) { receitaDetalhe = null }
        }
    }

    produtoDetalhe?.let { produto ->
        ModalBottomSheet(onDismissRequest = { produtoDetalhe = null }) {
            ProdutoDetalheSheet(
                produto = produto,
                onFechar = { produtoDetalhe = null },
                onComprar = {
                    marketViewModel.abrirProduto(produto.urlCompra, context)
                    produtoDetalhe = null
                },
            )
        }
    }
}

@Composable
private fun HomeObservers(viewModel: HomeViewModel, marketViewModel: MarketViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.recarregarCaloriasHome()
                marketViewModel.carregarDados()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun RequestNotificationPermission(context: Context) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    putBoolean(PREF_NOTIF_ATIVAS, true)
                }
            NotificationScheduler.ativar(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jaFoiSolicitada = prefs.getBoolean(PREF_NOTIF_SOLICITADA, false)
        val temPermissao = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!temPermissao && !jaFoiSolicitada) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            prefs.edit { putBoolean(PREF_NOTIF_SOLICITADA, true) }
        }
    }
}
