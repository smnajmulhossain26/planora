package com.planora.app.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.planora.app.databinding.ActivityForgotBinding;
import com.planora.app.util.Util;

/** Uses Firebase's secure password-reset email link. */
public class ForgotPasswordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        ActivityForgotBinding b = ActivityForgotBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.btnBack.setOnClickListener(v -> finish());
        b.btnSend.setOnClickListener(v -> {
            String email = b.etEmail.getText() == null ? "" : b.etEmail.getText().toString().trim();
            b.tilEmail.setError(null);
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.tilEmail.setError("Invalid email"); return; }
            b.progress.setVisibility(View.VISIBLE);
            b.btnSend.setEnabled(false);
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener(x -> {
                        Util.toast(this, "Reset link sent. Open it from your email, set a new password, then log in.");
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        b.progress.setVisibility(View.GONE);
                        b.btnSend.setEnabled(true);
                        Util.toast(this, Util.authError(e));
                    });
        });
    }
}
