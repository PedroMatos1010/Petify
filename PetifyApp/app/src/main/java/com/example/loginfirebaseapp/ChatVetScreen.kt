package com.example.loginfirebaseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun ChatVetScreen(
    authViewModel: AuthViewModel,
    petId: String,
    onBack: () -> Unit,
    onNavigateToChat: (String, String, String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateFromMenu: (String) -> Unit
) {
    val clinics by authViewModel.clinics
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    //TEMA
    val themeId = authViewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)


    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 650

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
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
                    text = stringResource(id = R.string.vets_title),
                    fontSize = if (isSmallScreen) 36.sp else 44.sp,
                    fontWeight = FontWeight.Black,
                    color = currentTheme.textColor,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = stringResource(id = R.string.vets_subtitle),
                    color = currentTheme.textColor.copy(alpha = 0.7f),
                    fontSize = if (isSmallScreen) 14.sp else 16.sp,
                    modifier = Modifier.padding(bottom = if (isSmallScreen) 16.dp else 24.dp)
                )

                if (clinics.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = currentTheme.textColor)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 120.dp) // Espaço extra para o botão de voltar
                    ) {
                        items(clinics) { clinic ->
                            val id = clinic["id"] as? String ?: ""
                            val name = clinic["name"] as? String ?: stringResource(id = R.string.unknown_name)

                            VetClinicCard(
                                name = name,
                                adress = clinic["adress"] as? String ?: stringResource(id = R.string.no_address),
                                phone = clinic["phone"] as? String ?: "",
                                email = clinic["email"] as? String ?: "",
                                onClick = { onNavigateToChat(id, name, petId) }
                            )
                        }
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

@Composable
fun VetClinicCard(
    name: String,
    adress: String,
    phone: String,
    email: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = Color(0xFFF2B676).copy(alpha = 0.2f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.profile_icon),
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = Color(0xFF715639)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.Black)
                Text(text = adress, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
                Text(text = email, fontSize = 12.sp, color = Color(0xFF715639), fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFF715639)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = phone, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Icon(
                painter = painterResource(id = R.drawable.chat_icon),
                contentDescription = stringResource(id = R.string.chat_title),
                tint = Color(0xFF715639),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}