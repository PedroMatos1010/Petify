package com.example.loginfirebaseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun AppointmentStatusScreen(
    viewModel: AuthViewModel,
    petId: String,
    onBack: () -> Unit
) {
    val appointments by viewModel.userAppointments
    var appointmentIdToCancel by remember { mutableStateOf<String?>(null) }

    // Lógica de Adaptação
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val screenHeight = configuration.screenHeightDp.dp

    val themeId = viewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)

    val upcomingAppointments = remember(appointments) {
        val now = Date()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        appointments.filter { appt ->
            val status = appt["status"]?.toString() ?: "pendente"
            val dateStr = appt["date"]?.toString() ?: ""
            val isNotCancelled = status != "cancelada" && status != "cancelado"

            try {
                val apptDate = sdf.parse(dateStr)
                val isFutureOrNow = apptDate?.after(now) == true || apptDate?.equals(now) == true
                isFutureOrNow && isNotCancelled
            } catch (e: Exception) {
                isNotCancelled
            }
        }
    }

    LaunchedEffect(petId) {
        viewModel.fetchPetAppointments(petId)
    }

    // Diálogo de Cancelamento
    if (appointmentIdToCancel != null) {
        AlertDialog(
            onDismissRequest = { appointmentIdToCancel = null },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text(
                    text = stringResource(id = R.string.popup_cancel_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.popup_cancel_message),
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        appointmentIdToCancel?.let { id ->
                            viewModel.cancelAppointment(id)
                        }
                        appointmentIdToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text(stringResource(id = R.string.btn_confirm_cancel), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { appointmentIdToCancel = null }) {
                    Text(stringResource(id = R.string.btn_back), color = Color.White)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(currentTheme.gradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isSmallScreen) 16.dp else 24.dp)
        ) {
            Spacer(modifier = Modifier.height(screenHeight * 0.08f))

            Text(
                text = stringResource(id = R.string.status_screen_title),
                fontSize = if (isSmallScreen) 34.sp else 42.sp,
                fontWeight = FontWeight.Black,
                color = currentTheme.textColor,
                lineHeight = if (isSmallScreen) 38.sp else 48.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (upcomingAppointments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.status_empty_msg),
                        color = currentTheme.textColor.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(
                        items = upcomingAppointments,
                        key = { it["id"]?.toString() ?: it.hashCode().toString() }
                    ) { appointment ->
                        AppointmentStatusCard(
                            appointment = appointment,
                            isSmallScreen = isSmallScreen,
                            onCancelClick = { id -> appointmentIdToCancel = id }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 24.dp)
                .offset(x = (-12).dp)
        ) {
            CircularBackButton(onBack = onBack)
        }
    }
}

@Composable
fun AppointmentStatusCard(
    appointment: Map<String, Any>,
    isSmallScreen: Boolean,
    onCancelClick: (String) -> Unit
) {
    val id = appointment["id"]?.toString() ?: ""
    val status = appointment["status"]?.toString() ?: "pendente"
    val reason = appointment["reason"]?.toString() ?: stringResource(id = R.string.default_reason)
    val price = appointment["price"]?.toString() ?: "0"
    val urgency = appointment["urgency"]?.toString() ?: "baixa"
    val fullDate = appointment["date"]?.toString() ?: stringResource(id = R.string.date_not_defined)

    val (statusText, statusColor) = when (status.lowercase()) {
        "confirmed", "confirmado", "approved" -> stringResource(id = R.string.status_confirmed) to Color(0xFF4CAF50)
        "rejected", "recusado", "cancelled", "cancelada", "cancelado" -> stringResource(id = R.string.status_cancelled) to Color(0xFFF44336)
        else -> stringResource(id = R.string.status_pending) to Color(0xFFFFC107)
    }

    val urgencyLabel = when (urgency.lowercase()) {
        "alta", "urgente" -> stringResource(id = R.string.urgency_high)
        "média", "media" -> stringResource(id = R.string.urgency_medium)
        else -> stringResource(id = R.string.urgency_low)
    }

    val urgencyColor = when (urgency.lowercase()) {
        "alta", "urgente" -> Color(0xFFF44336)
        "média", "media" -> Color(0xFFFFC107)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(if (isSmallScreen) 16.dp else 20.dp)) {
            // Header: Motivo e Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = reason,
                    color = Color.White,
                    fontSize = if (isSmallScreen) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText.uppercase(),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Data
            Text(
                text = "📅 $fullDate",
                color = Color(0xFFF2B676).copy(alpha = 0.9f),
                fontSize = if (isSmallScreen) 13.sp else 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Urgência
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.label_urgency).uppercase(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(urgencyColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = urgencyLabel.uppercase(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (status.lowercase() == "pendente" || status.lowercase() == "pending") {
                    Button(
                        onClick = { onCancelClick(id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336).copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_cancel_short).uppercase(),
                            color = Color(0xFFF44336),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(id = R.string.label_est_price).uppercase(),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${price}€",
                            color = Color(0xFFF2B676),
                            fontSize = if (isSmallScreen) 16.sp else 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}