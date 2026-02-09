package com.example.loginfirebaseapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton

@Composable
fun EditPetScreen(
    authViewModel: AuthViewModel,
    petId: String?,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        authViewModel.fetchUserPets()
    }

    val userPets by authViewModel.userPets
    val themeId = authViewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)


    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 650

    val pet = remember(userPets, petId) {
        userPets.find { it["id"].toString() == petId.toString() }
    }

    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    var existingImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var newImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        newImageUris = newImageUris + uris
    }

    LaunchedEffect(pet) {
        pet?.let {
            if (!isEditing) {
                name = it["name"]?.toString() ?: ""
                breed = it["breed"]?.toString() ?: ""
                age = it["age"]?.toString() ?: ""
                weight = it["weight"]?.toString() ?: ""
                existingImages = it["images"] as? List<String> ?: emptyList()
            }
        }
    }

    if (pet == null) {
        Box(modifier = Modifier.fillMaxSize().background(currentTheme.gradient), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = currentTheme.textColor)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(currentTheme.gradient)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(if (isSmallScreen) 40.dp else 60.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "EDITAR\nPET" else "DETALHES\nDO PET",
                        fontSize = if (isSmallScreen) 32.sp else 42.sp,
                        fontWeight = FontWeight.Black,
                        color = currentTheme.textColor,
                        lineHeight = if (isSmallScreen) 36.sp else 46.sp,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remover",
                            tint = if (themeId == 2) Color(0xFFFF5252) else Color.Red,
                            modifier = Modifier.size(if (isSmallScreen) 28.dp else 32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 32.dp))

                EditPetLabel("FOTOS DO COMPANHEIRO", currentTheme.textColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isEditing) {
                        Box(
                            modifier = Modifier
                                .size(if (isSmallScreen) 110.dp else 140.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(currentTheme.textColor.copy(alpha = 0.1f))
                                .border(1.dp, currentTheme.textColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = currentTheme.textColor)
                        }
                    }

                    existingImages.forEach { url ->
                        PhotoBox(model = url, isEditing = isEditing, isSmallScreen = isSmallScreen, onRemove = {
                            existingImages = existingImages.filter { it != url }
                        })
                    }

                    newImageUris.forEach { uri ->
                        PhotoBox(model = uri, isEditing = isEditing, isSmallScreen = isSmallScreen, onRemove = {
                            newImageUris = newImageUris.filter { it != uri }
                        })
                    }

                    if (!isEditing && existingImages.isEmpty()) {
                        Box(modifier = Modifier.size(if (isSmallScreen) 110.dp else 140.dp).clip(RoundedCornerShape(24.dp)).background(currentTheme.textColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.miau_face_icon), null, tint = currentTheme.textColor.copy(alpha = 0.2f), modifier = Modifier.size(if (isSmallScreen) 40.dp else 50.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 32.dp))

                EditPetLabel("NOME DO PET", currentTheme.textColor)
                EditPetInputField(value = name, enabled = isEditing, onValueChange = { name = it })

                Spacer(modifier = Modifier.height(16.dp))

                EditPetLabel("RAÇA", currentTheme.textColor)
                EditPetInputField(value = breed, enabled = isEditing, onValueChange = { breed = it })

                Spacer(modifier = Modifier.height(16.dp))

                EditPetLabel("IDADE", currentTheme.textColor)
                EditPetInputField(value = age, enabled = isEditing, onValueChange = { age = it })

                Spacer(modifier = Modifier.height(16.dp))

                EditPetLabel("PESO (KG)", currentTheme.textColor)
                EditPetInputField(value = weight, enabled = isEditing, onValueChange = { weight = it })

                Spacer(modifier = Modifier.height(if (isSmallScreen) 32.dp else 48.dp))

                Button(
                    onClick = {
                        if (isEditing) {
                            isLoading = true
                            authViewModel.updatePet(
                                petId = petId!!,
                                name = name,
                                breed = breed,
                                age = age,
                                weight = weight,
                                existingImages = existingImages,
                                newImageUris = newImageUris
                            ) {
                                isLoading = false
                                isEditing = false
                                newImageUris = emptyList()
                            }
                        } else {
                            isEditing = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(if (isSmallScreen) 50.dp else 60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditing) Color(0xFF4CAF50) else currentTheme.textColor,
                        contentColor = if (isEditing) Color.White else (if (currentTheme.textColor == Color.White) Color.Black else Color.White)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            text = if (isEditing) "GUARDAR ALTERAÇÕES" else "EDITAR INFORMAÇÕES",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isSmallScreen) 16.sp else 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(140.dp)) // Espaço extra para scroll total
            }

            //BOTÃO VOLTAR
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 32.dp).offset(x = (-16).dp)) {
                CircularBackButton(onBack = onBack)
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    containerColor = Color(0xFF1A1A1A),
                    title = { Text("Eliminar Pet", color = Color.White) },
                    text = { Text("Tens a certeza que queres eliminar o ${name}?", color = Color.White.copy(alpha = 0.7f)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            authViewModel.deletePet(petId!!) { onBack() }
                        }) {
                            Text("ELIMINAR", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("CANCELAR", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PhotoBox(model: Any, isEditing: Boolean, isSmallScreen: Boolean, onRemove: () -> Unit) {
    val size = if (isSmallScreen) 110.dp else 140.dp
    Box(modifier = Modifier.size(size).clip(RoundedCornerShape(24.dp))) {
        AsyncImage(model = model, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (isEditing) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun EditPetLabel(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = color.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}

@Composable
fun EditPetInputField(value: String, onValueChange: (String) -> Unit = {}, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.85f), // Melhorei a visibilidade do texto desativado
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            disabledTextColor = Color.Black
        )
    )
}