package com.example.playpals

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.playpals.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "User"
                        val initials = name.split(" ").joinToString("") { it.first().toString() }.uppercase()
                        binding.btnProfile.text = initials

                        binding.btnProfile.setOnClickListener {
                            AlertDialog.Builder(this)
                                .setTitle("User Profile")
                                .setMessage(
                                    "Name: ${document.getString("name")}\n" +
                                            "Age: ${document.getString("age")}\n" +
                                            "Gender: ${document.getString("gender")}\n" +
                                            "Contact: ${document.getString("contact")}\n" +
                                            "Hobby: ${document.getString("hobby")}\n" +
                                            "Email: ${document.getString("email")}"
                                )
                                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
