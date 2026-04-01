package com.example.mraccount

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class AddEntryActivity : AppCompatActivity() {

    private var progressDialog: AlertDialog? = null
    private lateinit var locationHelper: LocationHelper
    private var currentCity: String = "Unknown"

    // SharedPreferences Constants
    private val PREF_NAME = "mr_accountant_prefs"
    private val KEY_BUDGET = "budget_limit"
    private val KEY_TOTAL = "total_expense"
    private val KEY_ALERT_SENT = "alert_sent"
    private val KEY_LAST_EXPENSE = "last_expense"
    private val KEY_FOOD_EXPENSE = "food_expense"
    private val KEY_TRAVEL_EXPENSE = "travel_expense"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchLocation()
        }
        if (permissions[Manifest.permission.SEND_SMS] == true) {
            Toast.makeText(this, "SMS Permission Granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_entry)

        locationHelper = LocationHelper(this)
        checkPermissions()

        val btnSave = findViewById<Button>(R.id.btnSaveEntry)
        btnSave.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun fetchLocation() {
        locationHelper.getCurrentCity { city ->
            currentCity = city ?: "Unknown"
        }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Entry")
            .setMessage("Are you sure you want to save this transaction?")
            .setPositiveButton("Yes") { _, _ ->
                saveTransaction()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun saveTransaction() {
        showProgressDialog()

        // Ensure we try to get location again if it's still unknown
        if (currentCity == "Unknown") {
            locationHelper.getCurrentCity { city ->
                currentCity = city ?: "Unknown"
                executeSave()
            }
        } else {
            executeSave()
        }
    }

    private fun executeSave() {
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val etCategory = findViewById<EditText>(R.id.etCategory)
        val rbExpense = findViewById<RadioButton>(R.id.rbExpense)
        
        val amountStr = etAmount.text.toString()
        val category = etCategory.text.toString().lowercase()
        val amount = amountStr.toDoubleOrNull() ?: 0.0

        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        if (rbExpense.isChecked) {
            val currentTotal = prefs.getFloat(KEY_TOTAL, 0.0f)
            val budgetLimit = prefs.getFloat(KEY_BUDGET, 5000.0f)
            val alertSent = prefs.getBoolean(KEY_ALERT_SENT, false)
            
            val newTotal = currentTotal + amount.toFloat()
            
            // Update specific categories for report
            if (category.contains("food") || category.contains("eat")) {
                val currentFood = prefs.getFloat(KEY_FOOD_EXPENSE, 0.0f)
                editor.putFloat(KEY_FOOD_EXPENSE, currentFood + amount.toFloat())
            } else if (category.contains("travel") || category.contains("car") || category.contains("bus") || category.contains("fuel")) {
                val currentTravel = prefs.getFloat(KEY_TRAVEL_EXPENSE, 0.0f)
                editor.putFloat(KEY_TRAVEL_EXPENSE, currentTravel + amount.toFloat())
            }

            // Update total and last expense
            editor.putFloat(KEY_TOTAL, newTotal)
            editor.putFloat(KEY_LAST_EXPENSE, amount.toFloat())

            // SMS Alert Logic
            if (newTotal > budgetLimit && !alertSent) {
                sendBudgetAlertSms(newTotal, budgetLimit)
                editor.putBoolean(KEY_ALERT_SENT, true)
            }
        } else {
            // Income logic (optional, but keep total updated if needed)
            // For now, only focus on expenses for the report
        }
        
        editor.apply()

        Handler(Looper.getMainLooper()).postDelayed({
            dismissProgressDialog()
            Toast.makeText(this, "Transaction saved at $currentCity", Toast.LENGTH_SHORT).show()
            sendStatusNotification("Transaction Added", "Your new entry at $currentCity has been recorded.")
            finish()
        }, 1500)
    }

    private fun sendBudgetAlertSms(total: Float, limit: Float) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    this.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                
                val message = "MrAccountant Alert: Budget Exceeded! Total Expense: $total, Budget Limit: $limit"
                val phoneNumber = "5556" 
                
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(this, "Budget Alert SMS Sent", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "SMS Permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showProgressDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_progress, null)
        builder.setView(view)
        builder.setCancelable(false)
        progressDialog = builder.create()
        progressDialog?.show()
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun sendStatusNotification(title: String, message: String) {
        val channelId = "accounting_status_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Status Updates", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}