package com.example.loginfirebaseapp.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton
import com.example.loginfirebaseapp.ui.components.MenuDrawerContent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.launch

@Composable
fun FavoritePetsScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToForm: (String) -> Unit,
    onNavigateFromMenu: (String) -> Unit
) {
    val uiState by authViewModel.authUiState
    val favoriteIds = uiState.favoritePets
    val favoritePetsList = remember { mutableStateListOf<PetFirestore>() }
    var isLoading by remember { mutableStateOf(false) }

    //TEMA DINÂMICO
    val themeId = authViewModel.selectedThemeId.value
    val currentTheme = AppThemeColors.fromId(themeId)

    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenHeightDp < 650

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedPet by remember { mutableStateOf<PetFirestore?>(null) }

    LaunchedEffect(favoriteIds) {
        if (favoriteIds.isEmpty()) {
            favoritePetsList.clear()
            return@LaunchedEffect
        }
        isLoading = true
        val db = FirebaseFirestore.getInstance()
        db.collection("pets")
            .whereIn(FieldPath.documentId(), favoriteIds)
            .get()
            .addOnSuccessListener { snapshot ->
                val pets = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PetFirestore::class.java)?.copy(id = doc.id)
                }
                favoritePetsList.clear()
                favoritePetsList.addAll(pets)
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("FAVORITOS", "Erro ao carregar: ${e.message}")
                isLoading = false
            }
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
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {

                // HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (isSmallScreen) 16.dp else 24.dp)
                        .height(56.dp),
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

                    IconButton(onClick = onNavigateToHome) {
                        Icon(
                            painter = painterResource(id = R.drawable.home_icon),
                            contentDescription = stringResource(id = R.string.btn_home),
                            modifier = Modifier.size(if (isSmallScreen) 26.dp else 30.dp),
                            tint = currentTheme.textColor
                        )
                    }
                }

                Text(
                    text = stringResource(id = R.string.favorites_title),
                    fontSize = if (isSmallScreen) 44.sp else 56.sp,
                    fontWeight = FontWeight.Black,
                    color = currentTheme.textColor,
                    lineHeight = if (isSmallScreen) 48.sp else 60.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = currentTheme.textColor)
                    }
                } else if (favoritePetsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(id = R.string.no_favorites_msg),
                            color = currentTheme.textColor.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(favoritePetsList) { pet ->
                            AdoptionPetCard(
                                pet = pet,
                                isFavorite = true,
                                onCardClick = { selectedPet = pet },
                                onFavoriteClick = { authViewModel.toggleFavorite(pet.id) }
                            )
                        }
                    }
                }
            }

            // POP-UP DE DETALHES
            selectedPet?.let { pet ->
                AlertDialog(
                    onDismissRequest = { selectedPet = null },
                    containerColor = Color(0xFF1E1E1E),
                    title = { Text(pet.name.uppercase(), color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("${stringResource(id = R.string.pet_breed)}: ${pet.breed}", color = Color.White)
                            Text("${stringResource(id = R.string.pet_species)}: ${pet.species}", color = Color.White)
                            Text("${stringResource(id = R.string.pet_sex)}: ${pet.sex}", color = Color.White)
                            Text("${stringResource(id = R.string.pet_age)}: ${pet.age} ${stringResource(id = R.string.years)}", color = Color.White)
                            Text("${stringResource(id = R.string.pet_weight)}: ${pet.weight}kg", color = Color.White)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val petId = pet.id
                                selectedPet = null
                                onNavigateToForm(petId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(stringResource(id = R.string.btn_adopt), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedPet = null }) {
                            Text(stringResource(id = R.string.btn_cancel), color = Color.Gray)
                        }
                    }
                )
            }

            //BOTÃO VOLTAR
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 24.dp)
                    .offset(x = (-16).dp)
            ) {
                CircularBackButton(onBack = onBack)
            }
        }
    }
}