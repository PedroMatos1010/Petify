package com.example.loginfirebaseapp.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton
import com.example.loginfirebaseapp.ui.components.MenuDrawerContent
import com.example.loginfirebaseapp.ui.components.LocationDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn

@Composable
fun HealthScreen(
    viewModel: AuthViewModel,
    petId: String,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateFromMenu: (String) -> Unit,
    onNavigateToHistory: (String) -> Unit,
    onNavigateToAppointment: (String) -> Unit,
    onNavigateToStatus: (String) -> Unit,
    onNavigateToPastAppointments: (String) -> Unit,
    onNavigateToAddVaccine: (String) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val themeId = viewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)


    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 650

    var selectedPetId by rememberSaveable { mutableStateOf(petId) }

    val uiState by viewModel.authUiState
    val userPets by viewModel.userPets
    val nextAppointment by viewModel.nextAppointment
    val petVaccines by viewModel.petVaccines
    val clinics by viewModel.clinics

    var isSynchronizing by remember { mutableStateOf(true) }
    var showVaccineDialog by remember { mutableStateOf(false) }

    var showLocPopup by remember { mutableStateOf(false) }
    var selectedCoords by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedClinicName by remember { mutableStateOf(context.getString(R.string.default_location_name)) }

    val isAppointmentPast = remember(nextAppointment) {
        val dateStr = nextAppointment?.get("date")?.toString() ?: ""
        if (dateStr.isEmpty()) return@remember false
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val appointmentDate = sdf.parse(dateStr)
            appointmentDate?.before(Date()) ?: false
        } catch (e: Exception) {
            false
        }
    }

    LaunchedEffect(selectedPetId) {
        if (selectedPetId.isNotEmpty()) {
            isSynchronizing = true
            viewModel.fetchNextAppointment(selectedPetId)
            viewModel.fetchVaccines(selectedPetId)
            viewModel.fetchClinics()
            delay(600)
            isSynchronizing = false
        }
    }

    LocationDialog(
        showDialog = showLocPopup,
        onDismiss = { showLocPopup = false },
        lat = selectedCoords?.first,
        lng = selectedCoords?.second,
        context = context,
        clinicName = selectedClinicName
    )

    if (showVaccineDialog) {
        VaccineOptionsDialog(
            hasVaccines = petVaccines.isNotEmpty(),
            onDismiss = { showVaccineDialog = false },
            onViewHistory = {
                showVaccineDialog = false
                onNavigateToHistory(selectedPetId)
            },
            onAddNew = {
                showVaccineDialog = false
                onNavigateToAddVaccine(selectedPetId)
            }
        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = if (isSmallScreen) 16.dp else 24.dp),
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

                    Box(
                        modifier = Modifier
                            .size(if (isSmallScreen) 38.dp else 45.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.3f))
                            .clickable { onNavigateToProfile() }
                    ) {
                        AsyncImage(
                            model = uiState.profileImageUrl,
                            contentDescription = stringResource(id = R.string.profile_image_desc),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.profile_icon)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.health_title),
                    fontSize = if (isSmallScreen) 44.sp else 56.sp,
                    fontWeight = FontWeight.Black,
                    color = currentTheme.textColor,
                    lineHeight = if (isSmallScreen) 48.sp else 60.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.select_pet_label),
                    color = currentTheme.textColor.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 12.dp, top = 8.dp)
                ) {
                    items(userPets) { petMap ->
                        val id = petMap["id"]?.toString() ?: ""
                        val petImageUrl = petMap["displayImage"]?.toString() ?: ""

                        PetSelectorItem(
                            name = petMap["name"]?.toString() ?: "",
                            imageUrl = petImageUrl,
                            isSelected = id == selectedPetId,
                            onClick = { selectedPetId = id }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.next_appointment_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentTheme.textColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                val currentApptPetId = nextAppointment?.get("petId")?.toString()

                if (nextAppointment != null && currentApptPetId == selectedPetId && !isAppointmentPast) {
                    NextAppointmentCard(
                        appointment = nextAppointment,
                        onClick = {
                            val cId = nextAppointment!!["clinicId"]?.toString() ?: ""
                            val clinic = clinics.find { it["id"] == cId }
                            val coords = clinic?.get("coordinates") as? Map<String, Any>
                            val lat = coords?.get("lat")?.toString()
                            val lng = coords?.get("lng")?.toString()

                            if (!lat.isNullOrEmpty() && !lng.isNullOrEmpty()) {
                                selectedCoords = Pair(lat, lng)
                                selectedClinicName = clinic["name"]?.toString() ?: context.getString(R.string.default_location_name)
                                showLocPopup = true
                            }
                        }
                    )
                } else if (isSynchronizing) {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFF2B676), modifier = Modifier.size(24.dp))
                        }
                    }
                } else {
                    NextAppointmentCard(null)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 32.dp),
                    thickness = 1.dp,
                    color = currentTheme.textColor.copy(alpha = 0.1f)
                )

                HealthActionButton(
                    label = stringResource(id = R.string.btn_vaccine_card),
                    iconId = R.drawable.health_icon,
                    onClick = { showVaccineDialog = true },
                    textColor = currentTheme.textColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                HealthActionButton(
                    label = stringResource(id = R.string.btn_new_appointment),
                    iconId = R.drawable.calendar_icon,
                    onClick = { onNavigateToAppointment(selectedPetId) },
                    textColor = currentTheme.textColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                HealthActionButton(
                    label = stringResource(id = R.string.btn_request_status),
                    iconId = R.drawable.chat_icon,
                    onClick = { onNavigateToStatus(selectedPetId) },
                    textColor = currentTheme.textColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                HealthActionButton(
                    label = stringResource(id = R.string.btn_appointment_history),
                    iconId = R.drawable.time_icon,
                    onClick = { onNavigateToPastAppointments(selectedPetId) },
                    textColor = currentTheme.textColor
                )

                Spacer(modifier = Modifier.height(140.dp))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 24.dp)
                    .offset(x = (-16).dp) // Offset corrigido para ser responsivo
            ) {
                CircularBackButton(onBack = onBack)
            }
        }
    }
}

