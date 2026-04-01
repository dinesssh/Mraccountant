package com.example.mraccount

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BillCaptureActivity : AppCompatActivity() {

    private lateinit var ivBillPreview: ImageView
    private lateinit var tvBillInfo: TextView
    private var latestImageUri: Uri? = null
    private var latestImagePath: String? = null

    // Register TakePicture contract
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            displayCapturedBill()
        } else {
            Toast.makeText(this, "Image capture failed or cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bill_capture)

        ivBillPreview = findViewById(R.id.ivBillPreview)
        tvBillInfo = findViewById(R.id.tvBillInfo)
        val btnCapture = findViewById<Button>(R.id.btnCaptureBill)

        btnCapture.setOnClickListener {
            prepareAndLaunchCamera()
        }
    }

    private fun prepareAndLaunchCamera() {
        // 1. Generate unique Bill ID and Timestamp
        val billId = UUID.randomUUID().toString().substring(0, 8).uppercase()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        // 2. Prepare storage directory (Internal Storage)
        val storageDir = File(filesDir, "bill_images")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val fileName = "BILL_${billId}_${timestamp}.jpg"
        val imageFile = File(storageDir, fileName)
        latestImagePath = imageFile.absolutePath

        // 3. Get URI using FileProvider
        try {
            // FileProvider.getUriForFile returns a non-nullable Uri
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                imageFile
            )

            latestImageUri = uri

            // 4. Launch Camera - Passing the non-nullable 'uri' variable
            takePictureLauncher.launch(uri)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayCapturedBill() {
        // setImageURI accepts nullable Uri?
        ivBillPreview.setImageURI(latestImageUri)

        val fileName = latestImagePath?.let { File(it).name } ?: "Unknown"
        tvBillInfo.text = "Captured Successfully!\nPath: $latestImagePath\nFile: $fileName"

        Toast.makeText(this, "Bill Saved: $fileName", Toast.LENGTH_LONG).show()
    }
}