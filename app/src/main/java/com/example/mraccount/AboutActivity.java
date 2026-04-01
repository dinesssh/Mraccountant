package com.example.mraccount;

import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView tvEmail = findViewById(R.id.tvEmail);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvWebsite = findViewById(R.id.tvWebsite);
        Button btnBack = findViewById(R.id.btnBack);

        // Applying Linkify for clickable links
        if (tvEmail != null) Linkify.addLinks(tvEmail, Linkify.EMAIL_ADDRESSES);
        if (tvPhone != null) Linkify.addLinks(tvPhone, Linkify.PHONE_NUMBERS);
        if (tvWebsite != null) Linkify.addLinks(tvWebsite, Linkify.WEB_URLS);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }
}