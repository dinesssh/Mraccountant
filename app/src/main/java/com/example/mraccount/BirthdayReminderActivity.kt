package com.example.mraccount

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.*

class BirthdayReminderActivity : AppCompatActivity() {

    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0
    private var selectedHour = -1
    private var selectedMinute = -1

    // Handle Permission Request for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied. Reminder might not show.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_birthday_reminder)

        // Check for Notification Permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val btnDate = findViewById<Button>(R.id.btnSelectDate)
        val btnTime = findViewById<Button>(R.id.btnSelectTime)
        val btnSchedule = findViewById<Button>(R.id.btnScheduleReminder)
        val tvDate = findViewById<TextView>(R.id.tvSelectedDate)
        val tvTime = findViewById<TextView>(R.id.tvSelectedTime)

        val calendar = Calendar.getInstance()

        btnDate.setOnClickListener {
            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedYear = year
                selectedMonth = month
                selectedDay = dayOfMonth
                tvDate.text = "Date: $dayOfMonth/${month + 1}/$year"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        btnTime.setOnClickListener {
            val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                tvTime.text = String.format("Time: %02d:%02d", hourOfDay, minute)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
            timePicker.show()
        }

        btnSchedule.setOnClickListener {
            if (selectedYear == 0 || selectedHour == -1) {
                Toast.makeText(this, "Please select Date and Time first", Toast.LENGTH_SHORT).show()
            } else {
                checkAlarmPermissionAndSchedule()
            }
        }
    }

    private fun checkAlarmPermissionAndSchedule() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Android 12+ requires exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                Toast.makeText(this, "Please allow Exact Alarm permission for the reminder", Toast.LENGTH_LONG).show()
                return
            }
        }
        scheduleAlarm()
    }

    private fun scheduleAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderBroadcastReceiver::class.java)
        
        // FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE is required for modern Android
        val pendingIntent = PendingIntent.getBroadcast(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)

        // Ensure the time hasn't already passed
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, "Selected time has already passed!", Toast.LENGTH_SHORT).show()
            return
        }

        // Use setExactAndAllowWhileIdle for critical reminders like birthdays
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }

        Toast.makeText(this, "Birthday Reminder Scheduled! 🎉", Toast.LENGTH_SHORT).show()
        finish()
    }
}
