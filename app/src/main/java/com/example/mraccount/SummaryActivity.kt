package com.example.mraccount

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SummaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}
