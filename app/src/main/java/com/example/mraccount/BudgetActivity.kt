package com.example.mraccount

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class BudgetActivity : AppCompatActivity() {

    private val PREF_NAME = "mr_accountant_prefs"
    private val KEY_BUDGET = "budget_limit"
    private val KEY_ALERT_SENT = "alert_sent"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        val etBudget = findViewById<TextInputEditText>(R.id.etBudget)
        val btnSave = findViewById<Button>(R.id.btnSaveBudget)

        // Pre-fill existing budget if any
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentBudget = prefs.getFloat(KEY_BUDGET, 0.0f)
        if (currentBudget > 0) {
            etBudget.setText(currentBudget.toString())
        }

        btnSave.setOnClickListener {
            val budgetStr = etBudget.text.toString()
            val budget = budgetStr.toFloatOrNull()

            if (budget != null && budget > 0) {
                saveBudget(budget)
            } else {
                Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBudget(amount: Float) {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(KEY_BUDGET, amount)
            putBoolean(KEY_ALERT_SENT, false) // Reset alert flag as budget is updated
            apply()
        }
        
        Toast.makeText(this, "Budget Saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
