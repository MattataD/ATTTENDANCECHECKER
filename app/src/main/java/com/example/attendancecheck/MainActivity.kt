package com.example.attendancecheck

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var pendingSaveData: Triple<String, String, String>? = null
    private var currentFileName by mutableStateOf(generateNewFileName())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRCodeScreen(
                onSaveData = { name, studentId ->
                    Log.d("PermissionFlow", "onSaveData lambda called with name: $name, id: $studentId")
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    pendingSaveData = Triple(name, studentId, timestamp)
                    checkAndRequestStoragePermission()
                },
                onNewList = {
                    currentFileName = generateNewFileName()
                    Toast.makeText(this, "Starting a new attendance list: $currentFileName", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun generateNewFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "attendance_$timestamp.csv"
    }

    private fun checkAndRequestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("PermissionRequest", "Requesting WRITE_EXTERNAL_STORAGE permission")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        } else {
            Log.d("PermissionFlow", "Storage permission already granted.")
            pendingSaveData?.let { (name, studentId, timestamp) ->
                saveDataLocally(name, studentId, timestamp, currentFileName)
                pendingSaveData = null
            }
        }
    }

    private fun saveDataLocally(name: String, studentId: String, timestamp: String, fileName: String) {
        val data = "$name,$studentId,$timestamp\n"
        val file = File(getExternalFilesDir(null), fileName)

        try {
            val fileWriter = FileWriter(file, true)
            fileWriter.append(data)
            fileWriter.close()
            Toast.makeText(this, "Data saved to $fileName!", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("PermissionResult", "Request Code: $requestCode")
        if (permissions.isNotEmpty()) {
            Log.d("PermissionResult", "Permissions: ${permissions.joinToString()}")
        }
        if (grantResults.isNotEmpty()) {
            Log.d("PermissionResult", "Grant Results: ${grantResults.joinToString()}")
        }
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PermissionResult", "Storage permission GRANTED")
                    Toast.makeText(this, "Storage permission granted!", Toast.LENGTH_SHORT).show()
                    pendingSaveData?.let { (name, studentId, timestamp) ->
                        saveDataLocally(name, studentId, timestamp, currentFileName)
                        pendingSaveData = null
                    }
                } else {
                    Log.d("PermissionResult", "Storage permission DENIED")
                    Toast.makeText(this, "Storage permission denied!", Toast.LENGTH_SHORT).show()
                    Toast.makeText(this, "Storage permission is required to save data.", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                Log.d("PermissionResult", "Unknown request code: $requestCode")
            }
        }
    }

    companion object {
        const val STORAGE_PERMISSION_CODE = 101
    }
}

@Composable
fun QRCodeScreen(onSaveData: (String, String) -> Unit, onNewList: () -> Unit) {
    val context = LocalContext.current
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(generateQRCodeBitmap("Prototype Data Collection")) }
    var isQrCodeEnabled by remember { mutableStateOf(true) }
    var showInputDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var studentIdInput by remember { mutableStateOf("") }
    var newListClicked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "QR Code Attendance (Prototype)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrCodeBitmap != null && isQrCodeEnabled) {
                    Image(
                        bitmap = qrCodeBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code for Prototype",
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    Text("QR Code is Off")
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Data Collection:")
            Switch(
                checked = isQrCodeEnabled,
                onCheckedChange = { isChecked ->
                    isQrCodeEnabled = isChecked
                    qrCodeBitmap = if (isChecked) generateQRCodeBitmap("Prototype Data Collection") else null
                }
            )
        }

        Button(
            onClick = { showInputDialog = true },
            enabled = isQrCodeEnabled
        ) {
            Text("Simulate QR Scan & Enter Info")
        }

        Button(
            onClick = {
                onNewList()
                newListClicked = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start New Attendance List")
        }

        if (showInputDialog) {
            AlertDialog(
                onDismissRequest = { showInputDialog = false },
                title = { Text("Enter Information") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Name") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = studentIdInput,
                            onValueChange = { studentIdInput = it },
                            label = { Text("Student ID") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onSaveData(nameInput, studentIdInput)
                        nameInput = ""
                        studentIdInput = ""
                        showInputDialog = false
                        newListClicked = false
                    }) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    Button(onClick = { showInputDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (newListClicked) {
            Text(
                text = "New list started. Enter attendance data.",
                color = MaterialTheme.colors.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

fun generateQRCodeBitmap(text: String): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512, mapOf(EncodeHintType.MARGIN to 0))
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bmp
}

@Preview(showBackground = true)
@Composable
fun QRCodeScreenPreview() {
    QRCodeScreen(onSaveData = { _, _ -> }, onNewList = {})
}