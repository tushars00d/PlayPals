package com.example.playpals

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.playpals.databinding.ActivityOnboardingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val hobbiesArray = arrayOf("Gym", "Cricket", "Badminton", "Football", "Music", "Art", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, hobbiesArray)
        binding.spinnerHobbies.setAdapter(adapter)

        binding.btnSubmit.setOnClickListener {
            val email = intent.getStringExtra("email")!!
            val password = intent.getStringExtra("password")!!
            val name = binding.etName.text.toString().trim()
            val age = binding.etAge.text.toString().trim()
            val gender = if (binding.rbMale.isChecked) "Male" else "Female"
            val contact = binding.etContact.text.toString().trim()
            val hobby = if (binding.spinnerHobbies.text.toString() == "Other") {
                binding.etOtherHobby.text.toString().trim()
            } else {
                binding.spinnerHobbies.text.toString()
            }

            if (name.isNotEmpty() && age.isNotEmpty() && contact.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser!!.uid
                            val userMap = hashMapOf(
                                "name" to name,
                                "age" to age,
                                "gender" to gender,
                                "contact" to contact,
                                "hobby" to hobby,
                                "email" to email
                            )

                            db.collection("users").document(userId).set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Profile Created!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, HomeActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Signup Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
