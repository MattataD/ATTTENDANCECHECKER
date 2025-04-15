package com.example.attendancecheck

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.attendancecheck.databinding.SingupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase // If using Realtime Database
import com.google.firebase.firestore.FirebaseFirestore // If using Firestore

class SIGNUP : AppCompatActivity(){

    private lateinit var binding: SingupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    // For Realtime Database
    // private lateinit var database: FirebaseDatabase
    // For Firestore
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SingupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        // For Realtime Database
        // database = FirebaseDatabase.getInstance()
        // For Firestore
        firestore = FirebaseFirestore.getInstance()

        binding.loginRedirectText.setOnClickListener {
            val intent = Intent(this, student_login::class.java) // Adjust if you have separate login screens
            startActivity(intent)
        }

        binding.signbutton.setOnClickListener {
            val email = binding.signupEmail.text.toString()
            val name = binding.signupName.text.toString()
            val tupid = binding.signupTupid.text.toString() // You might rename this based on role
            val password = binding.signupPassword.text.toString()
            val selectedRole = when {
                binding.teacherRadioButton.isChecked -> "teacher"
                binding.studentRadioButton.isChecked -> "student"
                else -> ""
            }

            if (email.isNotEmpty() && name.isNotEmpty() && tupid.isNotEmpty() && password.isNotEmpty() && selectedRole.isNotEmpty()) {
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            val userId = user?.uid

                            if (userId != null) {
                                // Store additional user information in Realtime Database
                                // database.getReference("users").child(userId).child("role").setValue(selectedRole)
                                // database.getReference("users").child(userId).child("name").setValue(name)
                                // database.getReference("users").child(userId).child("tupid").setValue(tupid)

                                // Store additional user information in Firestore
                                val userMap = hashMapOf(
                                    "role" to selectedRole,
                                    "name" to name,
                                    "tupid" to tupid,
                                    "email" to email // Consider if you want to duplicate this
                                )
                                firestore.collection("users").document(userId)
                                    .set(userMap)
                                    .addOnSuccessListener {
                                        val intent = Intent(this, student_login::class.java) // Adjust as needed
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                        // Optionally, you might want to delete the Firebase Auth user if data saving fails
                                    }
                            } else {
                                Toast.makeText(this, "Error getting user ID.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill out all fields and select a role.", Toast.LENGTH_SHORT).show()
            }
        }
        // Your activity initialization code here
    }
}