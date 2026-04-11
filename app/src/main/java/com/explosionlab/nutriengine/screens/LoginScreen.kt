package com.explosionlab.nutriengine.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.explosionlab.nutriengine.R
import com.explosionlab.nutriengine.ui.theme.ErrorRed
import com.explosionlab.nutriengine.ui.theme.LightGreenContainer
import com.explosionlab.nutriengine.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoginSucesso: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val activity = LocalActivity.current as ComponentActivity

    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Bem-vindo ao NutriEngine",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )


        Text(
            text = "O motor de sua nutrição",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                viewModel.fazerLogin(activity) {
                    onLoginSucesso()
                }
            },
            modifier  = Modifier.fillMaxWidth().height(52.dp),
            shape     = RoundedCornerShape(8.dp),
            colors    = ButtonDefaults.buttonColors(containerColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            enabled   = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color       = Color.Gray
                )
            } else {
                Text(
                    text  = "Entrar com Google",
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (viewModel.errorMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text  = viewModel.errorMsg,
                color = ErrorRed,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
