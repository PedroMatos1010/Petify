package com.example.loginfirebaseapp

import android.content.Context
import android.os.Bundle
import android.app.Application
import android.app.Activity
import androidx.fragment.app.FragmentActivity // ALTERADO: Necessário para o Processing
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.loginfirebaseapp.ui.theme.LoginFirebaseAppTheme
import com.jakewharton.threetenabp.AndroidThreeTen
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build

import com.example.loginfirebaseapp.ui.screens.AdoptionStatusScreen

// --- IMPORTAÇÕES DE TELAS ---
import com.example.loginfirebaseapp.i18n.ProvideStringResources
import com.example.loginfirebaseapp.ui.screens.*


class MainActivity : FragmentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this)

        //IDIOMA
        val sharedPref = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val savedLang = sharedPref.getString("language", "pt") ?: "pt"

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return SettingsViewModel(authViewModel, applicationContext as Application) as T
                    }
                }
            )

            val settingsState by settingsViewModel.uiState.collectAsState()
            val currentLang = settingsState.currentLanguage
            val context = androidx.compose.ui.platform.LocalContext.current


            LaunchedEffect(currentLang) {
                updateLocale(context, currentLang)
            }


            LaunchedEffect(Unit) {
                settingsViewModel.loadSettings(savedLang, 1)

            }

            LoginFirebaseAppTheme {
                ProvideStringResources(currentLocale = currentLang) {

                    MyApp(authViewModel = authViewModel, settingsViewModel = settingsViewModel, intent = intent)
                }
            }
        }
    }

    // Função para forçar a atualização da configuração de recursos do Android
    private fun updateLocale(context: Context, languageCode: String) {
        val locale = java.util.Locale(languageCode)
        java.util.Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.createConfigurationContext(config)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}

// tema
val loginBackgroundBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFF2B676).copy(alpha = 0.8f),
        Color(0xFF715639).copy(alpha = 0.8f),
        Color(0xFF969696).copy(alpha = 0.8f)
    ),
    start = Offset.Zero,
    end = Offset.Infinite
)

