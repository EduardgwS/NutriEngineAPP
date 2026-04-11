package com.explosionlab.nutriengine.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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


    val context = androidx.compose.ui.platform.LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName

    }

    Surface(
        color          = NutriGreen,
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
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "Ícone do Aplicativo",
                modifier = Modifier
                    .size(30.dp)
                    .scale(2.2f),
                tint = Color.White
            )

            Spacer(Modifier.width(6.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(

                    text       = "NutriEngine\nVersão $versionName",
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 16.sp,
                    lineHeight = 16.sp
                )
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
