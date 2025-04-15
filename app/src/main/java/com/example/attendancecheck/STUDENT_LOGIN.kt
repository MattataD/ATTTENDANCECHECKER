package com.example.attendancecheck

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.attendancecheck.databinding.LoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class student_login : AppCompatActivity() {

    private lateinit var binding: LoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.signsupRedirectText.setOnClickListener {
            val intent = Intent(this, SIGNUP::class.java)
            startActivity(intent)
        }

        binding.loginButton.setOnClickListener {
            val email = binding.LoginEmail.text.toString()
            val password = binding.loginPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = firebaseAuth.currentUser?.uid
                            if (userId != null) {
                                // Retrieve the user's role from Firestore
                                firestore.collection("users").document(userId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (document != null && document.exists()) {
                                            val role = document.getString("role")
                                            if (role == "student") {
                                                Toast.makeText(this, "Student Login Successful!", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(this, student_qr::class.java)
                                                startActivity(intent)
                                                finish()
                                            } else if (role == "teacher") {
                                                Toast.makeText(this, "Teacher Login Successful!", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(this, MainActivity::class.java)
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                Toast.makeText(this, "User role not recognized.", Toast.LENGTH_SHORT).show()
                                                // Optionally, you might want to log the user out here
                                                firebaseAuth.signOut()
                                            }
                                        } else {
                                            Toast.makeText(this, "Error: Could not retrieve user data.", Toast.LENGTH_SHORT).show()
                                            // Optionally, you might want to log the user out here
                                            firebaseAuth.signOut()
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(this, "Error retrieving user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                                        // Optionally, you might want to log the user out here
                                        firebaseAuth.signOut()
                                    }
                            } else {
                                Toast.makeText(this, "Error: Could not get current user ID.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}