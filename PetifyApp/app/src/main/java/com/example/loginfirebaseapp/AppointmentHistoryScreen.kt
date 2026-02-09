package com.example.loginfirebaseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppointmentHistoryScreen(
    viewModel: AuthViewModel,
    petId: String,
    onBack: () -> Unit
) {
    val appointments by viewModel.userAppointments
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    // ---TEMA
    val themeId = viewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)

    val pastAppointments = remember(appointments) {
        val now = Date()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        appointments.filter { appt ->
            val dateStr = appt["date"]?.toString() ?: ""
            val status = (appt["status"]?.toString() ?: "pendente").lowercase()

            try {
                val apptDate = sdf.parse(dateStr)
                val isPast = apptDate?.before(now) ?: false

                when (status) {
                    "cancelada", "cancelado" -> true
                    "confirmada", "confirmado", "concluida", "concluido" -> isPast
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    LaunchedEffect(petId) {
        viewModel.fetchPetAppointments(petId)
    }

    Box(modifier = Modifier.fillMaxSize().background(currentTheme.gradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isSmallScreen) 16.dp else 24.dp)
        ) {

            Spacer(modifier = Modifier.height(configuration.screenHeightDp.dp * 0.08f))

            Text(
                text = stringResource(id = R.string.history_title),
                fontSize = if (isSmallScreen) 34.sp else 42.sp,
                fontWeight = FontWeight.Black,
                color = currentTheme.textColor,
                lineHeight = if (isSmallScreen) 38.sp else 48.sp
            )

            Text(
                text = stringResource(id = R.string.history_subtitle),
                fontSize = 14.sp,
                color = currentTheme.textColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (pastAppointments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.history_empty_msg),
                        color = currentTheme.textColor.copy(alpha = 0.4f),
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
                        items = pastAppointments,
                        key = { it["id"]?.toString() ?: it.hashCode().toString() }
                    ) { appointment ->
                        HistoryAppointmentCard(appointment, isSmallScreen)
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
fun HistoryAppointmentCard(appointment: Map<String, Any>, isSmallScreen: Boolean) {
    val reason = appointment["reason"]?.toString() ?: stringResource(id = R.string.default_reason)
    val fullDate = appointment["date"]?.toString() ?: ""
    val price = appointment["price"]?.toString() ?: "0"
    val vet = appointment["vetName"]?.toString() ?: stringResource(id = R.string.default_clinic_name)
    val status = (appointment["status"]?.toString() ?: "concluido").lowercase()

    val badgeColor = when(status) {
        "confirmada", "confirmado", "concluida", "concluido" -> Color(0xFF4CAF50)
        "cancelada", "cancelado" -> Color(0xFFE57373)
        else -> Color.White.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(if (isSmallScreen) 16.dp else 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reason,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = if (isSmallScreen) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = vet,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = badgeColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = status.uppercase(),
                        color = badgeColor.copy(alpha = 0.9f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = stringResource(id = R.string.label_record_date).uppercase(),
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = fullDate,
                        color = Color(0xFFF2B676).copy(alpha = 0.8f),
                        fontSize = if (isSmallScreen) 12.sp else 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.End
                ) {
                    val isCanceled = status == "cancelada" || status == "cancelado"
                    Text(
                        text = if (isCanceled) "STATUS" else stringResource(id = R.string.label_paid_value).uppercase(),
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCanceled) "N/A" else "${price}€",
                        color = Color.White,
                        fontSize = if (isSmallScreen) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}