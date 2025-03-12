package com.example.playpals

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var notificationsLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        notificationsLayout = findViewById(R.id.notificationsLayout)

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("notifications")
            .whereEqualTo("to", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                notificationsLayout.removeAllViews()

                if (querySnapshot.isEmpty) {
                    val noNotifications = TextView(this)
                    noNotifications.text = "No new notifications"
                    noNotifications.textSize = 16f
                    noNotifications.setPadding(16, 16, 16, 16)
                    notificationsLayout.addView(noNotifications)
                    return@addOnSuccessListener
                }

                for (document in querySnapshot.documents) {
                    val senderId = document.getString("from") ?: continue

                    db.collection("users").document(senderId).get()
                        .addOnSuccessListener { senderDoc ->
                            val senderName = senderDoc.getString("name") ?: "Unknown User"
                            val message = "$senderName has pinged you to join a game!"

                            val notificationView = LinearLayout(this)
                            notificationView.orientation = LinearLayout.HORIZONTAL
                            notificationView.setPadding(16, 16, 16, 16)

                            val textView = TextView(this)
                            textView.text = message
                            textView.textSize = 16f
                            textView.setPadding(16, 16, 16, 16)

                            val dismissButton = Button(this)
                            dismissButton.text = "Dismiss"
                            dismissButton.setOnClickListener {
                                document.reference.delete()
                                notificationsLayout.removeView(notificationView)
                            }

                            notificationView.addView(textView)
                            notificationView.addView(dismissButton)
                            notificationsLayout.addView(notificationView)
                        }
                }
            }
    }
}