@Composable
fun MyApp(authViewModel: AuthViewModel, settingsViewModel: SettingsViewModel, intent: android.content.Intent) {
    val navController = rememberNavController()
    val authState by authViewModel.authUiState

    //PERMISSÃO DE NOTIFICAÇÕES
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            authViewModel.updateFCMToken()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            authViewModel.updateFCMToken()
        }
    }


    val shouldOpenSettings = intent.getBooleanExtra("OPEN_SETTINGS", false)


    var isInitialNavigationProcessed by remember { mutableStateOf(false) }

    val initialRoute = when {
        shouldOpenSettings -> "settings"
        FirebaseAuth.getInstance().currentUser != null -> "home"
        else -> "auth"
    }


    SideEffect {
        if (shouldOpenSettings) {
            intent.removeExtra("OPEN_SETTINGS")
        }
    }

    val settingsState by settingsViewModel.uiState.collectAsState()
    val currentLang = settingsState.currentLanguage

    // Verificar user ao iniciar
    LaunchedEffect(Unit) {
        authViewModel.checkCurrentUser()
    }


    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            if (!shouldOpenSettings && !isInitialNavigationProcessed) {
                navController.navigate("home") {
                    popUpTo("auth") { inclusive = true }
                    launchSingleTop = true
                }
            }
            isInitialNavigationProcessed = true
        }
    }

    val displayName = if (authState.name.isNotEmpty()) authState.name else "User"

    //FUNÇÕES DE NAVEGAÇÃO
    val navigateToLogout: () -> Unit = {
        authViewModel.logout()
        navController.navigate("auth") { popUpTo(0) { inclusive = true } }
    }
    val navigateToProfile: () -> Unit = { navController.navigate("profile") { launchSingleTop = true } }
    val navigateToHome: () -> Unit = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
    val navigateToSettings: () -> Unit = { navController.navigate("settings") { launchSingleTop = true } }

    val handleMenuNavigation: (String) -> Unit = { route ->
        when (route) {
            "Logout" -> navigateToLogout()
            "Profile" -> navigateToProfile()
            "Definitions" -> navigateToSettings()
            "Home" -> navigateToHome()
            "Favorites" -> navController.navigate("favorite_pets")
            "Adoption" -> navController.navigate("adoption")
            "AdoptionStatus" -> navController.navigate("adoption_status")
            "Health" -> navController.navigate("health_animal")
            "Chat" -> navController.navigate("chat_animal")
            else -> navController.navigate(route.lowercase())
        }
    }

    NavHost(
        navController = navController,
        startDestination = initialRoute
    ) {
        //AUTENTICAÇÃO
        composable("auth") {
            AuthenticationScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { },
                onBack = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = { },
                onNavigateToLogin = { navController.navigate("login") },
                onBack = { navController.popBackStack() }
            )
        }

        //ECRÃ PRINCIPAL
        composable("home") {
            HomeScreen(
                viewModel = authViewModel,
                userName = displayName,
                onLogout = navigateToLogout,
                onNavigateToProfile = navigateToProfile,
                onNavigateToSettings = navigateToSettings,
                onNavigateToCalendar = { navController.navigate("calendar") },
                onNavigateToHealth = { petId ->
                    if (petId != null) navController.navigate("health/$petId")
                    else navController.navigate("health_animal")
                },
                onNavigateToChatAnimal = { navController.navigate("chat_animal") },
                onNavigateToNotifications = { navController.navigate("notifications") },
                onNavigateFromMenu = handleMenuNavigation
            )
        }
        //NOTIFICAÇÕES
        composable("notifications") {
            NotificationsScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        //SAÚDE E CONSULTAS
        composable(
            route = "health/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            HealthScreen(
                viewModel = authViewModel,
                petId = petId,
                onBack = { navController.popBackStack() },
                onNavigateToProfile = navigateToProfile,
                onNavigateFromMenu = handleMenuNavigation,
                onNavigateToHistory = { id -> navController.navigate("vaccine_history/$id") },
                onNavigateToAppointment = { id -> navController.navigate("select_clinic/$id") },
                onNavigateToStatus = { id -> navController.navigate("appointments_status/$id") },
                onNavigateToPastAppointments = { id -> navController.navigate("appointments_history/$id") },
                onNavigateToAddVaccine = { id -> navController.navigate("add_vaccine/$id") }
            )
        }


        composable(
            route = "add_vaccine/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            AddVaccineScreen(viewModel = authViewModel, petId = petId, onBack = { navController.popBackStack() })
        }

        composable(
            route = "vaccine_history/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            VaccinationHistoryScreen(viewModel = authViewModel, petId = petId, onBack = { navController.popBackStack() })
        }

        composable(
            route = "select_clinic/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            SelectClinicAppointmentScreen(authViewModel = authViewModel, petId = petId, onBack = { navController.popBackStack() }, onNavigateToHome = navigateToHome, onNavigateFromMenu = handleMenuNavigation, onNavigateToForm = { clinicId, pId -> navController.navigate("make_appointment/$clinicId/$pId") })
        }

        composable(
            route = "make_appointment/{clinicId}/{petId}",
            arguments = listOf(navArgument("clinicId") { type = NavType.StringType }, navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clinicId = backStackEntry.arguments?.getString("clinicId") ?: ""
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            MakeAppointmentScreen(viewModel = authViewModel, clinicId = clinicId, petId = petId, onBack = { navController.popBackStack() }, onNavigateToHealth = { navController.navigate("health/$petId") { popUpTo("health/$petId") { inclusive = true } } }, onNavigateToProfile = navigateToProfile, onNavigateFromMenu = handleMenuNavigation)
        }

        composable(
            route = "appointments_status/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            AppointmentStatusScreen(viewModel = authViewModel, petId = petId, onBack = { navController.popBackStack() })
        }

        composable(
            route = "appointments_history/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            AppointmentHistoryScreen(viewModel = authViewModel, petId = petId, onBack = { navController.popBackStack() })
        }

        composable("adoption") {
            AdoptionScreen(authViewModel = authViewModel, onBack = { navController.popBackStack() }, onNavigateToProfile = navigateToProfile, onNavigateFromMenu = handleMenuNavigation, onNavigateToForm = { petId -> navController.navigate("adoption_form/$petId") })
        }

        composable("favorite_pets") {
            FavoritePetsScreen(authViewModel = authViewModel, onBack = { navController.popBackStack() }, onNavigateToHome = navigateToHome, onNavigateToForm = { petId -> navController.navigate("adoption_form/$petId") }, onNavigateFromMenu = handleMenuNavigation)
        }

        composable(
            route = "adoption_form/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            AdoptionFormScreen(petId = petId, authViewModel = authViewModel, onBack = { navController.popBackStack() }, onSubmit = { navigateToHome() })
        }

        composable("chat_animal") {
            ChatAnimalScreen(authViewModel = authViewModel, onBack = { navController.popBackStack() }, onNavigateToHome = navigateToHome, onNavigateToChatVet = { petId -> navController.navigate("chat_vet/$petId") }, onNavigateFromMenu = handleMenuNavigation)
        }


        composable(
            route = "edit_pet/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId")
            EditPetScreen(
                authViewModel = authViewModel,
                petId = petId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "chat_vet/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            ChatVetScreen(authViewModel = authViewModel, petId = petId, onBack = { navController.popBackStack() }, onNavigateToHome = navigateToHome, onNavigateFromMenu = handleMenuNavigation, onNavigateToChat = { vetId, _, pId -> navController.navigate("chat_individual/$vetId/$pId") })
        }

        @Suppress("DEPRECATION")
        composable(
            route = "chat_individual/{vetId}/{petId}",
            arguments = listOf(navArgument("vetId") { type = NavType.StringType }, navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val vetId = backStackEntry.arguments?.getString("vetId") ?: ""
            val petId = backStackEntry.arguments?.getString("petId") ?: ""
            ChatScreen(authViewModel = authViewModel, vetId = vetId, petId = petId, onBack = { navController.popBackStack() }, onNavigateToHome = navigateToHome)
        }

        composable("profile") {
            ProfileScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onNavigateFromMenu = handleMenuNavigation,
                onNavigateToAddPet = { navController.navigate("add_pet") },
                onNavigateToEditPet = { id -> navController.navigate("edit_pet/$id") }
            )
        }
        composable("add_pet") {
            AddPetScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("calendar") {
            CalendarScreen(onBack = { navController.popBackStack() }, onLogout = navigateToLogout, onNavigateToProfile = navigateToProfile, onNavigateToSettings = navigateToSettings)
        }

        composable("settings") {
            SettingsScreen(authViewModel = authViewModel,settingsViewModel = settingsViewModel, onBack = { navController.popBackStack() }, onLogout = navigateToLogout, onNavigateToProfile = navigateToProfile, onNavigateToSettings = navigateToSettings, onNavigateToHome = navigateToHome)
        }


        composable("adoption_status") {
            AdoptionStatusScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}