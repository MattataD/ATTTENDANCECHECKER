package com.example.attendancecheck

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class studentprofile : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_studentprofile) // Assuming this is the name of your layout

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        setupCopyOnLongClick(findViewById(R.id.textView2))
        setupCopyOnLongClick(findViewById(R.id.textView3))
        setupCopyOnLongClick(findViewById(R.id.textView4))
        setupCopyOnLongClick(findViewById(R.id.editText1) as EditText)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.qr -> {
                    val intent = Intent(this, student_qr::class.java) // Assuming navibar handles QR
                    startActivity(intent)
                    true
                }
                R.id.student -> {
                    // We are already in the Studentprofile activity, so do nothing
                    true
                }
                R.id.logout -> {
                    val intent = Intent(this, student_login::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        // Set the "Student" item as initially selected
        bottomNavigationView.selectedItemId = R.id.student
    }

    private fun setupCopyOnLongClick(view: View) {
        view.setOnLongClickListener {
            if (view is TextView || view is EditText) {
                val text = (view as? TextView)?.text?.toString() ?: (view as? EditText)?.text?.toString() ?: ""

                if (text.isNotBlank()) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Copied Text", text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Text copied to clipboard!", Toast.LENGTH_SHORT).show()

                    // Optional visual feedback
                    view.alpha = 0.5f
                    view.postDelayed({ view.alpha = 1f }, 150)
                    return@setOnLongClickListener true
                } else {
                    Toast.makeText(this, "No text to copy", Toast.LENGTH_SHORT).show()
                }
            }
            false
        }
    }
}