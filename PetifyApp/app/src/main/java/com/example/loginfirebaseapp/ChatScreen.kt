package com.example.loginfirebaseapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    authViewModel: AuthViewModel,
    vetId: String,
    petId: String,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val myId = authViewModel.currentUser?.uid ?: ""
    val rawMessages by authViewModel.chatMessages

    val allMessagesIntercalated = remember(rawMessages) {
        rawMessages.sortedBy { msg ->
            when (val ts = msg["timestamp"]) {
                is Timestamp -> ts.toDate().time
                is Long -> ts
                else -> System.currentTimeMillis()
            }
        }
    }

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val themeId = authViewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)

    // Lógica de Responsividade
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 650

    LaunchedEffect(vetId, petId) {
        authViewModel.listenForMessages(vetId, petId)
    }

    LaunchedEffect(allMessagesIntercalated.size) {
        if (allMessagesIntercalated.isNotEmpty()) {
            listState.animateScrollToItem(allMessagesIntercalated.size - 1)
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(currentTheme.gradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            //HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isSmallScreen) 12.dp else 24.dp, start = 24.dp, end = 24.dp)
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* Menu */ },
                    modifier = Modifier.offset(x = (-12).dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.menu_icon),
                        contentDescription = null,
                        modifier = Modifier.size(if (isSmallScreen) 26.dp else 30.dp),
                        tint = currentTheme.textColor
                    )
                }
                IconButton(onClick = onNavigateToHome) {
                    Icon(
                        painter = painterResource(id = R.drawable.home_icon),
                        contentDescription = null,
                        modifier = Modifier.size(if (isSmallScreen) 26.dp else 30.dp),
                        tint = currentTheme.textColor
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.chat_title),
                fontSize = if (isSmallScreen) 38.sp else 56.sp,
                fontWeight = FontWeight.Black,
                color = currentTheme.textColor,
                modifier = Modifier.padding(horizontal = 24.dp)
            )


            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = if (isSmallScreen) 8.dp else 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.login_background),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.1f
                    )

                    Column(modifier = Modifier.fillMaxSize()) {

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp)
                        ) {
                            items(allMessagesIntercalated) { msg ->
                                val senderId = msg["senderId"]?.toString() ?: ""

                                MessageBubble(
                                    text = msg["text"]?.toString() ?: "",
                                    isMine = senderId == myId,
                                    theme = currentTheme
                                )
                            }
                        }


                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isSmallScreen) 8.dp else 16.dp),
                            color = Color(0xFFF3F3F3),
                            shape = RoundedCornerShape(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    placeholder = {
                                        Text(stringResource(id = R.string.chat_placeholder), color = Color.Gray, fontSize = 14.sp)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                                IconButton(onClick = {
                                    if (messageText.isNotBlank()) {
                                        authViewModel.sendMessage(vetId, messageText, petId)
                                        messageText = ""
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = Color(0xFF715639),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 70.dp else 90.dp))
        }

        //BOTÃO VOLTAR
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

@Composable
fun MessageBubble(
    text: String,
    isMine: Boolean,
    theme: AppThemeColors
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMine) theme.textColor.copy(alpha = 0.25f) else Color(0xFFECECEC),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            )
        ) {
            Text(
                text = text,
                color = Color.Black,
                fontSize = 15.sp,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = 250.dp)
            )
        }
    }
}