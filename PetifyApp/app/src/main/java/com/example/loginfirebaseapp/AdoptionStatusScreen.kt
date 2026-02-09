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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

//data
data class AdoptionReq(
    val id: String = "",
    val petName: String = "",
    val requestStatus: String = "pendente",
    val status: String = "",
    val petImageUrl: String = "",
    val interviewDate: String = ""
)

@Composable
fun AdoptionStatusScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var requests by remember { mutableStateOf<List<AdoptionReq>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val themeId = authViewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)

    // Lógica de Adaptação
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val screenHeight = configuration.screenHeightDp.dp

    LaunchedEffect(userId) {
        if (userId.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("adoption_requests")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(AdoptionReq::class.java)?.copy(id = doc.id)
                    }
                    requests = items
                }
                isLoading = false
            }
    }

    Box(modifier = Modifier.fillMaxSize().background(currentTheme.gradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isSmallScreen) 16.dp else 24.dp)
        ) {
            Spacer(modifier = Modifier.height(screenHeight * 0.08f))

            Text(
                text = stringResource(id = R.string.adoption_status_title),
                fontSize = if (isSmallScreen) 34.sp else 42.sp,
                fontWeight = FontWeight.Black,
                color = currentTheme.textColor,
                lineHeight = if (isSmallScreen) 38.sp else 48.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = currentTheme.textColor)
                }
            } else if (requests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.no_requests_msg),
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
                    items(requests) { request ->
                        AdoptionStatusCard(request = request, isSmallScreen = isSmallScreen)
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
fun AdoptionStatusCard(request: AdoptionReq, isSmallScreen: Boolean) {
    // 1. Definição de Cores e texto baseados no Status
    val (statusDisplay, statusColor) = when {
        request.status == "accepted" || request.status == "confirmado" ->
            stringResource(id = R.string.status_approved) to Color(0xFF4CAF50)
        request.status == "rejected" ->
            stringResource(id = R.string.status_rejected) to Color(0xFFF44336)
        request.requestStatus == "aceite para entrevista" ->
            "ENTREVISTA" to Color(0xFF2196F3)
        else ->
            stringResource(id = R.string.status_pending).uppercase() to Color(0xFFFFC107)
    }

    // 2. Lógica de Mensagem
    val (scheduleText, scheduleTextColor) = when {

        request.status == "accepted" || request.status == "confirmado" ->
            "🎉 Parabéns! O teu pedido foi aceite." to Color(0xFF4CAF50)


        request.status == "rejected" ->
            "❌ Processo de adoção encerrado." to Color(0xFFE57373)


        request.interviewDate.isNotEmpty() ->
            "📅 Entrevista: ${request.interviewDate}" to Color(0xFFF2B676)


        else ->
            "⏳ Aguardando análise da equipa" to Color(0xFFF2B676)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(if (isSmallScreen) 16.dp else 20.dp)) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = request.petName.uppercase(),
                    color = Color.White,
                    fontSize = if (isSmallScreen) 18.sp else 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusDisplay,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "RESULTADO",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // INFO BOX
            Surface(
                color = scheduleTextColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = scheduleText,
                    color = scheduleTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // FOOTER: ID e Passo Atual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ID DO PEDIDO",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "#${request.id.takeLast(6).uppercase()}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = request.requestStatus.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}