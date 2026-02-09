package com.example.loginfirebaseapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()


    var unreadCount by mutableStateOf(0)
        private set


    fun observeUnreadMessages(userId: String) {
        db.collectionGroup("messages")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                // Atualiza o contador automaticamente
                unreadCount = snapshot?.size() ?: 0
            }
    }
}