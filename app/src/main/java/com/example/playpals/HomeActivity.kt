package com.example.playpals

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.playpals.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.QuerySnapshot
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

        binding.btnFindPlayers.setOnClickListener {
            showGameFormDialog()
        }
    }

    private fun showGameFormDialog() {
        val formView = layoutInflater.inflate(R.layout.dialog_game_form, null)
        val gameTypeSpinner = formView.findViewById<Spinner>(R.id.spinnerGameType)
        val manualLocationInput = formView.findViewById<EditText>(R.id.etManualLocation)
        val useCurrentLocation = formView.findViewById<Button>(R.id.btnUseCurrentLocation)


        // Manually setting game options
        val gameOptions = arrayOf("Football", "Cricket", "Badminton", "Basketball", "Tennis")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, gameOptions)
        gameTypeSpinner.adapter = adapter

        var userLocation: GeoPoint? = null

        useCurrentLocation.setOnClickListener {
            getCurrentLocation { location ->
                userLocation = location
                Toast.makeText(this, "Location Set Automatically", Toast.LENGTH_SHORT).show()
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Find Players Near You")
            .setView(formView)
            .setPositiveButton("Submit") { _, _ ->
                val selectedGame = gameTypeSpinner.selectedItem.toString()
                val manualLocation = manualLocationInput.text.toString()

                if (userLocation == null && manualLocation.isEmpty()) {
                    Toast.makeText(this, "Please provide a location", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                saveUserGameEntry(selectedGame, userLocation, manualLocation)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(callback: (GeoPoint) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                callback(GeoPoint(location.latitude, location.longitude))
            }
        }
    }

    private fun saveUserGameEntry(game: String, location: GeoPoint?, manualLocation: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) return@addOnSuccessListener

                val name = document.getString("name") ?: "Unknown"
                val contact = document.getString("contact") ?: "N/A"

                val gameEntry = hashMapOf(
                    "userId" to userId,
                    "name" to name,
                    "contact" to contact,
                    "game" to game,
                    "location" to (location ?: GeoPoint(0.0, 0.0)),
                    "manualLocation" to manualLocation
                )

                db.collection("game_entries").document(userId).set(gameEntry)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Game Entry Submitted", Toast.LENGTH_SHORT).show()
                        findNearbyPlayers(game, location)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to submit entry", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun findNearbyPlayers(selectedGame: String, userLocation: GeoPoint?) {
        if (userLocation == null) return

        db.collection("game_entries")
            .whereEqualTo("game", selectedGame)
            .get()
            .addOnSuccessListener { querySnapshot: QuerySnapshot ->
                val nearbyPlayers = mutableListOf<String>()

                for (document in querySnapshot.documents) {
                    val otherUserLocation = document.getGeoPoint("location")
                    if (otherUserLocation != null) {
                        val distance = calculateDistance(
                            userLocation.latitude, userLocation.longitude,
                            otherUserLocation.latitude, otherUserLocation.longitude
                        )

                        if (distance <= 3) { // Within 3 km radius
                            val playerName = document.getString("name") ?: "Unknown"
                            val playerContact = document.getString("contact") ?: "N/A"
                            nearbyPlayers.add("$playerName - $playerContact")
                        }
                    }
                }

                showNearbyPlayersDialog(nearbyPlayers)
            }
    }

    private fun showNearbyPlayersDialog(players: List<String>) {
        val message = if (players.isNotEmpty()) players.joinToString("\n") else "No players found nearby."

        AlertDialog.Builder(this)
            .setTitle("Nearby Players")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return (results[0] / 1000).toDouble() // Convert to km
    }
}
