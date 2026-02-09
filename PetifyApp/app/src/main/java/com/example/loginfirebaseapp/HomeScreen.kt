package com.example.loginfirebaseapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.AuthViewModel
import androidx.compose.ui.res.stringResource
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.MenuDrawerContent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: AuthViewModel,
    onLogout: () -> Unit,
    userName: String,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToHealth: (String?) -> Unit,
    onNavigateToChatAnimal: () -> Unit,
    onNavigateFromMenu: (String) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()


    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 650

    val uiState by viewModel.authUiState
    val nextEvent by viewModel.nextUpcomingEvent
    val userPets by viewModel.userPets

    val unreadCount by viewModel.unreadNotificationsCount
    val unreadChats by viewModel.unreadChatCount

    val themeId = viewModel.selectedThemeId.value
    val currentThemeColors = AppThemeColors.fromId(themeId)

    LaunchedEffect(Unit) {
        viewModel.fetchNextUpcomingEvent()
        viewModel.fetchUserPets()
        viewModel.fetchAllEvents()
        viewModel.updateFCMToken()
        viewModel.listenForNotifications()
        viewModel.listenForUnreadChatMessages()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MenuDrawerContent(
                onNavigate = { screen ->
                    scope.launch {
                        drawerState.close()
                        when (screen) {
                            "Logout" -> onLogout()
                            "Home" -> { /* Nada a fazer */ }
                            else -> onNavigateFromMenu(screen)
                        }
                    }
                },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    )
    {
        Box(modifier = Modifier.fillMaxSize().background(currentThemeColors.gradient)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = if (isSmallScreen) 80.dp else 100.dp)
            ) {
                HomeHeader(
                    userName = userName,
                    profileImageUrl = uiState.profileImageUrl,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onNavigateToProfile = onNavigateToProfile,
                    unreadNotifications = unreadCount,
                    onNavigateToNotifications = onNavigateToNotifications,
                    textColor = currentThemeColors.textColor,
                    isSmallScreen = isSmallScreen
                )

                Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 32.dp))

                QuickActionButtons(
                    onNavigateToHealth = {
                        val firstPetId = userPets.firstOrNull()?.get("id")?.toString()
                        onNavigateToHealth(firstPetId)
                    },
                    onNavigateToCalendar = onNavigateToCalendar,
                    onNavigateToRecords = { onNavigateFromMenu("Adoption") }
                )

                Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 32.dp))

                NextEventsCard(
                    nextEvent = nextEvent,
                    onNavigateToCalendar = onNavigateToCalendar,
                    textColor = currentThemeColors.textColor,
                    isSmallScreen = isSmallScreen
                )

                Spacer(modifier = Modifier.height(if (isSmallScreen) 24.dp else 32.dp))

                MyPetsSectionHome(
                    pets = userPets,
                    onPetClick = { onNavigateToProfile() },
                    textColor = currentThemeColors.textColor
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = if (isSmallScreen) 8.dp else 16.dp)
            ) {
                ChatIconButtonWithBadge(
                    unreadCount = unreadChats,
                    onNavigate = onNavigateToChatAnimal
                )
            }
        }
    }
}

@Composable
fun ChatIconButtonWithBadge(
    unreadCount: Int,
    onNavigate: () -> Unit
) {
    Box(modifier = Modifier.size(70.dp)) {
        FloatingActionButton(
            onClick = onNavigate,
            containerColor = Color(0xFFC8A2C8),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.chat_icon),
                contentDescription = "Chat",
                modifier = Modifier.size(28.dp)
            )
        }

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .background(Color.Red, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HomeHeader(
    userName: String,
    profileImageUrl: String,
    onOpenMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    unreadNotifications: Int,
    onNavigateToNotifications: () -> Unit,
    textColor: Color,
    isSmallScreen: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
            IconButton(onClick = onOpenMenu, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.menu_icon),
                    contentDescription = stringResource(id = R.string.menu_description),
                    modifier = Modifier.size(if (isSmallScreen) 26.dp else 30.dp),
                    tint = textColor
                )
            }
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = stringResource(R.string.Hi),
                    fontSize = if (isSmallScreen) 42.sp else 54.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    lineHeight = if (isSmallScreen) 40.sp else 50.sp
                )
                Text(
                    text = userName,
                    fontSize = if (isSmallScreen) 18.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            NotificationBell(unreadCount = unreadNotifications, onClick = onNavigateToNotifications, iconTint = textColor)
            Spacer(modifier = Modifier.width(8.dp))
            ProfileAvatar(imageUrl = profileImageUrl, onNavigateToProfile = onNavigateToProfile)
        }
    }
}

