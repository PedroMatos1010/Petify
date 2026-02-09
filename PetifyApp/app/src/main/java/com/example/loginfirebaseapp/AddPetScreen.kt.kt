package com.example.loginfirebaseapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    //ESTADOS DO FORMULÁRIO
    var name by remember { mutableStateOf("") }
    var especieSelecionada by remember { mutableStateOf("") }
    var racaSelecionada by remember { mutableStateOf("") }
    var racaPersonalizada by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var age by remember { mutableStateOf("") }

    // Estados de UI
    var expandidoEspecie by remember { mutableStateOf(false) }
    var expandidoRaca by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = configuration.screenWidthDp < 360

    val themeId = viewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)


    val especiesERacas = remember {
        mapOf(
            "Cão" to listOf("Labrador", "Pastor Alemão", "Bulldog", "Poodle", "SRD (Rafeiro)", "Outro"),
            "Gato" to listOf("Persa", "Siamês", "Maine Coon", "SRD (Rafeiro)", "Outro"),
            "Pássaro" to listOf("Canário", "Papagaio", "Periquito", "Outro"),
            "Coelho" to listOf("Anão Holandês", "Belier", "Angorá", "Outro"),
            "Hamster" to listOf("Sírio", "Russo", "Outro")
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> imageUris = imageUris + uris }

    val isFormValid = name.isNotEmpty() && especieSelecionada.isNotEmpty() &&
            (racaSelecionada.isNotEmpty() && (racaSelecionada != "Outro" || racaPersonalizada.isNotEmpty()))

    Box(modifier = Modifier.fillMaxSize().background(currentTheme.gradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isSmallScreen) 16.dp else 24.dp)
        ) {

            Spacer(modifier = Modifier.height(screenHeight * 0.08f))

            //Cabeçalho
            AddPetHeader(theme = currentTheme)

            Spacer(modifier = Modifier.height(32.dp))

            //Campos de Texto principais
            PetInputField(label = "Nome", value = name, onValueChange = { name = it }, placeholder = "Ex: Bobi", themeColors = currentTheme)

            // Menu de abrir de Espécie
            Text("Espécie", color = currentTheme.textColor.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            ExposedDropdownMenuBox(
                expanded = expandidoEspecie,
                onExpandedChange = { expandidoEspecie = !expandidoEspecie },
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                TextField(
                    value = especieSelecionada,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Selecione a espécie", color = currentTheme.textColor.copy(alpha = 0.2f)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoEspecie) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = textFieldColors(currentTheme),
                    textStyle = TextStyle(color = currentTheme.textColor, fontSize = 16.sp)
                )
                ExposedDropdownMenu(expanded = expandidoEspecie, onDismissRequest = { expandidoEspecie = false }) {
                    especiesERacas.keys.forEach { especie ->
                        DropdownMenuItem(text = { Text(especie) }, onClick = { especieSelecionada = especie; racaSelecionada = ""; expandidoEspecie = false })
                    }
                }
            }

            // Menu  de abrir de Raça
            if (especieSelecionada.isNotEmpty()) {
                Text("Raça", color = currentTheme.textColor.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = expandidoRaca,
                    onExpandedChange = { expandidoRaca = !expandidoRaca },
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    TextField(
                        value = racaSelecionada,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Selecione a raça", color = currentTheme.textColor.copy(alpha = 0.2f)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoRaca) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = textFieldColors(currentTheme),
                        textStyle = TextStyle(color = currentTheme.textColor, fontSize = 16.sp)
                    )
                    ExposedDropdownMenu(expanded = expandidoRaca, onDismissRequest = { expandidoRaca = false }) {
                        especiesERacas[especieSelecionada]?.forEach { raca ->
                            DropdownMenuItem(text = { Text(raca) }, onClick = { racaSelecionada = raca; expandidoRaca = false })
                        }
                    }
                }
            }

            if (racaSelecionada == "Outro") {
                PetInputField(label = "Especifique a Raça", value = racaPersonalizada, onValueChange = { racaPersonalizada = it }, placeholder = "Qual a raça?", themeColors = currentTheme)
            }

            // Filtro: Só aceita números em idade e peso
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    PetInputField(
                        label = "Idade",
                        value = age,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) age = newValue
                        },
                        placeholder = "Ex: 2",
                        themeColors = currentTheme,
                        keyboardType = KeyboardType.Number
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    PetInputField(
                        label = "Peso (kg)",
                        value = weight,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.count { it == '.' || it == ',' } <= 1 &&
                                newValue.all { it.isDigit() || it == '.' || it == ',' }) {
                                weight = newValue.replace(',', '.')
                            }
                        },
                        placeholder = "Ex: 4.5",
                        themeColors = currentTheme,
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))


            SaveButton(
                isLoading = isLoading,
                enabled = isFormValid && !isLoading,
                theme = currentTheme,
                onClick = {
                    isLoading = true
                    val racaFinal = if (racaSelecionada == "Outro") racaPersonalizada else racaSelecionada
                    viewModel.savePet(name, especieSelecionada, racaFinal, age, weight, imageUris) {
                        isLoading = false
                        onBack()
                    }
                }
            )

            Spacer(modifier = Modifier.height(120.dp))
        }

        // Botão de Voltar Fixo
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 32.dp).offset(x = (-12).dp)) {
            CircularBackButton(onBack = onBack)
        }
    }
}

