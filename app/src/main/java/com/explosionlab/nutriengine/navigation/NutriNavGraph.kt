package com.explosionlab.nutriengine.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.explosionlab.nutriengine.screens.*
import com.explosionlab.nutriengine.viewmodel.AppViewModel
import com.explosionlab.nutriengine.viewmodel.HomeViewModel
import com.explosionlab.nutriengine.viewmodel.MegumiViewModel


@Composable
fun NutriNavGraph(
    appViewModel:    AppViewModel,
    homeViewModel:   HomeViewModel,
    megumiViewModel: MegumiViewModel,
) {
    val navController = rememberNavController()

    val rotasComNav = remember { NutriTab.entries.map { it.rota }.toSet() }

    val rotaAtual = navController
        .currentBackStackEntryFlow
        .collectAsState(initial = navController.currentBackStackEntry)
        .value?.destination?.route

    val mostrarNav = rotaAtual in rotasComNav
    val tabAtual   = NutriTab.entries.find { it.rota == rotaAtual } ?: NutriTab.INICIO



    val itensMenu = remember {
        listOf(
            MenuItemData(
                label = "Editar Perfil",
                icone = Icons.Default.Person,
                acao  = { navController.navigate("editar_perfil") },
            ),
            MenuItemData(
                label = "Sair",
                icone = Icons.AutoMirrored.Filled.ExitToApp,
                acao  = {
                    appViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            ),
        )
    }

    Scaffold(
        topBar    = { if (mostrarNav) TopBar(itensMenu = itensMenu) },
        bottomBar = {
            if (mostrarNav) {
                BottomBar(
                    tabAtual      = tabAtual,
                    onTabSelected = { tab ->
                        navController.navigate(tab.rota) {
                            popUpTo(NutriTab.INICIO.rota) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = appViewModel.telaInicial,
        ) {

            composable("login") {
                LoginScreen(
                    onLoginSucesso = {
                        val destino = if (appViewModel.perfilCompleto()) NutriTab.INICIO.rota else "hc_intro"
                        navController.navigate(destino) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                )
            }

            composable("hc_intro") {
                HealthConnectIntroScreen(
                    onContinuar = {
                        navController.navigate("perfil") {
                            popUpTo("hc_intro") { inclusive = true }
                        }
                    },
                )
            }

            composable("perfil") {
                ConhecerPerfilScreen(
                    onConcluido = {
                        navController.navigate(NutriTab.INICIO.rota) {
                            popUpTo("perfil") { inclusive = true }
                        }
                    },
                )
            }

            // ── Edição de perfil (acessível pelo menu) ─────────────────────────
            composable("editar_perfil") {
                ConhecerPerfilScreen(
                    isEdicao    = true,
                    onConcluido = { navController.popBackStack() },
                    onVoltar    = { navController.popBackStack() },
                )
            }

            composable(NutriTab.INICIO.rota) {
                HomeScreen(innerPadding = innerPadding, viewModel = homeViewModel)
            }

            composable(NutriTab.PESQUISAR.rota) {
                PesquisarScreen(innerPadding = innerPadding)
            }

            composable(NutriTab.MEGUMI.rota) {
                MegumiScreen(innerPadding = innerPadding, viewModel = megumiViewModel)
            }

            composable(NutriTab.RELATORIO.rota) {
                RelatorioScreen(innerPadding = innerPadding)
            }
        }
    }
}
