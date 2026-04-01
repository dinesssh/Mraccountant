package com.example.mraccount;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class DashboardActivity extends AppCompatActivity {

    private final String PREF_NAME = "mr_accountant_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Initialize Ads
        MobileAds.initialize(this, status -> {});
        AdView mAdView = findViewById(R.id.adView);
        if (mAdView != null) mAdView.loadAd(new AdRequest.Builder().build());

        // 2. Toolbar Setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        // 3. Quick Action Button Listeners
        findViewById(R.id.btnShareReport).setOnClickListener(v -> shareReport());

        setupNavButton(R.id.btnNavAddEntry, AddEntryActivity.class);
        setupNavButton(R.id.btnNavSummary, SummaryActivity.class);
        setupNavButton(R.id.btnHelpVideo, HelpVideoActivity.class);

        // 4. Bottom Navigation Logic
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        if (nav != null) {
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_reports) startActivity(new Intent(this, ReportsActivity.class));
                if (id == R.id.nav_settings) showLogoutDialog();
                return true;
            });
        }

        updateDashboardStats();
    }

    private void setupNavButton(int id, Class<?> cls) {
        MaterialCardView btn = findViewById(id);
        if (btn != null) btn.setOnClickListener(v -> startActivity(new Intent(this, cls)));
    }

    private void updateDashboardStats() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        float total = prefs.getFloat("total_expense", 0.0f);
        TextView tv = findViewById(R.id.tvTotalExpense);
        if (tv != null) tv.setText("₹" + String.format("%.0f", total));
    }

    private void shareReport() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String report = "📊 MrAccountant Summary\nTotal Spent: ₹" + prefs.getFloat("total_expense", 0.0f);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, report);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (d, w) -> {
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_about) startActivity(new Intent(this, AboutActivity.class));
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboardStats();
    }
}