package com.planora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.planora.app.R;
import com.planora.app.calc.CalculatorFragment;
import com.planora.app.databinding.ActivityMainBinding;
import com.planora.app.locker.LockerFragment;
import com.planora.app.locker.LockerSession;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding b;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment f;
            boolean fab = false;
            if (id == R.id.nav_planner) { f = new PlannerFragment(); fab = true; }
            else if (id == R.id.nav_calc) f = new CalculatorFragment();
            else if (id == R.id.nav_locker) f = new LockerFragment();
            else if (id == R.id.nav_profile) f = new ProfileFragment();
            else { f = new HomeFragment(); fab = true; }
            b.fab.setVisibility(fab ? View.VISIBLE : View.GONE);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
            return true;
        });
        if (s == null) b.nav.setSelectedItemId(R.id.nav_home);
        b.fab.setOnClickListener(v -> startActivity(new Intent(this, AddTaskActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            if (new com.planora.app.locker.LockPrefs(this).enabled()
                    && com.planora.app.locker.LockService.hasPermissions(this)
                    && !com.planora.app.locker.LockService.isRunning()) {
                androidx.core.content.ContextCompat.startForegroundService(this,
                        new Intent(this, com.planora.app.locker.LockService.class));
            }
        } catch (Exception ignored) { }
    }

    public void selectHome() {
        if (b != null) b.nav.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) LockerSession.authed = false; // ask for the Locker password again next time
    }
}
