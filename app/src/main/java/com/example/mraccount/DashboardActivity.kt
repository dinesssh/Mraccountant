package com.example.mraccount

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class DashboardActivity : AppCompatActivity() {

    private lateinit var locationHelper: LocationHelper
    private var tvCurrentLocation: TextView? = null
    private val PREF_NAME = "mr_accountant_prefs"
    private var mediaPlayer: MediaPlayer? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchAndShowLocation()
        } else {
            tvCurrentLocation?.text = "Current Location: Permission Denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        try {
            // Increment App Launch Count
            val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val launchCount = prefs.getInt("app_launch_count", 0) + 1
            prefs.edit().putInt("app_launch_count", launchCount).apply()

            // Display Launch Count and Welcome Message
            findViewById<TextView>(R.id.tvLaunchCount)?.text = "You opened this app $launchCount times"
            val userName = prefs.getString("user_name", "User")
            findViewById<TextView>(R.id.tvWelcomeUser)?.text = "Welcome, $userName"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Start Foreground Reminder Service
        startReminderService()

        locationHelper = LocationHelper(this)
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false) // Hide default app title
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        val btnAddEntry = findViewById<MaterialCardView>(R.id.btnNavAddEntry)
        val btnSummary = findViewById<MaterialCardView>(R.id.btnNavSummary)
        val btnBirthday = findViewById<MaterialCardView>(R.id.btnNavBirthday)
        val btnBudget = findViewById<MaterialCardView>(R.id.btnNavBudget)
        val btnHelpVideo = findViewById<MaterialCardView>(R.id.btnHelpVideo)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        btnAddEntry?.setOnClickListener {
            val intent = Intent(this, AddEntryActivity::class.java)
            startActivity(intent)
        }

        btnSummary?.setOnClickListener {
            val intent = Intent(this, SummaryActivity::class.java)
            startActivity(intent)
        }

        btnBirthday?.setOnClickListener {
            val intent = Intent(this, BirthdayReminderActivity::class.java)
            startActivity(intent)
        }

        btnBudget?.setOnClickListener {
            val intent = Intent(this, BudgetActivity::class.java)
            startActivity(intent)
        }

        btnHelpVideo?.setOnClickListener {
            val intent = Intent(this, HelpVideoActivity::class.java)
            startActivity(intent)
        }

        bottomNavigation?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_entries -> {
                    Toast.makeText(this, "Entries clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_reports -> {
                    val intent = Intent(this, ReportsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_settings -> {
                    showLogoutDialog()
                    true
                }
                else -> false
            }
        }

        checkLocationPermissions()
    }

    override fun onResume() {
        super.onResume()
        try {
            updateBudgetUI()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBudgetUI() {
        try {
            val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val budgetLimit = prefs.getFloat("budget_limit", 0.0f)
            val totalExpense = prefs.getFloat("total_expense", 0.0f)
            val lastExpense = prefs.getFloat("last_expense", 0.0f)
            val alertSent = prefs.getBoolean("alert_sent", false)
            
            val remaining = budgetLimit - totalExpense
            // Safety: Avoid divide-by-zero
            val percent = if (budgetLimit > 0) ((totalExpense / budgetLimit) * 100).toInt() else 0

            findViewById<TextView>(R.id.tvBudgetLimit)?.text = "Limit: ₹$budgetLimit"
            findViewById<TextView>(R.id.tvTotalSpent)?.text = "Spent: ₹$totalExpense"
            findViewById<TextView>(R.id.tvRemainingBudget)?.text = "Rem: ₹$remaining"
            findViewById<TextView>(R.id.tvBudgetPercent)?.text = "$percent%"
            findViewById<TextView>(R.id.tvLastExpense)?.text = "Last Expense: ₹$lastExpense"

            val progressBar = findViewById<ProgressBar>(R.id.pbBudgetProgress)
            progressBar?.let { 
                it.progress = if (percent > 100) 100 else percent
            }
            
            // Budget Warning System
            val tvWarning = findViewById<TextView>(R.id.tvBudgetWarning)
            if (tvWarning != null) {
                when {
                    percent >= 100 -> {
                        tvWarning.text = "Budget exceeded"
                        tvWarning.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                        tvWarning.visibility = View.VISIBLE
                        
                        // Play alert sound if not already sent
                        if (!alertSent && budgetLimit > 0) {
                            playBudgetAlertSound()
                            prefs.edit().putBoolean("alert_sent", true).apply()
                        }
                    }
                    percent >= 80 -> {
                        tvWarning.text = "Budget almost reached"
                        tvWarning.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                        tvWarning.visibility = View.VISIBLE
                    }
                    else -> {
                        tvWarning.visibility = View.GONE
                        // Reset alert flag if we are below budget (optional, depends on requirement)
                        // But requirement says "Prevent repeated playback using the existing alert_sent flag"
                    }
                }
            }

            // Spending Insight System
            val tvInsight = findViewById<TextView>(R.id.tvInsightMessage)
            tvInsight?.text = when {
                percent < 30 -> "Great! You are managing your budget well."
                percent in 30..70 -> "Keep tracking your expenses."
                else -> "You are close to your budget limit."
            }

            // Visual indicator for over-budget on percent text
            if (totalExpense > budgetLimit) {
                findViewById<TextView>(R.id.tvBudgetPercent)?.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            } else {
                findViewById<TextView>(R.id.tvBudgetPercent)?.setTextColor(Color.BLACK)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playBudgetAlertSound() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.budget_alert)
            mediaPlayer?.setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startReminderService() {
        try {
            val serviceIntent = Intent(this, ReminderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkLocationPermissions() {
        if (locationHelper.hasLocationPermissions()) {
            fetchAndShowLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchAndShowLocation() {
        locationHelper.getCurrentCity { city ->
            tvCurrentLocation?.text = city ?: "Mountain View" // Defaulting for visual consistency if null
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to close Mr. Account?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setCancelable(false)
            .setPositiveButton("Logout") { _, _ ->
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_about -> {
                Toast.makeText(this, "About App clicked", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
