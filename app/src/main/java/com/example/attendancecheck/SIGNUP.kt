package com.example.attendancecheck

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.attendancecheck.databinding.LoginBinding
import com.example.attendancecheck.databinding.SingupBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class SIGNUP : AppCompatActivity(){

    private lateinit var binding: SingupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SingupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.loginRedirectText.setOnClickListener {
            val intent = Intent(this, LOGIN::class.java) // Replace MainActivity with your intended screen
            startActivity(intent)
        }
        binding.signbutton.setOnClickListener {
            val email = binding.signupEmail.text.toString()
            val name = binding.signupName.text.toString()
            val tupid = binding.signupTupid.text.toString()
            val password = binding.signupPassword.text.toString()

            if (email.isNotEmpty() && name.isNotEmpty() && tupid.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task -> // Corrected lambda syntax
                        if (task.isSuccessful) {
                            val intent = Intent(this, LOGIN::class.java) // Replace MainActivity with your intended screen
                            startActivity(intent)
                            finish() // Optional: Close the signup activity
                        } else {
                            Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }
        // Your activity initialization code here
    }
}