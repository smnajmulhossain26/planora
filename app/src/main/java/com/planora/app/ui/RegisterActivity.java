package com.planora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.planora.app.data.Cloud;
import com.planora.app.databinding.ActivityRegisterBinding;
import com.planora.app.util.Prefs;
import com.planora.app.util.Util;

/** Creates the account and logs the user in immediately (no email verification step). */
public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding b;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.btnBack.setOnClickListener(v -> finish());
        b.btnRegister.setOnClickListener(v -> register());
    }

    private String t(com.google.android.material.textfield.TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString();
    }

    private void busy(boolean on) {
        b.progress.setVisibility(on ? View.VISIBLE : View.GONE);
        b.btnRegister.setEnabled(!on);
    }

    private void register() {
        String name = t(b.etName).trim(), email = t(b.etEmail).trim(), p = t(b.etPass), c = t(b.etConfirm);
        b.tilName.setError(null); b.tilEmail.setError(null); b.tilPass.setError(null); b.tilConfirm.setError(null);
        if (name.isEmpty()) { b.tilName.setError("Enter your full name"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.tilEmail.setError("Invalid email"); return; }
        if (p.length() < 6) { b.tilPass.setError("At least 6 characters"); return; }
        if (!p.equals(c)) { b.tilConfirm.setError("Passwords do not match"); return; }
        if (!Util.online(this)) { Util.toast(this, "No internet connection. Connect to the internet to create an account."); return; }
        busy(true);
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, p).addOnSuccessListener(res -> {
            FirebaseUser u = res.getUser();   // the new user is already signed in
            if (u == null) { busy(false); return; }
            u.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build())
                    .addOnCompleteListener(x -> goNext(u));
        }).addOnFailureListener(e -> {
            busy(false);
            Util.toast(this, Util.authError(e));
        });
    }

    private void goNext(FirebaseUser u) {
        Cloud.afterLogin(this, u);
        boolean first = !new Prefs(this).permSetupDone();
        startActivity(new Intent(this, first ? PermissionCenterActivity.class : MainActivity.class)
                .putExtra("first_run", first)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