@Composable
fun NotificationBell(unreadCount: Int, onClick: () -> Unit, iconTint: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.notification_icon),
            contentDescription = stringResource(id = R.string.notifications_description),
            modifier = Modifier.size(28.dp),
            tint = iconTint
        )

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp)
                    .background(Color.Red, CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun ProfileAvatar(imageUrl: String, onNavigateToProfile: () -> Unit) {
    Surface(
        modifier = Modifier.size(52.dp).clickable(onClick = onNavigateToProfile).clip(CircleShape),
        color = Color.White,
        border = BorderStroke(2.dp, Color.Black.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(id = R.string.profile_image_desc),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = Color(0xFFC8A2C8)
                )
            }
        }
    }
}

@Composable
fun QuickActionButtons(onNavigateToHealth: () -> Unit, onNavigateToCalendar: () -> Unit, onNavigateToRecords: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ActionButton(R.drawable.health_icon, Color(0xFF4285F4), onNavigateToHealth)
        ActionButton(R.drawable.calendar_icon, Color(0xFFFFA726), onNavigateToCalendar)
        ActionButton(R.drawable.clipboard_icon, Color(0xFFF44336), onNavigateToRecords)
    }
}

@Composable
fun ActionButton(iconId: Int, backgroundColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(90.dp).clip(RoundedCornerShape(20.dp)).background(backgroundColor).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter = painterResource(iconId), contentDescription = null, tint = Color.White, modifier = Modifier.size(45.dp))
    }
}

@Composable
fun NextEventsCard(nextEvent: Map<String, Any>?, onNavigateToCalendar: () -> Unit, textColor: Color, isSmallScreen: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_next_events),
            fontSize = if (isSmallScreen) 20.sp else 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 115.dp else 130.dp).clickable { onNavigateToCalendar() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            if (nextEvent != null) {
                Row(modifier = Modifier.fillMaxSize().padding(if (isSmallScreen) 14.dp else 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val iconRes = if (nextEvent["type"] == "appointment") R.drawable.health_icon else R.drawable.calendar_icon
                        Icon(painter = painterResource(iconRes), contentDescription = null, tint = Color(0xFFFFA726), modifier = Modifier.size(if (isSmallScreen) 28.dp else 32.dp))
                        Text(text = formatarDataHome(nextEvent["date"]?.toString() ?: ""), color = Color.White, fontSize = if (isSmallScreen) 12.sp else 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(if (isSmallScreen) 12.dp else 20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = nextEvent["title"]?.toString() ?: stringResource(id = R.string.default_event_title), color = Color.White, fontSize = if (isSmallScreen) 17.sp else 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(text = nextEvent["description"]?.toString() ?: "", color = Color.White.copy(alpha = 0.6f), fontSize = if (isSmallScreen) 12.sp else 14.sp, maxLines = 1)
                    }
                    Image(painter = painterResource(R.drawable.miau_face_icon), contentDescription = null, modifier = Modifier.size(if (isSmallScreen) 50.dp else 60.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(id = R.string.no_events_msg), color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun MyPetsSectionHome(pets: List<Map<String, Any>>, onPetClick: () -> Unit, textColor: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_my_pets),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )

            if (pets.size > 2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ver todos",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { onPetClick() }
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.menu_icon),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(start = 4.dp),
                        tint = textColor.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pets.isEmpty()) {
            Text(text = stringResource(id = R.string.register_first_pet_msg), color = textColor.copy(alpha = 0.5f))
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(end = 40.dp)
                ) {
                    items(pets) { pet ->
                        PetCardHome(
                            name = pet["name"]?.toString() ?: "PET",
                            imageUrl = pet["displayImage"]?.toString() ?: "",
                            onClick = onPetClick
                        )
                    }
                }

                if (pets.size > 2) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(30.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("➔", color = textColor.copy(alpha = 0.3f), fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PetCardHome(name: String, imageUrl: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(160.dp).height(190.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)),
        border = BorderStroke(2.dp, Color(0xFFC8A2C8).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name.uppercase(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 12.dp)
            )

            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier.padding(12.dp).fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.josh_image),
                    contentDescription = name,
                    modifier = Modifier.padding(12.dp).fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

fun formatarDataHome(dataIso: String): String {
    if (dataIso.isEmpty()) return ""
    return try {
        val cleanDate = dataIso.split(" ")[0]
        val date = LocalDate.parse(cleanDate)
        date.format(DateTimeFormatter.ofPattern("dd/MM"))
    } catch (e: Exception) {
        dataIso
    }
}