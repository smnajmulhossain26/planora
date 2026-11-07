package com.planora.app.locker;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.planora.app.databinding.ActivityLockSetupBinding;

/** Set (or change) the App Locker password. */
public class LockSetupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        ActivityLockSetupBinding b = ActivityLockSetupBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.toolbar.setNavigationOnClickListener(v -> finish());
        b.btnSave.setOnClickListener(v -> {
            String p = b.etPass.getText() == null ? "" : b.etPass.getText().toString();
            String c = b.etConfirm.getText() == null ? "" : b.etConfirm.getText().toString();
            b.tilPass.setError(null);
            b.tilConfirm.setError(null);
            if (p.length() < 4) { b.tilPass.setError("At least 4 characters"); return; }
            if (!p.equals(c)) { b.tilConfirm.setError("Passwords do not match"); return; }
            new LockPrefs(this).setPassword(p);
            setResult(RESULT_OK);
            finish();
        });
    }
}