@Composable
fun PetSelectorItem(name: String, imageUrl: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .then(
                    if (isSelected) Modifier.shadow(
                        elevation = 15.dp,
                        shape = CircleShape,
                        spotColor = Color(0xFFF2B676),
                        ambientColor = Color(0xFFF2B676)
                    ) else Modifier
                )
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFFF2B676) else Color.Black.copy(0.2f))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) Color.White else Color.White.copy(0.2f),
                    shape = CircleShape
                )
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isSelected) 3.dp else 0.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.profile_icon)
            )

            if (!isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = if (isSelected) Color(0xFFF2B676) else Color.Black.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = if (isSelected) 4.dp else 0.dp
        ) {
            Text(
                text = name.uppercase(),
                color = if (isSelected) Color.Black else Color.White.copy(0.6f),
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun VaccineOptionsDialog(
    hasVaccines: Boolean,
    onDismiss: () -> Unit,
    onViewHistory: () -> Unit,
    onAddNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp),
        confirmButton = {},
        containerColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = Color(0xFFF2B676).copy(alpha = 0.1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.health_icon),
                        contentDescription = null,
                        tint = Color(0xFFF2B676),
                        modifier = Modifier.padding(15.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(id = R.string.vaccine_record_title),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (hasVaccines) stringResource(id = R.string.vaccines_exist_msg) else stringResource(id = R.string.no_vaccines_msg),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onAddNew,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2B676)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (hasVaccines) stringResource(id = R.string.btn_add_new) else stringResource(id = R.string.btn_create_record),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (hasVaccines) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onViewHistory,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF2B676))
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_view_record),
                            color = Color(0xFFF2B676),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(id = R.string.btn_cancel), color = Color.White.copy(alpha = 0.4f))
                }
            }
        }
    )
}

@Composable
fun NextAppointmentCard(appointment: Map<String, Any>?, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(enabled = appointment != null) { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        if (appointment != null) {
            val status = appointment["status"]?.toString() ?: "pendente"
            val rawDate = appointment["date"]?.toString() ?: ""
            val atSeparator = stringResource(id = R.string.at_time)
            val displayDate = try {
                val parts = rawDate.split(" ")
                val dateParts = parts[0].split("-")
                "${dateParts[2]}/${dateParts[1]} $atSeparator ${parts[1]}"
            } catch (e: Exception) { rawDate }
            val (statusText, statusColor) = when(status.lowercase()) {
                "pendente" -> stringResource(id = R.string.status_pending) to Color(0xFFFFC107)
                "confirmado" -> stringResource(id = R.string.status_confirmed) to Color(0xFF4CAF50)
                "recusado", "rejeitado" -> stringResource(id = R.string.status_rejected) to Color(0xFFF44336)
                else -> status.uppercase() to Color(0xFFF2B676)
            }
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.time_icon),
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment["reason"]?.toString() ?: stringResource(id = R.string.default_appointment_reason),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "$displayDate • $statusText",
                        color = statusColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(24.dp))
                    Text(text = stringResource(id = R.string.map_label), color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(id = R.string.no_appointments_msg), color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun HealthActionButton(label: String, iconId: Int, onClick: () -> Unit, textColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(72.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = textColor.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFFF2B676).copy(alpha = 0.2f)) {
                Icon(painter = painterResource(id = iconId), contentDescription = null, tint = Color(0xFFF2B676), modifier = Modifier.padding(8.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}