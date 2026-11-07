package com.planora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.planora.app.databinding.ActivitySplashBinding;
import com.planora.app.util.Prefs;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(ActivitySplashBinding.inflate(getLayoutInflater()).getRoot());
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Prefs prefs = new Prefs(this);
            Intent i;
            if (!prefs.onboardingDone()) i = new Intent(this, OnboardingActivity.class);
            else if (FirebaseAuth.getInstance().getCurrentUser() == null) i = new Intent(this, LoginActivity.class);
            else if (!prefs.permSetupDone()) i = new Intent(this, PermissionCenterActivity.class).putExtra("first_run", true);
            else i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
        }, 1500);
    }
}
