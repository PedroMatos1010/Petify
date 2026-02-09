package com.example.loginfirebaseapp

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID
import java.time.LocalDate
import java.time.LocalTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.get
import com.google.firebase.messaging.FirebaseMessaging

//ESTADO DA UI
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val phone: String = "",
    val age: String = "",
    val citizenCard: String = "",
    val role: String = "client",
    val profileImageUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val favoritePets: List<String> = emptyList(),
    val settings: Map<String, @JvmSuppressWildcards Any> = mapOf(
        "language" to "pt",
        "theme" to 1
    )
)

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // CONTROLO DE LISTENERS
    private var nextAppointmentListener: ListenerRegistration? = null
    private var petAppointmentsListener: ListenerRegistration? = null
    private var occupiedSlotsListener: ListenerRegistration? = null
    private var vaccinesListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private var notificationsListListener: ListenerRegistration? = null

    //ESTADOS DE EVENTOS
    var eventsOfTheDay = mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set
    var allEvents = mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set
    var nextUpcomingEvent = mutableStateOf<Map<String, Any>?>(null)
        private set

    // ESTADOS DE UTILIZADOR
    private var _authUiState = mutableStateOf(AuthUiState())
    val authUiState: State<AuthUiState> = _authUiState

    var loginState = mutableStateOf(AuthUiState())
        private set
    var registerState = mutableStateOf(AuthUiState())
        private set

    //ESTADO DO TEMA
    var selectedThemeId = mutableStateOf(1)
        private set

    //ESTADOS DE PETS
    var userPets = mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set
    //not
    private var unreadChatListener: ListenerRegistration? = null
    //ESTADO DE CONSULTAS (APPOINTMENTS)
    private val _nextAppointment = mutableStateOf<Map<String, Any>?>(null)
    val nextAppointment: State<Map<String, Any>?> = _nextAppointment



    private val _userAppointments = mutableStateOf<List<Map<String, Any>>>(emptyList())
    val userAppointments: State<List<Map<String, Any>>> = _userAppointments

    private val _occupiedSlots = mutableStateOf<List<String>>(emptyList())
    val occupiedSlots: State<List<String>> = _occupiedSlots

    //ESTADO DE VACINAS / HISTÓRICO
    private val _petVaccines = mutableStateOf<List<Map<String, Any>>>(emptyList())
    val petVaccines: State<List<Map<String, Any>>> = _petVaccines

    //OUTROS ESTADOS
    var clinics = mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set
    var chatMessages = mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set
    var unreadChatCount = mutableStateOf(0)
        private set

    //ESTADO DE NOTIFICAÇÕES
    var unreadNotificationsCount = mutableStateOf(0)
        private set
    var notificationsList = mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set

    init {
        checkCurrentUser()
        fetchClinics()
        fetchUserSettings()
    }
    fun checkCurrentUser() {
        auth.currentUser?.let {
            fetchUserData(it.uid)
            listenForNotifications()
            listenForUnreadChatMessages()

        }
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    //ATUALIZAÇÃO DE INPUTS
    fun onLoginEmailChange(newValue: String) { loginState.value = loginState.value.copy(email = newValue) }
    fun onLoginPasswordChange(newValue: String) { loginState.value = loginState.value.copy(password = newValue) }
    fun onRegisterNameChange(newValue: String) { registerState.value = registerState.value.copy(name = newValue) }
    fun onRegisterEmailChange(newValue: String) { registerState.value = registerState.value.copy(email = newValue) }
    fun onRegisterPasswordChange(newValue: String) { registerState.value = registerState.value.copy(password = newValue) }
    fun onRegisterPhoneChange(newValue: String) { registerState.value = registerState.value.copy(phone = newValue) }
    fun onRegisterCitizenCardChange(newValue: String) { registerState.value = registerState.value.copy(citizenCard = newValue) }
    fun onRegisterAgeChange(newValue: String) { registerState.value = registerState.value.copy(age = newValue) }
    fun onProfileNameChange(newName: String) { _authUiState.value = _authUiState.value.copy(name = newName) }

    //AUTENTICAÇÃO
    fun login() {
        val state = loginState.value
        if (state.email.isEmpty() || state.password.isEmpty()) return
        loginState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(state.email, state.password).await()
                result.user?.let {
                    fetchUserData(it.uid)
                    listenForNotifications()
                    loginState.value = AuthUiState(isLoggedIn = true)
                }
            } catch (e: Exception) {
                loginState.value = loginState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
            }
        }
    }

    fun register() {
        val state = registerState.value
        if (state.email.isEmpty() || state.password.isEmpty()) return
        registerState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(state.email, state.password).await()
                result.user?.let { firebaseUser ->
                    val userData = hashMapOf(
                        "uid" to firebaseUser.uid,
                        "name" to state.name,
                        "email" to state.email,
                        "phone" to state.phone,
                        "age" to state.age,
                        "citizenCard" to state.citizenCard,
                        "role" to "client",
                        "profileImageUrl" to "",
                        "favoritePets" to emptyList<String>(),
                        "settings" to mapOf("language" to "pt", "theme" to 1)
                    )
                    db.collection("users").document(firebaseUser.uid).set(userData).await()
                    fetchUserData(firebaseUser.uid)
                    listenForNotifications()
                    registerState.value = registerState.value.copy(isLoggedIn = true, isLoading = false)
                }
            } catch (e: Exception) {
                registerState.value = registerState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
            }
        }
    }

    fun logout() {
        auth.signOut()
        nextAppointmentListener?.remove()
        petAppointmentsListener?.remove()
        occupiedSlotsListener?.remove()
        vaccinesListener?.remove()
        chatListener?.remove()
        notificationsListener?.remove()
        notificationsListListener?.remove()
        _authUiState.value = AuthUiState(isLoggedIn = false)
        loginState.value = AuthUiState()
        registerState.value = AuthUiState()
        userPets.value = emptyList()
        _userAppointments.value = emptyList()
        _nextAppointment.value = null
        _petVaccines.value = emptyList()
        eventsOfTheDay.value = emptyList()
        allEvents.value = emptyList()
        nextUpcomingEvent.value = null
        _occupiedSlots.value = emptyList()
        unreadNotificationsCount.value = 0
        notificationsList.value = emptyList()
        unreadChatListener?.remove()
        unreadChatCount.value = 0
    }
    fun updateFCMToken() {
        val userId = auth.currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result

                // Guarda o token
                db.collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        println("FCM Token atualizado com sucesso!")
                    }
            }
        }
    }
    private fun fetchUserData(uid: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val userData = doc.data
                    val rawSettings = doc.get("settings") as? Map<String, Any>


                    val themeFromDb = (rawSettings?.get("theme") as? Long)?.toInt() ?: 1
                    selectedThemeId.value = themeFromDb

                    _authUiState.value = _authUiState.value.copy(
                        name = userData?.get("name") as? String ?: "",
                        email = userData?.get("email") as? String ?: "",
                        phone = userData?.get("phone") as? String ?: "",
                        age = userData?.get("age") as? String ?: "",
                        citizenCard = userData?.get("citizenCard") as? String ?: "",
                        role = userData?.get("role") as? String ?: "client",
                        profileImageUrl = userData?.get("profileImageUrl") as? String ?: "",
                        favoritePets = doc.get("favoritePets") as? List<String> ?: emptyList(),
                        isLoggedIn = true,
                        settings = rawSettings ?: _authUiState.value.settings,
                        isLoading = false
                    )

                    updateFCMToken()
                    fetchUserPets()
                    fetchNextUpcomingEvent()
                    fetchAllEvents()
                    fetchAllUserAppointments()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Erro fetchUserData: ${e.message}")
            }
        }
    }
    fun markMessagesAsRead(chatId: String, currentUserId: String) {
        val db = FirebaseFirestore.getInstance()


        db.collection("messages")
            .whereEqualTo("chatId", chatId)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update("isRead", true)
                }
            }
    }
    //GESTÃO DE CONSULTAS

    fun fetchAllUserAppointments() {
        val userId = auth.currentUser?.uid ?: return
        petAppointmentsListener?.remove()

        petAppointmentsListener = db.collection("appointments")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Erro fetchAllUserAppointments: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _userAppointments.value = snapshot.documents.map { doc ->
                        doc.data?.plus("id" to doc.id) ?: emptyMap()
                    }
                }
            }
    }

    fun listenForUnreadChatMessages() {
        val currentUserId = auth.currentUser?.uid ?: return
        unreadChatListener?.remove()

        unreadChatListener = db.collectionGroup("messages")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Erro no badge do chat: ${error.message}")
                    return@addSnapshotListener
                }
                unreadChatCount.value = snapshot?.size() ?: 0
            }
    }

    fun makeAppointment(
        clinicId: String,
        petId: String,
        reason: String,
        urgency: String,
        price: Int,
        date: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val normalizedDate = date.trim()

                // verificacao se tem consultas
                val petBusy = db.collection("appointments")
                    .whereEqualTo("petId", petId)
                    .whereEqualTo("date", normalizedDate)
                    .whereIn("status", listOf("pendente", "confirmado", "confirmada"))
                    .get()
                    .await()

                if (!petBusy.isEmpty) {
                    onError("Este pet já tem uma consulta marcada para este horário!")
                    return@launch // Interrompe o processo aqui
                }

                //Verificar limite de veterinários na clínica
                val clinicDoc = db.collection("clinics").document(clinicId).get().await()
                val totalVets = (clinicDoc.get("staffCount") as? Number)?.toInt() ?: 1

                val existingAppointments = db.collection("appointments")
                    .whereEqualTo("clinicId", clinicId)
                    .whereEqualTo("date", normalizedDate)
                    .whereIn("status", listOf("pendente", "confirmado", "confirmada"))
                    .get()
                    .await()

                val occupiedSlotsCount = existingAppointments.size()

                //Verificação final
                if (occupiedSlotsCount < totalVets) {
                    val appointmentRef = db.collection("appointments").document()
                    val appointmentData = hashMapOf(
                        "id" to appointmentRef.id,
                        "clinicId" to clinicId,
                        "petId" to petId,
                        "userId" to userId,
                        "vetId" to "pending",
                        "reason" to reason,
                        "urgency" to urgency,
                        "price" to price,
                        "date" to normalizedDate,
                        "status" to "pendente",
                        "timestamp" to Timestamp.now()
                    )
                    appointmentRef.set(appointmentData).await()
                    onSuccess()
                } else {
                    onError("Este horário já está preenchido (Limite: $totalVets veterinários).")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Erro: ${e.message}")
                onError("Erro ao processar: ${e.message}")
            }
        }
    }

    fun checkAppointmentLimit(callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("appointments")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "pendente")
            .get()
            .addOnSuccessListener { documents -> callback(documents.size() < 3) }
            .addOnFailureListener { callback(false) }
    }
    fun fetchOccupiedSlots(clinicId: String, dateString: String, petId: String) {
        occupiedSlotsListener?.remove()



        occupiedSlotsListener = db.collection("appointments")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    viewModelScope.launch {
                        // 1. Obter dados da clínica atual para saber o limite de staff
                        val clinicDoc = db.collection("clinics").document(clinicId).get().await()
                        val totalVets = (clinicDoc.get("staffCount") as? Long)?.toInt() ?: 1

                        // 2. Filtrar apenas consultas do dia selecionado e que estejam ativas
                        val allActiveAppointmentsOnDay = snapshot.documents.filter { doc ->
                            val apptDate = doc.getString("date") ?: ""
                            val status = doc.getString("status") ?: ""
                            apptDate.startsWith(dateString) &&
                                    listOf("pendente", "confirmado", "confirmada").contains(status)
                        }

                        // 3. Lógica da Clínica: Contar consultas nesta clínica específica
                        val apptsInThisClinic = allActiveAppointmentsOnDay.filter {
                            it.getString("clinicId") == clinicId
                        }
                        val clinicHourCounts = apptsInThisClinic.groupingBy {
                            it.getString("date")?.split(" ")?.last() ?: ""
                        }.eachCount()

                        // Horas bloqueadas por excesso de lotação na clínica
                        val occupiedByClinic = clinicHourCounts.filter { it.value >= totalVets }.keys

                        //Ver se o petId atual já tem marcação neste dia
                        val occupiedByPet = allActiveAppointmentsOnDay.filter {
                            it.getString("petId") == petId
                        }.map {
                            it.getString("date")?.split(" ")?.last() ?: ""
                        }


                        val combinedOccupied = (occupiedByClinic + occupiedByPet).toMutableList()

                        //Bloquear horas passadas se for hoje
                        val today = java.time.LocalDate.now().toString()
                        if (dateString == today) {
                            val currentTime = java.time.LocalTime.now()
                            val allPossibleSlots = listOf("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00")
                            allPossibleSlots.forEach { slot ->
                                try {
                                    if (java.time.LocalTime.parse(slot).isBefore(currentTime)) {
                                        if (!combinedOccupied.contains(slot)) combinedOccupied.add(slot)
                                    }
                                } catch (e: Exception) { /* Ignora erro de parse */ }
                            }
                        }

                        _occupiedSlots.value = combinedOccupied
                    }
                }
            }
    }
    fun fetchNextAppointment(petId: String) {
        val userId = auth.currentUser?.uid ?: return
        _nextAppointment.value = null
        nextAppointmentListener?.remove()

        nextAppointmentListener = db.collection("appointments")
            .whereEqualTo("userId", userId)
            .whereEqualTo("petId", petId)
            .whereIn("status", listOf("pendente", "confirmado", "confirmada"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Erro fetchNext: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val now = Date()
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                    val nextOne = snapshot.documents
                        .map { it.data?.plus("id" to it.id) ?: emptyMap<String, Any>() }
                        .filter { appt ->
                            val dateStr = appt["date"]?.toString() ?: ""
                            try {
                                val apptDate = sdf.parse(dateStr)
                                apptDate?.after(now) == true
                            } catch (e: Exception) { true }
                        }
                        .minByOrNull { it["date"]?.toString() ?: "9999" }

                    _nextAppointment.value = nextOne
                } else {
                    _nextAppointment.value = null
                }
            }
    }
    fun fetchPetAppointments(petId: String) {
        val userId = auth.currentUser?.uid ?: return
        _userAppointments.value = emptyList()
        petAppointmentsListener?.remove()

        petAppointmentsListener = db.collection("appointments")
            .whereEqualTo("userId", userId)
            .whereEqualTo("petId", petId)
            .whereIn("status", listOf("pendente", "confirmado", "confirmada", "concluída", "concluído", "cancelada"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Erro fetchPetAppointments: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _userAppointments.value = snapshot.documents.map { doc ->
                        doc.data?.plus("id" to doc.id) ?: emptyMap()
                    }.sortedByDescending { it["date"]?.toString() ?: "" }
                }
            }
    }
    fun cancelAppointment(appointmentId: String) {
        viewModelScope.launch {
            try {
                db.collection("appointments").document(appointmentId)
                    .update("status", "cancelada")
                    .await()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Erro ao cancelar consulta: ${e.message}")
            }
        }
    }
    //GESTÃO DE VACINAS
    fun saveVaccineRecord(petId: String, name: String, dateAdministered: String, validUntil: String, batchNumber: String, vetName: String, imageUri: Uri?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {

                val vaccineRef = db.collection("pets")
                    .document(petId)
                    .collection("vaccination_card")
                    .document() // Gera o ID automaticamente aqui

                val generatedId = vaccineRef.id

                //Upload da imagem para o Storage
                var finalImageUrl = ""
                imageUri?.let { uri ->
                    val storageRef = storage.reference.child("vaccine_proofs/${generatedId}.jpg")
                    storageRef.putFile(uri).await()
                    finalImageUrl = storageRef.downloadUrl.await().toString()
                }


                val vaccineData = hashMapOf(
                    "vcId" to generatedId,
                    "name" to name,
                    "dateAdministered" to dateAdministered,
                    "validUntil" to validUntil,
                    "batchNumber" to batchNumber,
                    "vetName" to vetName,
                    "imageUrl" to finalImageUrl,
                    "petsId" to petId,
                    "timestamp" to Timestamp.now()
                )

                //Guardar
                vaccineRef.set(vaccineData).await()

                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Erro ao salvar vacina: ${e.message}")
            }
        }
    }
    fun fetchVaccines(petId: String) {
        vaccinesListener?.remove()
        vaccinesListener = db.collection("pets").document(petId)
            .collection("vaccination_card")
            .orderBy("dateAdministered", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    _petVaccines.value = snapshot.documents.map { it.data?.plus("id" to it.id) ?: emptyMap() }
                } else if (error != null) {
                    Log.e("AuthViewModel", "Erro ao buscar vacinas: ${error.message}")
                }
            }
    }
    fun updatePet(
        petId: String,
        name: String,
        breed: String,
        age: String,
        weight: String,
        existingImages: List<String>,
        newImageUris: List<Uri>,
        onComplete: () -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val finalImagesList = existingImages.toMutableList()

                newImageUris.forEachIndexed { index, uri ->
                    val fileName = "image_${System.currentTimeMillis()}_$index.jpg"
                    val storageRef = storage.reference.child("pet_images/$petId/$fileName")

                    storageRef.putFile(uri).await()
                    val url = storageRef.downloadUrl.await().toString()
                    finalImagesList.add(url)
                }

                val data = mutableMapOf<String, Any>(
                    "name" to name.lowercase(),
                    "breed" to breed,
                    "age" to age,
                    "weight" to weight,
                    "images" to finalImagesList,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )

                db.collection("pets").document(petId)
                    .update(data)
                    .await()

                fetchUserPets()
                onComplete()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Erro ao atualizar pet: ${e.message}")
                onComplete()
            }
        }
    }

    fun toggleFavorite(petId: String) {
        val userId = auth.currentUser?.uid ?: return
        val currentFavorites = _authUiState.value.favoritePets.toMutableList()
        val isFavorite = currentFavorites.contains(petId)
        val userRef = db.collection("users").document(userId)
        if (isFavorite) currentFavorites.remove(petId) else currentFavorites.add(petId)
        _authUiState.value = _authUiState.value.copy(favoritePets = currentFavorites)
        viewModelScope.launch {
            try {
                if (isFavorite) userRef.update("favoritePets", FieldValue.arrayRemove(petId)).await()
                else userRef.update("favoritePets", FieldValue.arrayUnion(petId)).await()
            } catch (e: Exception) { fetchUserData(userId) }
        }
    }
    fun fetchClinics() {
        db.collection("clinics").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                clinics.value = snapshot.documents.map { doc ->
                    doc.data?.plus("id" to doc.id) ?: emptyMap()
                }
            }
        }
    }
    fun listenForMessages(vetId: String, petId: String) {
        val userId = auth.currentUser?.uid ?: return
        val chatId = "${userId}_${vetId}_$petId"

        chatListener?.remove()

        chatListener = db.collection("chats").document(chatId).collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.let { querySnapshot ->
                    val list = querySnapshot.documents.mapNotNull { it.data }


                    chatMessages.value = list.sortedBy { m ->
                        val ts = m["timestamp"] as? Timestamp
                        ts?.seconds ?: 0L
                    }
                }
            }
    }
    fun sendMessage(clinicId: String, text: String, petId: String) {
        val userId = auth.currentUser?.uid ?: return
        val chatId = "${userId}_${clinicId}_$petId"
        val timestamp = Timestamp.now()

        val messageData = hashMapOf(
            "chatId" to chatId,
            "senderId" to userId,
            "receiverId" to clinicId,
            "text" to text,
            "timestamp" to timestamp,
            "petId" to petId
        )

        val chatData = hashMapOf(
            "chatId" to chatId,
            "userId" to userId,
            "clinicId" to clinicId,
            "lastMessage" to text,
            "updatedAt" to timestamp,
            "userName" to _authUiState.value.name,
            "petId" to petId
        )

        viewModelScope.launch {
            // Grava a mensagem
            db.collection("chats").document(chatId).collection("messages").add(messageData)

            db.collection("chats").document(chatId).set(chatData, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    fun saveEvent(title: String, description: String, date: String) {
        val userId = auth.currentUser?.uid ?: return
        val eventRef = db.collection("events").document()

        val eventData = hashMapOf(
            "id" to eventRef.id,
            "title" to title,
            "description" to description,
            "date" to date,
            "userId" to userId,
            "createdAt" to Timestamp.now()
        )

        eventRef.set(eventData).addOnSuccessListener {
            fetchEventsForDate(date)
            fetchAllEvents()
        }
    }
    fun fetchEventsForDate(date: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("events").whereEqualTo("userId", userId).whereEqualTo("date", date).get()
            .addOnSuccessListener { snapshot ->
                eventsOfTheDay.value = snapshot.documents.map { it.data?.plus("id" to it.id) ?: emptyMap() }
            }
    }
    val adoptionRequests = mutableStateOf<List<Map<String, Any>>>(emptyList())

    fun fetchAdoptionRequests() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("adoption_requests")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val docs = snapshot?.documents?.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data.toMap()
                } ?: emptyList()

                adoptionRequests.value = docs
            }
    }
    val adoptionsAsEvents: List<Map<String, Any>>
        get() = adoptionRequests.value
            .filter { it["interviewDate"]?.toString()?.isNotEmpty() == true }
            .map { req ->
                val fullDateTime = req["interviewDate"].toString()
                val parts = fullDateTime.split(" ")

                mapOf(
                    "id" to (req["id"] ?: ""),
                    "clinicId" to (req["adoptionCenterId"] ?: ""),
                    "title" to "Entrevista: ${req["petName"] ?: "Pet"}",
                    "description" to "Entrevista de Adoção",
                    "date" to (parts.getOrNull(0) ?: ""),
                    "time" to (parts.getOrNull(1) ?: ""),
                    "urgency" to "entrevista",
                    "isAppointment" to true
                )
            }
    fun fetchAllEvents() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("events").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { it.data?.plus("id" to it.id) ?: emptyMap() }
                    allEvents.value = list
                    calculateNextEvent(list)
                }
            }
    }
    private fun calculateNextEvent(events: List<Map<String, Any>>) {
        val today = LocalDate.now().toString()
        nextUpcomingEvent.value = events.filter { (it["date"]?.toString() ?: "") >= today }
            .minByOrNull { it["date"]?.toString() ?: "9999-99-99" }
    }
    fun fetchNextUpcomingEvent() {
        val userId = auth.currentUser?.uid ?: return
        val todayDate = LocalDate.now().toString() // "yyyy-MM-dd"
        val nowDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        //Buscar Eventos Manuais
        db.collection("events")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { eventSnapshot ->
                val manuals = eventSnapshot.map { doc ->
                    doc.data + mapOf("id" to doc.id, "type" to "event")
                }.filter { it["date"].toString() >= todayDate }

                //Buscar Consultas Confirmadas
                db.collection("appointments")
                    .whereEqualTo("userId", userId)
                    .whereIn("status", listOf("confirmado", "confirmada"))
                    .get()
                    .addOnSuccessListener { apptSnapshot ->
                        val appts = apptSnapshot.map { doc ->
                            val d = doc.data
                            val fullDate = d["date"]?.toString() ?: ""
                            mapOf(
                                "id" to doc.id,
                                "title" to "Consulta: ${d["reason"]}",
                                "date" to fullDate,
                                "type" to "appointment",
                                "description" to "Urgência: ${d["urgency"]}"
                            )
                        }.filter { it["date"].toString() >= nowDateTime }

                        //Buscar Entrevistas de Adoção
                        db.collection("adoption_requests")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener { adoptionSnapshot ->
                                val adoptions = adoptionSnapshot.mapNotNull { doc ->
                                    val d = doc.data
                                    val interviewDate = d["interviewDate"]?.toString() ?: ""

                                    if (interviewDate.isEmpty()) return@mapNotNull null

                                    mapOf(
                                        "id" to doc.id,
                                        "title" to "Entrevista: ${d["petName"] ?: "Pet"}",
                                        "date" to interviewDate,
                                        "type" to "adoption",
                                        "description" to "Entrevista de Adoção"
                                    )
                                }.filter { it["date"].toString() >= nowDateTime }


                                val allUnified = (manuals + appts + adoptions)
                                    .sortedBy { it["date"].toString() }

                                nextUpcomingEvent.value = allUnified.firstOrNull()
                            }
                    }
            }
    }
    fun deleteEvent(eventId: String, dateToRefresh: String) {
        db.collection("events").document(eventId).delete().addOnSuccessListener {
            fetchEventsForDate(dateToRefresh)
            fetchAllEvents()
        }
    }
    fun savePet(
        name: String,
        species: String,
        breed: String,
        age: String,
        weight: String,
        imageUris: List<Uri>,
        onSuccess: () -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val petRef = db.collection("pets").document()
                val petId = petRef.id
                val imageUrls = mutableListOf<String>()

                //Uploadimagem
                imageUris.forEachIndexed { index, uri ->
                    val storageRef = storage.reference.child("pet_images/$petId/image_$index.jpg")
                    storageRef.putFile(uri).await()
                    val url = storageRef.downloadUrl.await().toString()
                    imageUrls.add(url)
                }

                //criar lista de imagens
                val petData = hashMapOf(
                    "id" to petId,
                    "ownerId" to userId,
                    "name" to name.lowercase(),
                    "species" to species,
                    "breed" to breed,
                    "age" to age,
                    "weight" to weight,
                    "images" to imageUrls,
                    "status" to "owned",
                    "active" to true,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                petRef.set(petData).await()
                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Erro ao salvar pet com múltiplas fotos: ${e.message}")
            }
        }
    }
    fun fetchUserPets() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    userPets.value = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data?.toMutableMap() ?: return@mapNotNull null

                        if (data["active"] == false) return@mapNotNull null

                        // Tenta obter a primeira imagem da lista
                        val imagesList = data["images"] as? List<String>
                        val firstImage = imagesList?.firstOrNull()


                        val finalImage = firstImage ?: data["imageUrl"] ?: ""

                        data["id"] = doc.id
                        data["displayImage"] = finalImage

                        data.toMap()
                    }
                }
            }
    }

    fun deletePet(petId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {

                db.collection("pets").document(petId)
                    .update("active", false)
                    .await()


                onComplete()
                println("SUCESSO: Pet $petId desativado logicamente.")
            } catch (e: Exception) {
                println("ERRO ao desativar pet: ${e.message}")
                onComplete()
            }
        }
    }

    fun fetchUserSettings() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val rawSettings = document.get("settings") as? Map<String, Any>


                    val themeId = (rawSettings?.get("theme") as? Long)?.toInt() ?: 1
                    selectedThemeId.value = themeId

                    Log.d("ThemeCheck", "Tema carregado após recriação: $themeId")
                }
            }
    }
    fun uploadProfileImage(imageUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val storageRef = storage.reference.child("profile_images/$uid.jpg")
                storageRef.putFile(imageUri).await()
                val url = storageRef.downloadUrl.await().toString()
                db.collection("users").document(uid).update("profileImageUrl", url).await()
                _authUiState.value = _authUiState.value.copy(profileImageUrl = url)
            } catch (e: Exception) { }
        }
    }
    fun updateProfileData() {
        val uid = auth.currentUser?.uid ?: return
        val state = _authUiState.value
        viewModelScope.launch {
            try {
                db.collection("users").document(uid).update("name", state.name, "phone", state.phone, "age", state.age, "citizenCard", state.citizenCard).await()
            } catch (e: Exception) { }
        }
    }
    fun updateUserSettings(language: String, themeId: Int, notificationsEnabled: Boolean) {
        val userId = auth.currentUser?.uid ?: return


        val settingsMap = mapOf(
            "language" to language,
            "theme" to themeId,
            "notificationEnable" to notificationsEnabled
        )

        db.collection("users").document(userId)
            .update("settings", settingsMap)
            .addOnSuccessListener {
                // Atualiza o estado local IMEDIATAMENTE para a UI mudar
                selectedThemeId.value = themeId
            }
    }
    //ESCUTAR NOTIFICAÇÕES EM TEMPO REAL
    fun listenForNotifications() {
        val userId = auth.currentUser?.uid ?: return
        notificationsListener?.remove()

        notificationsListener = db.collection("users").document(userId)
            .collection("notifications")
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Erro listenForNotifications: ${error.message}")
                    return@addSnapshotListener
                }
                unreadNotificationsCount.value = snapshot?.size() ?: 0
            }
    }
    //GESTÃO DA LISTA DE NOTIFICAÇÕES
    fun fetchNotifications() {
        val userId = auth.currentUser?.uid ?: return
        notificationsListListener?.remove()

        notificationsListListener = db.collection("users").document(userId)
            .collection("notifications")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Erro fetchNotifications: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    notificationsList.value = snapshot.documents.map {
                        it.data?.plus("id" to it.id) ?: emptyMap()
                    }
                }
            }
    }
    fun markNotificationAsRead(notificationId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("notifications").document(notificationId)
            .update("read", true)
            .addOnFailureListener { e -> Log.e("AuthViewModel", "Erro markRead: ${e.message}") }
    }
    fun deleteNotification(notificationId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("notifications").document(notificationId)
            .delete()
    }
    override fun onCleared() {
        super.onCleared()
        nextAppointmentListener?.remove()
        petAppointmentsListener?.remove()
        occupiedSlotsListener?.remove()
        vaccinesListener?.remove()
        chatListener?.remove()
        notificationsListener?.remove()
        notificationsListListener?.remove()
        unreadChatListener?.remove()
    }
    fun deleteAllNotifications() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val snapshots = db.collection("users").document(userId).collection("notifications").get().await()
            for (document in snapshots.documents) {
                document.reference.delete()
            }
        }
    }
    //localizacao
    fun openClinicInMaps(context: android.content.Context, clinicId: String) {
        val clinic = clinics.value.find { it["id"] == clinicId }
        val coords = clinic?.get("coordinates") as? Map<String, Any> // Mudado para Any
        val lat = coords?.get("lat")?.toString()
        val lng = coords?.get("lng")?.toString()
        if (!lat.isNullOrEmpty() && !lng.isNullOrEmpty()) {
            val gmmIntentUri = android.net.Uri.parse("google.navigation:q=$lat,$lng")
            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            context.startActivity(mapIntent)
        } else {
            Log.e("AuthViewModel", "Coordenadas não encontradas para a clínica: $clinicId")
        }
    }
    fun updateUserLanguage(lang: String) {
        val userId = auth.currentUser?.uid ?: return
        // Atualiza apenas o campo da língua dentro do objeto settings no Firestore
        db.collection("users").document(userId)
            .update("settings.language", lang)
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Erro ao atualizar língua: ${e.message}")
            }
    }
}