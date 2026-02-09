package com.example.loginfirebaseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.loginfirebaseapp.AuthViewModel
import com.example.loginfirebaseapp.R
import com.example.loginfirebaseapp.ui.AppThemeColors
import com.example.loginfirebaseapp.ui.components.CircularBackButton
import com.example.loginfirebaseapp.ui.components.MenuDrawerContent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

// Data Class
data class PetFirestore(
    val id: String = "",
    val name: String = "",
    val species: String = "",
    val breed: String = "",
    val age: String = "",
    val weight: String = "",
    val sex: String = "",
    val ownerId: String = "",
    val status: String = "",
    val imageUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateFromMenu: (String) -> Unit,
    onNavigateToForm: (String) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Lógica de Adaptação
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val screenHeight = configuration.screenHeightDp.dp

    var selectedPet by remember { mutableStateOf<PetFirestore?>(null) }
    val petsList = remember { mutableStateListOf<PetFirestore>() }

    val uiState by authViewModel.authUiState
    val currentTheme = AppThemeColors.fromId(authViewModel.selectedThemeId.value)

    //ESTADOS
    var searchQuery by remember { mutableStateOf("") }
    var especieSelecionada by remember { mutableStateOf<String?>(null) }
    var racaSelecionada by remember { mutableStateOf<String?>(null) }
    var idadeSelecionada by remember { mutableStateOf<String?>(null) }
    var pesoSelecionado by remember { mutableStateOf<String?>(null) }

    val especiesERacas = remember {
        mapOf(
            "Cão" to listOf("Labrador", "Pastor Alemão", "Bulldog", "Poodle", "Golden Retriever", "Beagle", "SRD (Rafeiro)", "Outro"),
            "Gato" to listOf("Persa", "Siamês", "Maine Coon", "Bengal", "SRD (Rafeiro)", "Outro"),
            "Pássaro" to listOf("Canário", "Papagaio", "Periquito", "Outro"),
            "Outro" to listOf("Coelho", "Hamster", "Tartaruga", "Outro")
        )
    }
    val faixasEtarias = listOf("Filhote (<1 ano)", "Jovem (1-3 anos)", "Adulto (>3 anos)")
    val faixasPeso = listOf("Pequeno (<5kg)", "Médio (5-15kg)", "Grande (>15kg)")

    // Lógica dos filtros
    val filteredPets = petsList.filter { pet ->
        val matchesSearch = pet.name.contains(searchQuery, ignoreCase = true)
        val matchesEspecie = especieSelecionada == null || pet.species.equals(especieSelecionada, ignoreCase = true)
        val matchesRaca = racaSelecionada == null || pet.breed.equals(racaSelecionada, ignoreCase = true)
        val idadeNum = pet.age.filter { it.isDigit() }.toIntOrNull() ?: 0
        val pesoNum = pet.weight.replace(",", ".").filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0

        val matchesIdade = when (idadeSelecionada) {
            "Filhote (<1 ano)" -> idadeNum < 1
            "Jovem (1-3 anos)" -> idadeNum in 1..3
            "Adulto (>3 anos)" -> idadeNum > 3
            else -> true
        }
        val matchesPeso = when (pesoSelecionado) {
            "Pequeno (<5kg)" -> pesoNum < 5.0
            "Médio (5-15kg)" -> pesoNum in 5.0..15.0
            "Grande (>15kg)" -> pesoNum > 15.0
            else -> true
        }
        matchesSearch && matchesEspecie && matchesRaca && matchesIdade && matchesPeso
    }

    // Firebase
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").whereEqualTo("role", "ORGANIZATION").get().addOnSuccessListener { userDocs ->
            val orgIds = userDocs.documents.map { it.id }
            db.collection("pets")
                .whereEqualTo("status", "available")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val allPets = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(PetFirestore::class.java)?.copy(id = doc.id)
                        }
                        petsList.clear()
                        petsList.addAll(allPets.filter { it.ownerId.isEmpty() || orgIds.contains(it.ownerId) })
                    }
                }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isSmallScreen) 16.dp else 24.dp)
            ) {
                // HEADER
                Spacer(modifier = Modifier.height(screenHeight * 0.02f))
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(painterResource(R.drawable.menu_icon), null, Modifier.size(30.dp), currentTheme.textColor)
                    }
                    ProfileAvatar(imageUrl = uiState.profileImageUrl, onNavigateToProfile = onNavigateToProfile)
                }

                // TÍTULO
                Text(
                    text = stringResource(id = R.string.adoption_title),
                    fontSize = if (isSmallScreen) 40.sp else 56.sp,
                    lineHeight = if (isSmallScreen) 44.sp else 60.sp,
                    fontWeight = FontWeight.Black,
                    color = currentTheme.textColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                CustomSearchBar(query = searchQuery, onQueryChange = { searchQuery = it }, textColor = currentTheme.textColor)

                Spacer(modifier = Modifier.height(12.dp))

                // FILTROS
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterTag("Todos", especieSelecionada == null, { especieSelecionada = null; racaSelecionada = null }, currentTheme) }
                        items(especiesERacas.keys.toList()) { esp ->
                            FilterTag(esp, especieSelecionada == esp, { especieSelecionada = if (especieSelecionada == esp) null else esp; racaSelecionada = null }, currentTheme)
                        }
                    }

                    if (especieSelecionada != null) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterTag("Todas as Raças", racaSelecionada == null, { racaSelecionada = null }, currentTheme) }
                            items(especiesERacas[especieSelecionada] ?: emptyList()) { raca ->
                                FilterTag(raca, racaSelecionada == raca, { racaSelecionada = if (racaSelecionada == raca) null else raca }, currentTheme)
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterTag("Idade", idadeSelecionada == null, { idadeSelecionada = null }, currentTheme) }
                        items(faixasEtarias) { f -> FilterTag(f, idadeSelecionada == f, { idadeSelecionada = if (idadeSelecionada == f) null else f }, currentTheme) }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterTag("Peso", pesoSelecionado == null, { pesoSelecionado = null }, currentTheme) }
                        items(faixasPeso) { f -> FilterTag(f, pesoSelecionado == f, { pesoSelecionado = if (pesoSelecionado == f) null else f }, currentTheme) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LISTA
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredPets) { pet ->
                        AdoptionPetCard(
                            pet = pet,
                            isFavorite = uiState.favoritePets.contains(pet.id),
                            onCardClick = { selectedPet = pet },
                            onFavoriteClick = { authViewModel.toggleFavorite(pet.id) }
                        )
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
                            Text("Raça: ${pet.breed}", color = Color.White)
                            Text("Espécie: ${pet.species}", color = Color.White)
                            Text("Idade: ${pet.age}", color = Color.White)
                            Text("Peso: ${pet.weight}kg", color = Color.White)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val id = pet.id
                                selectedPet = null
                                onNavigateToForm(id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ADOTAR", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedPet = null }) {
                            Text("CANCELAR", color = Color.Gray)
                        }
                    }
                )
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
        }
    }
}

@Composable
fun FilterTag(label: String, isSelected: Boolean, onClick: () -> Unit, theme: AppThemeColors) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) theme.textColor else theme.textColor.copy(alpha = 0.1f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (isSelected) Color.Black else theme.textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CustomSearchBar(query: String, onQueryChange: (String) -> Unit, textColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = textColor.copy(alpha = 0.1f)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = textColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                decorationBox = { inner ->
                    if(query.isEmpty()) Text("Pesquisar pet...", color = textColor.copy(0.3f))
                    inner()
                }
            )
        }
    }
}

@Composable
fun AdoptionPetCard(pet: PetFirestore, isFavorite: Boolean, onCardClick: () -> Unit, onFavoriteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp).clickable { onCardClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(85.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.05f))) {
                if (pet.imageUrl.isNotEmpty()) {
                    AsyncImage(model = pet.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(
                        painter = painterResource(if (pet.species.lowercase().contains("gato")) R.drawable.miau_image else R.drawable.josh_image),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.padding(12.dp).align(Alignment.Center)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(pet.name.uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(pet.breed, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(color = Color(0xFFC5935F).copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                    Text(pet.sex, color = Color(0xFFC5935F), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else Color.White.copy(0.3f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}