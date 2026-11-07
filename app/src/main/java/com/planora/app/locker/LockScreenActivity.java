package com.planora.app.locker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.planora.app.databinding.ActivityLockScreenBinding;

/** Password screen. Used (1) over a locked app and (2) as a gate before opening App Locker settings. */
public class LockScreenActivity extends AppCompatActivity {
    private ActivityLockScreenBinding b;
    private String pkg;
    private boolean gate;
    private LockPrefs prefs;

    private final ActivityResultLauncher<Intent> reset =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK) {
                    b.tilPass.setError(null);
                    b.etPass.setText("");
                    com.planora.app.util.Util.toast(this, "Password changed. Enter your new password.");
                }
            });

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        b = ActivityLockScreenBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        prefs = new LockPrefs(this);
        gate = getIntent().getBooleanExtra("gate", false);
        bindTarget(getIntent());

        b.btnUnlock.setOnClickListener(v -> tryUnlock());
        b.tvForgot.setOnClickListener(v -> reset.launch(new Intent(this, LockerResetActivity.class)));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (gate) { setResult(RESULT_CANCELED); finish(); }
                else goHome();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        bindTarget(intent);
        b.etPass.setText("");
    }

    private void bindTarget(Intent i) {
        pkg = i.getStringExtra("pkg");
        if (gate || pkg == null) {
            b.tvTitle.setText("Enter App Locker password");
            return;
        }
        try {
            PackageManager pm = getPackageManager();
            Drawable icon = pm.getApplicationIcon(pkg);
            CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
            b.ivIcon.setImageDrawable(icon);
            b.tvTitle.setText(label + " is locked");
        } catch (Exception e) {
            b.tvTitle.setText("This app is locked");
        }
    }

    private void tryUnlock() {
        String p = b.etPass.getText() == null ? "" : b.etPass.getText().toString();
        if (prefs.check(p)) {
            if (gate) {
                setResult(RESULT_OK);
            } else if (pkg != null) {
                LockService.markUnlocked(pkg);
            }
            finish();
        } else {
            b.tilPass.setError("Wrong password");
        }
    }

    private void goHome() {
        Intent h = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(h);
        finish();
    }
}
