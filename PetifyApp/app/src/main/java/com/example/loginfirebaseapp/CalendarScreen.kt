package com.example.loginfirebaseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton
import com.example.loginfirebaseapp.ui.components.MenuDrawerContent
import com.example.loginfirebaseapp.ui.components.LocationDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val today = remember { LocalDate.now() }
    val context = LocalContext.current

    val themeId = authViewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)


    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 650

    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(today) }
    var showLocationPopup by remember { mutableStateOf(false) }
    var selectedClinicCoords by remember { mutableStateOf<Pair<String, String>?>(null) }

    val allEventsList by authViewModel.allEvents
    val userAppointments by authViewModel.userAppointments
    val userPets by authViewModel.userPets
    val clinics by authViewModel.clinics
    val adoptionRequests by authViewModel.adoptionRequests

    LaunchedEffect(Unit) {
        authViewModel.fetchAllEvents()
        authViewModel.fetchAllUserAppointments()
        authViewModel.fetchAdoptionRequests()
        authViewModel.fetchUserPets()
        authViewModel.fetchClinics()
    }

    LaunchedEffect(selectedDate) {
        authViewModel.fetchEventsForDate(selectedDate.toString())
    }

    val combinedEvents = remember(allEventsList, userAppointments, adoptionRequests, userPets) {
        val appointmentsAsEvents = userAppointments
            .filter { it["status"].toString().lowercase().contains("confirmad") }
            .map { appt ->
                val fullDate = appt["date"]?.toString() ?: ""
                val dateParts = fullDate.split(" ")
                val petId = appt["petId"]?.toString() ?: ""
                val petName = userPets.find { it["id"] == petId }?.get("name")?.toString() ?: "Pet"
                mapOf(
                    "id" to (appt["id"] ?: ""),
                    "clinicId" to (appt["clinicId"] ?: ""),
                    "title" to "Consulta: $petName",
                    "description" to (appt["reason"] ?: ""),
                    "date" to (dateParts.getOrNull(0) ?: ""),
                    "time" to (dateParts.getOrNull(1) ?: ""),
                    "urgency" to (appt["urgency"] ?: "média"),
                    "isAppointment" to true
                )
            }

        val adoptionsAsEvents = adoptionRequests
            .filter { it["interviewDate"]?.toString()?.isNotEmpty() == true &&
                    it["requestStatus"]?.toString() == "interview" }
            .map { req ->
                val fullDateTime = req["interviewDate"].toString()
                val parts = fullDateTime.split(" ")
                mapOf(
                    "id" to (req["id"] ?: ""),
                    "clinicId" to (req["adoptionCenterId"] ?: ""),
                    "title" to "Entrevista: ${req["petName"] ?: "Adoção"}",
                    "description" to "Entrevista de Adoção",
                    "date" to (parts.getOrNull(0) ?: ""),
                    "time" to (parts.getOrNull(1) ?: ""),
                    "urgency" to "entrevista",
                    "isAppointment" to true
                )
            }

        (allEventsList + appointmentsAsEvents + adoptionsAsEvents).sortedBy { it["time"]?.toString() ?: "" }
    }

    val pageCount = 2000
    val initialPage = pageCount / 2
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }
    val currentMonthVisible = remember(pagerState.currentPage) {
        today.withDayOfMonth(1).plusMonths((pagerState.currentPage - initialPage).toLong())
    }

    LocationDialog(
        showDialog = showLocationPopup,
        onDismiss = { showLocationPopup = false },
        lat = selectedClinicCoords?.first,
        lng = selectedClinicCoords?.second,
        context = context,
        clinicName = stringResource(id = R.string.clinic_location_title)
    )

    if (showDialog) {
        AddEventDialog(
            selectedDate = selectedDate,
            onDismiss = { showDialog = false },
            onConfirm = { title, desc ->
                authViewModel.saveEvent(title, desc, selectedDate.toString())
                showDialog = false
            }
        )
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
                            "Profile" -> onNavigateToProfile()
                            "Definitions" -> onNavigateToSettings()
                        }
                    }
                },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(currentTheme.gradient)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                // Cabeçalho
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = if (isSmallScreen) 16.dp else 24.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier.offset(x = (-12).dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.menu_icon),
                            contentDescription = null,
                            modifier = Modifier.size(if (isSmallScreen) 26.dp else 30.dp),
                            tint = currentTheme.textColor
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.calendar_title),
                        fontSize = if (isSmallScreen) 32.sp else 42.sp,
                        fontWeight = FontWeight.Black,
                        color = currentTheme.textColor,
                        lineHeight = if (isSmallScreen) 38.sp else 50.sp
                    )
                }

                Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 16.dp))

                // Calendário (Grid)
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.2f)).padding(if (isSmallScreen) 8.dp else 16.dp)) {
                    Text(
                        text = currentMonthVisible.month.getDisplayName(TextStyle.FULL, Locale("pt", "PT")).replaceFirstChar { it.titlecase() } + " " + currentMonthVisible.year,
                        fontSize = if (isSmallScreen) 18.sp else 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp)
                    )
                    DaysOfWeekHeader()
                    HorizontalPager(state = pagerState) { pageIndex ->
                        val monthToDisplay = today.withDayOfMonth(1).plusMonths((pageIndex - initialPage).toLong())
                        DaysGrid(currentMonth = monthToDisplay, selectedDate = selectedDate, eventsList = combinedEvents, onDayClick = { day -> selectedDate = monthToDisplay.withDayOfMonth(day) })
                    }
                }

                Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 24.dp))
                Text(
                    text = "${stringResource(id = R.string.events_on_day)} ${selectedDate.dayOfMonth}/${selectedDate.monthValue}",
                    fontSize = if (isSmallScreen) 18.sp else 20.sp, fontWeight = FontWeight.Bold, color = currentTheme.textColor
                )
                Spacer(modifier = Modifier.height(8.dp))

                val dailyEvents = combinedEvents.filter { it["date"] == selectedDate.toString() }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (dailyEvents.isEmpty()) {
                        item {
                            Text(
                                stringResource(id = R.string.no_events_msg),
                                color = currentTheme.textColor.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        items(dailyEvents) { event ->
                            EventItem(
                                title = event["title"]?.toString() ?: "",
                                description = event["description"]?.toString() ?: "",
                                time = event["time"]?.toString() ?: "",
                                isAppointment = event["isAppointment"] as? Boolean ?: false,
                                urgency = event["urgency"]?.toString() ?: "",
                                onLocationClick = {
                                    val cId = event["clinicId"]?.toString() ?: ""
                                    val clinic = clinics.find { it["id"] == cId || it["clinicId"] == cId }
                                    val coords = clinic?.get("coordinates") as? Map<String, Any>
                                    val lat = coords?.get("lat")?.toString()
                                    val lng = coords?.get("lng")?.toString()
                                    if (!lat.isNullOrEmpty() && !lng.isNullOrEmpty()) {
                                        selectedClinicCoords = Pair(lat, lng)
                                        showLocationPopup = true
                                    }
                                },
                                onDelete = { authViewModel.deleteEvent(event["id"]?.toString() ?: "", selectedDate.toString()) }
                            )
                        }
                    }
                }
            }

            // Botão Adicionar Evento
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = currentTheme.textColor,
                    contentColor = if (currentTheme.textColor == Color.White) Color.Black else Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(if (isSmallScreen) 48.dp else 56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }

            // Botão Voltar
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 32.dp).offset(x = (-16).dp)) {
                CircularBackButton(onBack = onBack)
            }
        }
    }
}


