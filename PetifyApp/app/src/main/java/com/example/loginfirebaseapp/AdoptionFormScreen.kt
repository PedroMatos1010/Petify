package com.example.loginfirebaseapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionFormScreen(
    petId: String,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    val userState by authViewModel.authUiState
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    //LÓGICA DE TEMA E RESPONSIVIDADE
    val themeId = authViewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    // Estados
    val loadingText = stringResource(id = R.string.loading)
    var petNameLoaded by remember { mutableStateOf(loadingText) }
    var petImageUrlLoaded by remember { mutableStateOf("") }
    var nomeAdotante by remember { mutableStateOf(userState.name) }
    var contacto by remember { mutableStateOf(userState.phone) }
    var email by remember { mutableStateOf(userState.email) }
    var morada by remember { mutableStateOf("") }
    var adoptionCenterIdLoaded by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    var housingType by remember { mutableStateOf("") }
    val options = listOf(stringResource(id = R.string.housing_house), stringResource(id = R.string.housing_apartment))

    var showConfirmationDialog by remember { mutableStateOf(false) }

    //FIREBASE
    LaunchedEffect(petId) {
        FirebaseFirestore.getInstance().collection("pets").document(petId).get()
            .addOnSuccessListener { document ->
                petNameLoaded = document.getString("name") ?: "Unknown Pet"
                val imagesList = document.get("images") as? List<String>
                petImageUrlLoaded = imagesList?.firstOrNull()
                    ?: document.getString("displayImage")
                            ?: document.getString("imageUrl")
                            ?: ""
                adoptionCenterIdLoaded = document.getString("adoptionCenterId") ?: ""
            }
    }

    LaunchedEffect(userState.name, userState.phone, userState.email) {
        nomeAdotante = userState.name
        contacto = userState.phone
        email = userState.email
    }

    Box(modifier = Modifier.fillMaxSize().background(currentTheme.gradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isSmallScreen) 16.dp else 24.dp)
        ) {
            // HEADER
            Spacer(modifier = Modifier.height(configuration.screenHeightDp.dp * 0.02f))
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(
                        painterResource(id = R.drawable.menu_icon),
                        null,
                        Modifier.size(30.dp),
                        currentTheme.textColor
                    )
                }

                ProfileAvatar(
                    imageUrl = userState.profileImageUrl,
                    onNavigateToProfile = { /* Navegação */ }
                )
            }

            // TÍTULO
            Text(
                text = stringResource(id = R.string.adoption_title),
                fontSize = if (isSmallScreen) 42.sp else 56.sp,
                lineHeight = if (isSmallScreen) 48.sp else 62.sp,
                fontWeight = FontWeight.Black,
                color = currentTheme.textColor,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // CAMPOS DO FORMULÁRIO
            FormInputField(
                value = petNameLoaded,
                onValueChange = {},
                label = stringResource(id = R.string.label_pet),
                enabled = false,
                isBold = true
            )

            FormInputField(value = nomeAdotante, onValueChange = { nomeAdotante = it }, label = stringResource(id = R.string.label_adopter_name))
            FormInputField(value = contacto, onValueChange = { contacto = it }, label = stringResource(id = R.string.label_contact))
            FormInputField(value = email, onValueChange = { email = it }, label = stringResource(id = R.string.label_email))
            FormInputField(value = morada, onValueChange = { morada = it }, label = stringResource(id = R.string.label_address))

            Spacer(modifier = Modifier.height(8.dp))

            // menu de pre selecao
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = housingType,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(stringResource(id = R.string.placeholder_housing), color = Color.Gray) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    options.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption, color = Color.Black) },
                            onClick = {
                                housingType = selectionOption
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(140.dp)) // Espaço para não bater no FAB
        }

        // BOTÃO VOLTAR
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 24.dp)
                .offset(x = (-12).dp)
        ) {
            CircularBackButton(onBack = onBack)
        }

        // BOTÃO ENVIAR
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
            FloatingActionButton(
                onClick = {
                    if (housingType.isNotEmpty() && morada.isNotEmpty()) {
                        showConfirmationDialog = true
                    } else {
                        Toast.makeText(context, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = currentTheme.textColor,
                shape = CircleShape,
                modifier = Modifier.size(if (isSmallScreen) 56.dp else 64.dp)
            ) {
                val iconColor = if (currentTheme.textColor == Color.White) Color.Black else Color.White
                Icon(
                    Icons.Default.ArrowForward,
                    stringResource(id = R.string.btn_send),
                    tint = iconColor,
                    modifier = Modifier.size(if (isSmallScreen) 28.dp else 32.dp)
                )
            }
        }

        // POPUP DE CONFIRMAÇÃO
        if (showConfirmationDialog) {
            ConfirmAdoptionPopup(
                petName = petNameLoaded,
                onDismiss = { showConfirmationDialog = false },
                onConfirm = {
                    showConfirmationDialog = false
                    val requestId = "${userId}_$petId"
                    val db = FirebaseFirestore.getInstance()
                    val requestRef = db.collection("adoption_requests").document(requestId)

                    requestRef.get().addOnSuccessListener { document ->
                        if (document.exists()) {
                            Toast.makeText(context, "Já enviou um pedido para este pet!", Toast.LENGTH_LONG).show()
                        } else {
                            val requestData = hashMapOf(
                                "adoptionCenterId" to adoptionCenterIdLoaded,
                                "petId" to petId,
                                "petName" to petNameLoaded,
                                "petImageUrl" to petImageUrlLoaded,
                                "userId" to userId,
                                "requestStatus" to "pendente",
                                "status" to "",
                                "interviewDate" to "",
                                "timestamp" to FieldValue.serverTimestamp(),
                                "formData" to hashMapOf(
                                    "fullName" to nomeAdotante,
                                    "email" to email,
                                    "phone" to contacto,
                                    "address" to morada,
                                    "housingType" to housingType
                                )
                            )

                            requestRef.set(requestData)
                                .addOnSuccessListener { onSubmit() }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Erro ao submeter pedido.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun FormInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    isBold: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heightIn(min = 56.dp), // Altura mínima flexível
        placeholder = { Text(text = label, color = Color.Gray, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal) },
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.9f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            disabledTextColor = Color.Black.copy(alpha = 0.7f)
        )
    )
}

@Composable
fun ConfirmAdoptionPopup(petName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(id = R.string.popup_confirm_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.popup_adoption_msg, petName),
                    fontSize = 16.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        border = BorderStroke(1.dp, Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(id = R.string.btn_no), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(id = R.string.btn_yes), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}