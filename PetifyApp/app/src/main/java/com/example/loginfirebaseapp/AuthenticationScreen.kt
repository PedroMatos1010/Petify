package com.example.loginfirebaseapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.ui.AppThemeColors

@Composable
fun AuthenticationScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    //TEMA
    val themeId = viewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = configuration.screenWidthDp < 360

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        //IMAGEM DE FUNDO
        Image(
            painter = painterResource(id = R.drawable.cao_start),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.7f
        )

        //DEGRADÊ DINÂMICO
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentTheme.gradient)
                .alpha(0.6f)
        )

        //CONTEÚDO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isSmallScreen) 32.dp else 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(modifier = Modifier.weight(3.5f))

            // BOTÃO DE LOGIN
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 52.dp else 59.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTheme.textColor == Color.White) Color.Black else Color.White,
                    contentColor = if (currentTheme.textColor == Color.White) Color.White else Color.Black,
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(id = R.string.btn_login).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = if (isSmallScreen) 18.sp else 22.sp
                )
            }


            Spacer(modifier = Modifier.height(if (isSmallScreen) 24.dp else 45.dp))

            // BOTÃO DE REGISTAR
            Button(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 52.dp else 59.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTheme.textColor == Color.White) Color.Black else Color.White,
                    contentColor = if (currentTheme.textColor == Color.White) Color.White else Color.Black,
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(id = R.string.btn_register).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = if (isSmallScreen) 18.sp else 22.sp
                )
            }


            Spacer(modifier = Modifier.weight(1f))
        }
    }
}