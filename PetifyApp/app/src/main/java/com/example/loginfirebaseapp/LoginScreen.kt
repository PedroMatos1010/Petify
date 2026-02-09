package com.example.loginfirebaseapp.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val fieldsState by authViewModel.loginState
    val globalState by authViewModel.authUiState
    var passwordVisible by remember { mutableStateOf(false) }

    val themeId = authViewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)

    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 640
    val scrollState = rememberScrollState()

    LaunchedEffect(globalState.isLoggedIn) {
        if (globalState.isLoggedIn) {
            authViewModel.updateFCMToken()
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentTheme.gradient)
                .alpha(0.8f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(if (isSmallScreen) 50.dp else 100.dp))

            Text(
                text = stringResource(id = R.string.login_title),
                fontSize = if (isSmallScreen) 38.sp else 48.sp,
                fontWeight = FontWeight.Black,
                color = currentTheme.textColor,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            LoginInputField(
                value = fieldsState.email,
                onValueChange = { authViewModel.onLoginEmailChange(it) },
                label = stringResource(id = R.string.email_label),
                placeholder = "your@email.com",
                keyboardType = KeyboardType.Email,
                contentColor = currentTheme.textColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fieldsState.password,
                onValueChange = { authViewModel.onLoginPasswordChange(it) },
                label = { Text(stringResource(id = R.string.password_label), color = currentTheme.textColor, fontWeight = FontWeight.Bold) },
                placeholder = { Text("******", color = currentTheme.textColor.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = currentTheme.textColor,
                    unfocusedBorderColor = currentTheme.textColor.copy(alpha = 0.5f),
                    focusedTextColor = currentTheme.textColor,
                    unfocusedTextColor = currentTheme.textColor,
                    cursorColor = currentTheme.textColor
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(id = R.drawable.pata_password),
                            contentDescription = stringResource(id = R.string.show_password_desc),
                            modifier = Modifier.size(28.dp),
                            tint = currentTheme.textColor
                        )
                    }
                }
            )

            Box(modifier = Modifier.heightIn(min = 32.dp).padding(top = 8.dp)) {
                if (!fieldsState.errorMessage.isNullOrEmpty()) {
                    Text(
                        text = fieldsState.errorMessage!!,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 40.dp))

            Button(
                onClick = { authViewModel.login() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !fieldsState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (fieldsState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(id = R.string.btn_login), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text(
                    stringResource(id = R.string.register_link),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = currentTheme.textColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(120.dp))
        }

        //BOTÃO VOLTAR
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 0.dp, bottom = 24.dp) // start = 0 para encostar
        ) {
            CircularBackButton(onBack = onBack)
        }
    }
}

@Composable
fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    contentColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = contentColor, fontWeight = FontWeight.Bold) },
        placeholder = { Text(placeholder, color = contentColor.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = contentColor,
            unfocusedBorderColor = contentColor.copy(alpha = 0.5f),
            focusedTextColor = contentColor,
            unfocusedTextColor = contentColor,
            cursorColor = contentColor
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}