//SUB-COMPONENTES

@Composable
fun AddPetHeader(theme: AppThemeColors) {
    Column {
        Text(
            text = "Adicionar Novo Pet",
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Black,
            color = theme.textColor
        )
        Text(
            text = "Preencha os detalhes do seu companheiro",
            fontSize = 14.sp,
            color = theme.textColor.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun PhotoSection(
    imageUris: List<Uri>,
    onAddClick: () -> Unit,
    onRemoveClick: (Uri) -> Unit,
    theme: AppThemeColors,
    screenWidth: androidx.compose.ui.unit.Dp
) {
    // Foto maior em tablets, menor em telemóveis pequenos
    val imageSize = if (screenWidth < 360.dp) 100.dp else 125.dp

    Column {
        Text("Fotos do Pet", color = theme.textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botão Adicionar
            Box(
                modifier = Modifier.size(imageSize).clip(RoundedCornerShape(24.dp))
                    .background(theme.textColor.copy(alpha = 0.1f))
                    .border(1.dp, theme.textColor.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = theme.textColor.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
            }

            // Lista de fotos
            imageUris.forEachIndexed { index, uri ->
                Box(modifier = Modifier.size(imageSize).clip(RoundedCornerShape(24.dp))) {
                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

                    //foto principal
                    if (index == 0) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(theme.textColor.copy(alpha = 0.7f)).padding(2.dp), contentAlignment = Alignment.Center) {
                            Text("CAPA", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = { onRemoveClick(uri) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).background(Color.Black.copy(0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SaveButton(isLoading: Boolean, enabled: Boolean, theme: AppThemeColors, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = theme.textColor,
            disabledContainerColor = theme.textColor.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(18.dp),
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = if (theme.textColor == Color.White) Color.Black else Color.White, modifier = Modifier.size(24.dp))
        } else {
            Text("Salvar Pet", color = if (theme.textColor == Color.White) Color.Black else Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun PetInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    themeColors: AppThemeColors, // Recebe o tema
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(
            text = label,
            color = themeColors.textColor.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = themeColors.textColor, fontSize = 16.sp),
            placeholder = {
                Text(placeholder, color = themeColors.textColor.copy(alpha = 0.3f))
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = textFieldColors(themeColors),
            singleLine = true
        )
    }
}

@Composable
fun textFieldColors(theme: AppThemeColors) = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    cursorColor = theme.textColor,

    // COR DO TEXTO
    focusedTextColor = theme.textColor,
    unfocusedTextColor = theme.textColor,

    // COR DA LINHA
    focusedIndicatorColor = theme.textColor,
    unfocusedIndicatorColor = theme.textColor.copy(alpha = 0.2f),

    // COR DA LABEL
    focusedLabelColor = theme.textColor,
    unfocusedLabelColor = theme.textColor.copy(alpha = 0.6f)
)