@Composable
fun EventItem(title: String, description: String, time: String, isAppointment: Boolean, urgency: String, onLocationClick: () -> Unit, onDelete: () -> Unit) {
    val urgencyColor = getUrgencyColor(urgency)
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (isAppointment) urgencyColor.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.7f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isAppointment) {
                Box(modifier = Modifier.width(4.dp).height(50.dp).clip(RoundedCornerShape(2.dp)).background(urgencyColor))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    if (time.isNotEmpty()) {
                        Text(text = " • $time", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Text(description, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
            if (isAppointment) {
                IconButton(onClick = onLocationClick) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                }
            } else {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun DaysGrid(currentMonth: LocalDate, selectedDate: LocalDate, eventsList: List<Map<String, Any>>, onDayClick: (Int) -> Unit) {
    val firstDayOfMonth = currentMonth.withDayOfMonth(1)
    val startDayOfWeek = (firstDayOfMonth.dayOfWeek.value % 7)
    val daysInMonth = currentMonth.lengthOfMonth()
    Column {
        for (row in 0..5) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 1..7) {
                    val dayIndex = row * 7 + column - startDayOfWeek
                    if (dayIndex in 1..daysInMonth) {
                        val date = currentMonth.withDayOfMonth(dayIndex)
                        val dayEvents = eventsList.filter { it["date"] == date.toString() }
                        val eventColors = dayEvents.map { getUrgencyColor(it["urgency"]?.toString()) }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            DayCell(day = dayIndex, isToday = (LocalDate.now() == date), isSelected = (selectedDate == date), eventColors = eventColors, onClick = { onDayClick(dayIndex) })
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(day: Int, isToday: Boolean, isSelected: Boolean, eventColors: List<Color>, onClick: () -> Unit) {
    val backgroundColor = when {
        isSelected -> Color.White.copy(alpha = 0.3f)
        isToday -> Color(0xFFE53935)
        else -> Color.Transparent
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.size(42.dp).clip(CircleShape).background(backgroundColor).clickable { onClick() },
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = day.toString(), color = Color.White, fontSize = 14.sp, fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            eventColors.take(3).forEach { color ->
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
fun DaysOfWeekHeader() {
    val dayNames = listOf("S", "T", "Q", "Q", "S", "S", "D")
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        dayNames.forEach { day ->
            Text(text = day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

@Composable
fun AddEventDialog(selectedDate: LocalDate, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("${stringResource(id = R.string.new_event_title)} ${selectedDate.dayOfMonth}/${selectedDate.monthValue}", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.label_title), color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), focusedLabelColor = Color.White, cursorColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.label_description), color = Color.White.copy(alpha = 0.6f)) },
                    minLines = 3, modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), focusedLabelColor = Color.White, cursorColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onConfirm(title, description) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), shape = RoundedCornerShape(8.dp)) {
                Text(stringResource(id = R.string.btn_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.btn_cancel), color = Color.White) } }
    )
}

fun getUrgencyColor(urgency: String?): Color {
    return when (urgency?.lowercase()) {
        "alta", "high", "muito alta" -> Color(0xFFE53935)
        "media", "média", "medium" -> Color(0xFFFFB300)
        "baixa", "low" -> Color(0xFF4CAF50)
        "entrevista" -> Color(0xFF2196F3)
        else -> Color.White
    }
}