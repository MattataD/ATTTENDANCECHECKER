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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class student_qr : AppCompatActivity() {

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
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    startCamera()
                } else {
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                resultTextView.text = "Camera permission is needed to scan QR codes."
            }
        }

        // ✅ Bottom Navigation View Setup
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.qr -> {
                    // Already on this screen
                    true
                }
                R.id.student -> {
                    val intent = Intent(this, studentprofile::class.java)
                    startActivity(intent)
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
    }




    private fun startCamera() {
        Log.d("STUDENTQR", "startCamera() called")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val screenSize = Size(1080, 720) // Adjust as needed
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
    private fun student_qr.handleBarcode(barcode: Barcode) {
        val rawValue = barcode.rawValue
        if (!rawValue.isNullOrEmpty()) {
            Log.d("STUDENTQR", "Detected Barcode Value: $rawValue")
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
    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanningActive) {
            val imageRotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, imageRotationDegrees)

            // 1. Get the bounds of PreviewView and qrCodeFinder
            val previewViewRect = previewView.run {
                val location = IntArray(2)
                getLocationOnScreen(location)
                android.graphics.Rect(location[0], location[1], location[0] + width, location[1] + height)
            }

            val qrCodeFinderRectAbsolute = qrCodeFinder.run {
                val location = IntArray(2)
                getLocationOnScreen(location)
                android.graphics.Rect(location[0], location[1], location[0] + width, location[1] + height)
            }

            // 2. Calculate the intersection of qrCodeFinder with PreviewView
            val scanningRectAbsolute = android.graphics.Rect(qrCodeFinderRectAbsolute)
            if (!scanningRectAbsolute.intersect(previewViewRect)) {
                imageProxy.close()
                return // Finder is not within the preview, no need to process
            }

            // 3. Calculate the relative coordinates of the scanning rect within PreviewView
            val relativeLeft = scanningRectAbsolute.left - previewViewRect.left
            val relativeTop = scanningRectAbsolute.top - previewViewRect.top
            val relativeRight = relativeLeft + scanningRectAbsolute.width()
            val relativeBottom = relativeTop + scanningRectAbsolute.height()

            // 4. Normalize the relative coordinates based on the image dimensions and rotation
            val imageWidth = mediaImage.width
            val imageHeight = mediaImage.height

            val normalizedRect = when (imageRotationDegrees) {
                0 -> android.graphics.Rect(
                    (relativeLeft * imageWidth / previewView.width.toFloat()).toInt(),
                    (relativeTop * imageHeight / previewView.height.toFloat()).toInt(),
                    (relativeRight * imageWidth / previewView.width.toFloat()).toInt(),
                    (relativeBottom * imageHeight / previewView.height.toFloat()).toInt()
                )
                90 -> android.graphics.Rect(
                    (relativeTop * imageWidth / previewView.height.toFloat()).toInt(),
                    ((previewView.width - relativeRight) * imageHeight / previewView.width.toFloat()).toInt(),
                    (relativeBottom * imageWidth / previewView.height.toFloat()).toInt(),
                    ((previewView.width - relativeLeft) * imageHeight / previewView.width.toFloat()).toInt()
                )
                180 -> android.graphics.Rect(
                    ((previewView.width - relativeRight) * imageWidth / previewView.width.toFloat()).toInt(),
                    ((previewView.height - relativeBottom) * imageHeight / previewView.height.toFloat()).toInt(),
                    ((previewView.width - relativeLeft) * imageWidth / previewView.width.toFloat()).toInt(),
                    ((previewView.height - relativeTop) * imageHeight / previewView.height.toFloat()).toInt()
                )
                270 -> android.graphics.Rect(
                    ((previewView.height - relativeBottom) * imageWidth / previewView.height.toFloat()).toInt(),
                    ((relativeLeft) * imageHeight / previewView.width.toFloat()).toInt(),
                    ((previewView.height - relativeTop) * imageWidth / previewView.height.toFloat()).toInt(),
                    ((relativeRight) * imageHeight / previewView.width.toFloat()).toInt()
                )
                else -> android.graphics.Rect(0, 0, imageWidth, imageHeight) // Default to full image
            }

            barcodeScanner.process(image) // Process the full image
                .addOnSuccessListener { barcodes ->
                    Log.d("STUDENTQR", "Processed full image. Found ${barcodes.size} barcodes")
                    if (barcodes.isNotEmpty()) {
                        // Filter barcodes to check if they are within the normalizedRect
                        val validBarcodes = barcodes.filter { barcode ->
                            val boundingBox = barcode.boundingBox
                            boundingBox?.let {
                                normalizedRect.contains(it.left, it.top) && normalizedRect.contains(it.right, it.bottom)
                            } ?: false
                        }

                        if (validBarcodes.isNotEmpty()) {
                            isScanningActive = false
                            scanButton.text = "Scan QR Code"
                            cameraPreviewContainer.visibility = View.GONE
                            stopCamera()
                            validBarcodes.forEach { barcode ->
                                handleBarcode(barcode) // Now handleBarcode is in the correct scope
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("STUDENTQR", "Barcode processing failed on full image", e)
                    // Optionally handle failure
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
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

