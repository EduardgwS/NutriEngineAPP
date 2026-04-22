package com.explosionlab.nutriengine.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.explosionlab.nutriengine.NetworkModule
import com.explosionlab.nutriengine.R
import com.explosionlab.nutriengine.ui.theme.NutriGreen



data class MenuItemData(
    val label: String,
    val icone: ImageVector? = null,
    val acao:  () -> Unit
)

@Composable
fun TopBar(
    itensMenu:      List<MenuItemData> = emptyList()
) {
    var menuAberto by remember { mutableStateOf(false) }

    val servidorDisponivel by NetworkModule.servidorDisponivel.collectAsStateWithLifecycle()

    Surface(
        color          = if (servidorDisponivel) NutriGreen else Color(0xFF424242),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Ícone do Aplicativo",
                modifier = Modifier
                    .size(35.dp)
            )

            Spacer(Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text       = "NutriEngine",
                    color      = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize   = 18.sp
                )
                if (!servidorDisponivel) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text       = "SEM CONEXÃO COM O SERVIDOR",
                            color      = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))



            Box {
                IconButton(onClick = { menuAberto = true }) {
                    Icon(
                        imageVector        = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint               = Color.White,
                        modifier           = Modifier.size(26.dp)
                    )
                }

                DropdownMenu(
                    expanded         = menuAberto,
                    onDismissRequest = { menuAberto = false }
                ) {
                    itensMenu.forEach { item ->
                        DropdownMenuItem(
                            text    = { Text(item.label) },
                            onClick = {
                                menuAberto = false
                                item.acao()
                            },
                            leadingIcon = item.icone?.let { icone ->
                                { Icon(icone, contentDescription = null) }
                            }
                        )
                    }
                }
            }
        }
    }
}
