package com.planora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.planora.app.App;
import com.planora.app.data.SyncStatus;
import com.planora.app.data.SyncWorker;
import com.planora.app.databinding.ActivitySettingsBinding;
import com.planora.app.util.Prefs;
import com.planora.app.util.Util;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        ActivitySettingsBinding b = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.toolbar.setNavigationOnClickListener(v -> finish());
        Prefs prefs = new Prefs(this);

        int t = prefs.theme();
        (t == 1 ? b.rbLight : t == 2 ? b.rbDark : b.rbSystem).setChecked(true);
        b.rgTheme.setOnCheckedChangeListener((g, id) -> {
            int n = id == b.rbLight.getId() ? 1 : id == b.rbDark.getId() ? 2 : 0;
            prefs.setTheme(n);
            App.applyTheme(n);
        });

        SyncStatus.observe(this).observe(this, st -> {
            b.tvSync.setText(st.label);
            b.btnRetry.setVisibility(st.canRetry ? View.VISIBLE : View.GONE);
        });
        b.btnRetry.setOnClickListener(v -> SyncWorker.enqueueNow(this));
        b.btnResync.setOnClickListener(v -> {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) { prefs.setLastSync(uid, 0); SyncWorker.enqueueNow(this); Util.toast(this, "Full re-sync started."); }
        });
        b.btnPerms.setOnClickListener(v -> startActivity(new Intent(this, PermissionCenterActivity.class)));
        b.btnCats.setOnClickListener(v -> startActivity(new Intent(this, CategoriesActivity.class)));
        b.btnNotifSettings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        b.btnLanguage.setOnClickListener(v -> Util.toast(this, "Language: English (more languages coming soon)."));
        b.btnPrivacy.setOnClickListener(v -> new MaterialAlertDialogBuilder(this).setTitle("Privacy Policy")
                .setMessage("Planora stores your tasks on your device first. If you are logged in, they are backed up to your private Firebase account. Nobody else can read your data. App Locker never leaves your phone: only a salted hash of its password is stored, encrypted with the Android Keystore. Planora never accesses files, contacts or other data without your permission.")
                .setPositiveButton("OK", null).show());
        b.btnAbout.setOnClickListener(v -> new MaterialAlertDialogBuilder(this).setTitle("About Planora")
                .setMessage("Planora v1.0\nOffline-first To-Do, Scientific Calculator and App Locker.")
                .setPositiveButton("OK", null).show());
    }
}
