package com.example.loginfirebaseapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
import java.text.SimpleDateFormat
import java.util.Locale

//DATAS
class DateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1 || i == 3) out += "/"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 3) return offset + 1
                if (offset <= 8) return offset + 2
                return 10
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                if (offset <= 10) return offset - 2
                return 8
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

fun formatDateWithSlashes(date: String): String {
    return if (date.length == 8) {
        "${date.substring(0, 2)}/${date.substring(2, 4)}/${date.substring(4, 8)}"
    } else date
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVaccineScreen(
    viewModel: AuthViewModel,
    petId: String,
    onBack: () -> Unit
) {
    // ESTADOS
    var name by remember { mutableStateOf("") }
    var dateAdministered by remember { mutableStateOf("") }
    var validUntil by remember { mutableStateOf("") }
    var batchNumber by remember { mutableStateOf("") }
    var vetName by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    //LÓGICA DE ADAPTAÇÃO E TEMA
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isSmallScreen = configuration.screenWidthDp < 360

    val themeId = viewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)
    val dateFormatter = remember { SimpleDateFormat("ddMMyyyy", Locale.getDefault()) }

    //Validações
    fun isDateStructureValid(date: String): Boolean {
        if (date.length != 8) return false
        val day = date.substring(0, 2).toIntOrNull() ?: 0
        val month = date.substring(2, 4).toIntOrNull() ?: 0
        return day in 1..31 && month in 1..12
    }

    val isDateSequenceValid = remember(dateAdministered, validUntil) {
        if (dateAdministered.length == 8 && validUntil.length == 8) {
            try {
                val date1 = dateFormatter.parse(dateAdministered)
                val date2 = dateFormatter.parse(validUntil)
                date2?.after(date1) ?: false
            } catch (e: Exception) { false }
        } else true
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    Box(modifier = Modifier.fillMaxSize().background(currentTheme.gradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isSmallScreen) 16.dp else 24.dp)
        ) {

            Spacer(modifier = Modifier.height(screenHeight * 0.07f))

            //Cabeçalho
            VaccineHeader(theme = currentTheme)

            Spacer(modifier = Modifier.height(32.dp))

            //Formulário
            VaccineInputField(
                label = stringResource(id = R.string.label_vaccine_name),
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(id = R.string.placeholder_vaccine_name),
                themeColors = currentTheme
            )

            VaccineInputField(
                label = stringResource(id = R.string.label_dosage_date),
                value = dateAdministered,
                onValueChange = { if (it.length <= 8) dateAdministered = it.filter { c -> c.isDigit() } },
                placeholder = "DD/MM/AAAA",
                isDate = true,
                themeColors = currentTheme
            )

            VaccineInputField(
                label = stringResource(id = R.string.label_next_boost),
                value = validUntil,
                onValueChange = { if (it.length <= 8) validUntil = it.filter { c -> c.isDigit() } },
                placeholder = "DD/MM/AAAA",
                isDate = true,
                isError = !isDateSequenceValid,
                errorText = stringResource(id = R.string.error_date_sequence),
                themeColors = currentTheme
            )

            // NÚMERO DO LOTE
            VaccineInputField(
                label = stringResource(id = R.string.label_batch_number),
                value = batchNumber,
                onValueChange = { batchNumber = it },
                placeholder = stringResource(id = R.string.placeholder_optional),
                themeColors = currentTheme,
                keyboardType = KeyboardType.Number
            )

            VaccineInputField(
                label = stringResource(id = R.string.label_veterinarian),
                value = vetName,
                onValueChange = { vetName = it },
                placeholder = stringResource(id = R.string.placeholder_vet_name),
                themeColors = currentTheme
            )

            Spacer(modifier = Modifier.height(24.dp))

            //Foto do Comprovativo
            PhotoStampSection(imageUri, currentTheme) { launcher.launch("image/*") }

            Spacer(modifier = Modifier.height(40.dp))

            // confirmacao se data de reforco superior
            val isFormValid = name.isNotEmpty() &&
                    isDateStructureValid(dateAdministered) &&
                    isDateStructureValid(validUntil) &&
                    isDateSequenceValid

            SaveVaccineButton(
                isLoading = isLoading,
                enabled = isFormValid && !isLoading,
                theme = currentTheme,
                onClick = {
                    isLoading = true
                    viewModel.saveVaccineRecord(
                        petId = petId,
                        name = name,
                        dateAdministered = formatDateWithSlashes(dateAdministered),
                        validUntil = formatDateWithSlashes(validUntil),
                        batchNumber = batchNumber,
                        vetName = vetName,
                        imageUri = imageUri,
                        onSuccess = { isLoading = false; onBack() }
                    )
                }
            )
            Spacer(modifier = Modifier.height(120.dp))
        }

        // Botão Voltar Fixo
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

//SUB-COMPONENTES

@Composable
fun VaccineHeader(theme: AppThemeColors) {
    Column {
        Text(
            text = stringResource(id = R.string.add_vaccine_title),
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Black,
            color = theme.textColor
        )
        Text(
            text = stringResource(id = R.string.add_vaccine_subtitle),
            fontSize = 14.sp,
            color = theme.textColor.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun PhotoStampSection(uri: Uri?, theme: AppThemeColors, onClick: () -> Unit) {
    Text(
        text = stringResource(id = R.string.label_photo_stamp),
        color = theme.textColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(theme.textColor.copy(alpha = 0.1f))
            .border(1.dp, theme.textColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painter = painterResource(id = R.drawable.health_icon), contentDescription = null, tint = theme.textColor.copy(alpha = 0.3f))
                Text(text = stringResource(id = R.string.attach_card_photo), color = theme.textColor.copy(alpha = 0.3f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SaveVaccineButton(isLoading: Boolean, enabled: Boolean, theme: AppThemeColors, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = theme.textColor,
            disabledContainerColor = theme.textColor.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = if (theme.textColor == Color.White) Color.Black else Color.White, modifier = Modifier.size(24.dp))
        } else {
            Text(stringResource(id = R.string.btn_save_bulletin), color = if (theme.textColor == Color.White) Color.Black else Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccineInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isDate: Boolean = false,
    isError: Boolean = false,
    errorText: String = "",
    themeColors: AppThemeColors,
    keyboardType: KeyboardType? = null // Permite forçar o teclado numérico
) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(
            text = label,
            color = if (isError) Color.Red else themeColors.textColor.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = themeColors.textColor, fontSize = 16.sp),
            placeholder = { Text(placeholder, color = themeColors.textColor.copy(alpha = 0.2f)) },
            isError = isError,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType ?: if (isDate) KeyboardType.Number else KeyboardType.Text
            ),
            visualTransformation = if (isDate) DateTransformation() else VisualTransformation.None,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = themeColors.textColor,
                unfocusedTextColor = themeColors.textColor,
                focusedIndicatorColor = if (isError) Color.Red else themeColors.textColor,
                unfocusedIndicatorColor = if (isError) Color.Red.copy(alpha = 0.5f) else themeColors.textColor.copy(alpha = 0.1f),
                cursorColor = themeColors.textColor
            ),
            singleLine = true
        )
        if (isError) {
            Text(text = errorText, color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}