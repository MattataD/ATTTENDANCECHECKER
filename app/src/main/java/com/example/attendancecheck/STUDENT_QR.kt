package com.example.attendancecheck

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class STUDENTQR : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultTextView: TextView
    private lateinit var scanButton: Button
    private lateinit var qrCodeFinder: View
    private lateinit var cameraPreviewContainer: FrameLayout
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var isScanningActive = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d("STUDENTQR", "Camera permission result: $isGranted")
        if (isGranted) {
            if (isScanningActive) {
                startCamera() // Start camera if permission granted after button press
            }
        } else {
            resultTextView.text = if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                "Camera permission is needed to scan QR codes."
                // Optionally show a dialog explaining why permission is needed
            } else {
                "Camera permission was denied. Please grant it in the app settings."
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = android.net.Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                "Camera permission denied."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webviewactivity)

        previewView = findViewById(R.id.previewView)
        resultTextView = findViewById(R.id.resultTextView)
        scanButton = findViewById(R.id.scanButton)
        qrCodeFinder = findViewById(R.id.qrCodeFinder)
        cameraPreviewContainer = findViewById(R.id.cameraPreviewContainer)

        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()

        scanButton.setOnClickListener {
            isScanningActive = !isScanningActive
            if (isScanningActive) {
                cameraPreviewContainer.visibility = View.VISIBLE
                resultTextView.text = "Scanning..."
                // Check if permission is already granted before starting the camera
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    startCamera()
                } else {
                    // Request permission if not granted
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                scanButton.text = "Stop Scan"
            } else {
                cameraPreviewContainer.visibility = View.GONE
                resultTextView.text = "Press 'Scan QR Code' to start"
                stopCamera()
                scanButton.text = "Scan QR Code"
            }
        }

        // Initial request for camera permission (will only trigger if not already granted)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // Explain why permission is needed (optional)
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                resultTextView.text = "Camera permission is needed to scan QR codes."
                // You might want to show a more user-friendly dialog here
            }
            // Request the permission
            // Note: We don't start the camera here, only when the button is pressed AND permission is granted.
        }
    }

    private fun startCamera() {
        Log.d("STUDENTQR", "startCamera() called")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val screenSize = Size(1280, 720) // Adjust as needed
        val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(screenSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER)
        ).build()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (isScanningActive) {
                            processImageProxy(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                Log.d("STUDENTQR", "Camera bound successfully")
            } catch (e: Exception) {
                resultTextView.text = "Camera binding failed: ${e.message}"
                Log.e("STUDENTQR", "Camera binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onResume() {
        super.onResume()
        previewView.post {
            Log.d("STUDENTQR", "PreviewView Width: ${previewView.width}, Height: ${previewView.height}")
        }
        qrCodeFinder.post {
            Log.d("STUDENTQR", "Finder View Width: ${qrCodeFinder.width}, Height: ${qrCodeFinder.height}, X: ${qrCodeFinder.x}, Y: ${qrCodeFinder.y}")
        }
    }

    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            Log.d("STUDENTQR", "Camera unbound")
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    Log.d("STUDENTQR", "Barcode processing successful. Found ${barcodes.size} barcodes")
                    if (barcodes.isNotEmpty() && isScanningActive) {
                        // Stop scanning immediately after detecting a barcode
                        isScanningActive = false
                        scanButton.text = "Scan QR Code"
                        cameraPreviewContainer.visibility = View.GONE
                        stopCamera()
                        for (barcode in barcodes) {
                            handleBarcode(barcode)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("STUDENTQR", "Barcode processing failed", e)
                    if (isScanningActive) {
                        resultTextView.text = "Failed to scan QR code: ${e.localizedMessage}"
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            Log.d("STUDENTQR", "Media image is null")
            imageProxy.close()
        }
    }

    private fun handleBarcode(barcode: Barcode) {
        val rawValue = barcode.rawValue
        if (!rawValue.isNullOrEmpty()) {
            // Check if the scanned value looks like a URL
            if (rawValue.startsWith("http://") || rawValue.startsWith("https://")) {
                // It's a link, proceed to open it in a WebView or browser
                openWebPage(rawValue)
            } else {
                // It's not a link, handle it as other data (e.g., student ID)
                resultTextView.text = "QR Code Data: $rawValue"
                sendDataToBackend(rawValue)
            }
        } else {
            resultTextView.text = "NO QR CODE DETECTED"
        }
    }

    private fun openWebPage(url: String) {
        val intent = Intent(this, WEBVIEWACTIVITY::class.java).apply {
            putExtra("url", url)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun sendDataToBackend(qrCodeValue: String) {
        // Implement your network request here for non-link data
        resultTextView.text = "Sending data: $qrCodeValue"
        Log.d("STUDENTQR", "Sending QR code value: $qrCodeValue to backend")
    }

    private fun getStudentIdAfterFaceRecognition(): String {
        return "dummyStudentID123" // Placeholder
    }
}