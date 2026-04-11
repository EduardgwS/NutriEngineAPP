package com.explosionlab.nutriengine.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.explosionlab.nutriengine.ui.theme.NutriGreen

enum class NutriTab(
    val rota:  String,
    val label: String,
    val icone: ImageVector
) {
    INICIO   ("inicio",   "Início",    Icons.Default.Home),
    PESQUISAR("pesquisar", "Pesquisar", Icons.Default.Search),
    MEGUMI   ("megumi",   "Megumi",    Icons.Default.AutoAwesome),
    RELATORIO("relatorio","Relatório", Icons.Default.BarChart),
}

@Composable
fun BottomBar(
    tabAtual:      NutriTab,
    onTabSelected: (NutriTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        NutriTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == tabAtual,
                onClick  = { onTabSelected(tab) },
                icon     = {
                    Icon(
                        imageVector        = tab.icone,
                        contentDescription = tab.label
                    )
                },
                label  = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = NutriGreen,
                    selectedTextColor   = NutriGreen,
                    indicatorColor      = NutriGreen.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
    }
}
