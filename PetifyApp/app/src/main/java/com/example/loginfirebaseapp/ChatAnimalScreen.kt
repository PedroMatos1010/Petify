package com.example.loginfirebaseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton
import com.example.loginfirebaseapp.ui.components.MenuDrawerContent
import com.example.loginfirebaseapp.ui.components.ChatPetCard
import kotlinx.coroutines.launch

@Composable
fun ChatAnimalScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToChatVet: (String) -> Unit,
    onNavigateFromMenu: (String) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val userPets by authViewModel.userPets

    //Tema
    val themeId = authViewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)


    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 650

    LaunchedEffect(Unit) {
        authViewModel.fetchUserPets()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuDrawerContent(
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    onNavigateFromMenu(route)
                },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(currentTheme.gradient)) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                    // HEADER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.offset(x = (-12).dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.menu_icon),
                                contentDescription = stringResource(id = R.string.menu_description),
                                modifier = Modifier.size(if (isSmallScreen) 26.dp else 30.dp),
                                tint = currentTheme.textColor
                            )
                        }
                        IconButton(onClick = onNavigateToHome) {
                            Icon(
                                painter = painterResource(id = R.drawable.home_icon),
                                contentDescription = stringResource(id = R.string.btn_home),
                                modifier = Modifier.size(if (isSmallScreen) 26.dp else 30.dp),
                                tint = currentTheme.textColor
                            )
                        }
                    }

                    Text(
                        text = stringResource(id = R.string.chat_title),
                        fontSize = if (isSmallScreen) 42.sp else 56.sp,
                        fontWeight = FontWeight.Black,
                        color = currentTheme.textColor,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Text(
                        text = stringResource(id = R.string.chat_subtitle),
                        color = currentTheme.textColor.copy(alpha = 0.7f),
                        fontSize = if (isSmallScreen) 14.sp else 16.sp,
                        modifier = Modifier.padding(bottom = if (isSmallScreen) 16.dp else 32.dp)
                    )
                }

                if (userPets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.no_pets_registered),
                                color = currentTheme.textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    items(userPets) { petMap ->
                        val petId = petMap["id"]?.toString() ?: ""
                        val petName = petMap["name"]?.toString() ?: stringResource(id = R.string.default_animal_name)
                        val petSpecies = petMap["species"]?.toString() ?: "N/A"
                        val petImageUrl = petMap["displayImage"]?.toString() ?: ""

                        ChatPetCard(
                            name = petName,
                            info = petSpecies,
                            imageUrl = petImageUrl,
                            onClick = {
                                if (petId.isNotEmpty()) {
                                    onNavigateToChatVet(petId)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            //BOTÃO DE VOLTAR
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 24.dp)
                    .offset(x = (-16).dp)
            ) {
                CircularBackButton(onBack = onBack)
            }
        }
    }
}