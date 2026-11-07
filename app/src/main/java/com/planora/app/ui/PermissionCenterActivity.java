package com.planora.app.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.planora.app.databinding.ActivityPermissionsBinding;
import com.planora.app.locker.LockService;
import com.planora.app.reminder.Alarms;
import com.planora.app.util.Prefs;
import com.planora.app.util.Util;

/**
 * Permission Center. With extra "first_run" it is shown once right after login
 * (Notifications + Exact alarm first). A denied permission never crashes the app.
 */
public class PermissionCenterActivity extends AppCompatActivity {
    private ActivityPermissionsBinding b;
    private boolean firstRun;

    private final ActivityResultLauncher<String> notifLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), g -> {
                if (!g) Util.toast(this, "Notifications are off, so reminders cannot be shown. You can allow them later.");
                refresh();
            });

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityPermissionsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        firstRun = getIntent().getBooleanExtra("first_run", false);
        Prefs prefs = new Prefs(this);

        if (firstRun) {
            b.toolbar.setVisibility(View.GONE);
            b.headerBlock.setVisibility(View.VISIBLE);
            b.btnContinue.setVisibility(View.VISIBLE);
            b.btnContinue.setOnClickListener(v -> finishSetup());
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override public void handleOnBackPressed() { finishSetup(); }
            });
            // Notifications are asked first, with the official system dialog
            if (Build.VERSION.SDK_INT >= 33 && !hasNotif() && !prefs.notifAsked()) {
                prefs.setNotifAsked();
                b.getRoot().post(() -> notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS));
            }
        } else {
            b.toolbar.setNavigationOnClickListener(v -> finish());
        }

        b.btnNotif.setOnClickListener(v -> {
            boolean rationale = Build.VERSION.SDK_INT >= 33
                    && shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS);
            if (Build.VERSION.SDK_INT >= 33 && (!prefs.notifAsked() || rationale)) {
                prefs.setNotifAsked();
                notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            } else {
                startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
            }
        });
        b.btnAlarm.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 31)
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
        });
        b.btnBattery.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        b.btnUsage.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        b.btnOverlay.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        b.btnFile.setVisibility(View.GONE);
    }

    private void finishSetup() {
        new Prefs(this).setPermSetupDone();
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private boolean hasNotif() {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void set(TextView tv, MaterialButton btn, boolean ok, String okText, String badText) {
        tv.setText(ok ? okText : badText);
        btn.setVisibility(ok ? View.GONE : View.VISIBLE);
    }

    private void refresh() {
        set(b.tvNotifStatus, b.btnNotif, hasNotif(), "Granted ✓", "Not granted – reminders won't be shown");
        set(b.tvAlarmStatus, b.btnAlarm, Alarms.canExact(this), "Enabled ✓", "Disabled – reminders may be slightly late");
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        boolean ignoring = pm.isIgnoringBatteryOptimizations(getPackageName());
        b.tvBatteryStatus.setText(ignoring ? "Unrestricted ✓" : "Optimized (optional to change)");
        b.tvFileStatus.setText("No permission needed ✓");
        set(b.tvUsageStatus, b.btnUsage, LockService.hasUsageAccess(this), "Granted ✓", "Not granted – needed for App Locker");
        set(b.tvOverlayStatus, b.btnOverlay, LockService.hasOverlay(this), "Granted ✓", "Not granted – needed for App Locker");
    }
}
