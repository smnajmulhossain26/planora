package com.planora.app.locker;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;
import com.planora.app.databinding.ActivityLockerResetBinding;
import com.planora.app.util.Util;

import java.util.Collections;
import java.util.Map;

/** Forgot password: a 6-digit code is sent (by Cloud Functions) to the logged-in account's email. */
public class LockerResetActivity extends AppCompatActivity {
    private ActivityLockerResetBinding b;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityLockerResetBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.toolbar.setNavigationOnClickListener(v -> finish());

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || u.getEmail() == null) {
            b.tvInfo.setText("Please log in to Planora first. The code is sent to your account email.");
            b.btnSend.setEnabled(false);
            return;
        }
        b.tvInfo.setText("We will send a 6-digit code to\n" + u.getEmail());
        b.btnSend.setOnClickListener(v -> sendCode());
        b.btnReset.setOnClickListener(v -> verifyAndReset());
    }

    private void sendCode() {
        if (!Util.online(this)) { Util.toast(this, "Internet connection required to send the code."); return; }
        b.btnSend.setEnabled(false);
        FirebaseFunctions.getInstance().getHttpsCallable("sendLockerOtp").call()
                .addOnSuccessListener(r -> {
                    Util.toast(this, "Code sent. Check your email (and spam folder).");
                    b.groupVerify.setVisibility(View.VISIBLE);
                    new CountDownTimer(60000, 1000) {
                        @Override public void onTick(long ms) { b.btnSend.setText("Resend in " + ms / 1000 + "s"); }
                        @Override public void onFinish() { b.btnSend.setText("Resend code"); b.btnSend.setEnabled(true); }
                    }.start();
                })
                .addOnFailureListener(e -> {
                    b.btnSend.setEnabled(true);
                    Util.toast(this, e.getMessage() == null ? "Could not send code." : e.getMessage());
                });
    }

    private void verifyAndReset() {
        String code = b.etCode.getText() == null ? "" : b.etCode.getText().toString().trim();
        String p = b.etNew.getText() == null ? "" : b.etNew.getText().toString();
        String c = b.etConfirm.getText() == null ? "" : b.etConfirm.getText().toString();
        b.tilCode.setError(null);
        b.tilNew.setError(null);
        b.tilConfirm.setError(null);
        if (code.length() != 6) { b.tilCode.setError("Enter the 6-digit code"); return; }
        if (p.length() < 4) { b.tilNew.setError("At least 4 characters"); return; }
        if (!p.equals(c)) { b.tilConfirm.setError("Passwords do not match"); return; }
        b.btnReset.setEnabled(false);
        FirebaseFunctions.getInstance().getHttpsCallable("verifyLockerOtp")
                .call(Collections.singletonMap("code", code))
                .addOnSuccessListener(r -> {
                    Object d = r.getData();
                    boolean ok = d instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) d).get("ok"));
                    if (ok) {
                        new LockPrefs(this).setPassword(p);
                        Util.toast(this, "Password reset successfully.");
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        b.btnReset.setEnabled(true);
                        b.tilCode.setError("Invalid code");
                    }
                })
                .addOnFailureListener(e -> {
                    b.btnReset.setEnabled(true);
                    b.tilCode.setError(e.getMessage() == null ? "Invalid or expired code" : e.getMessage());
                });
    }
}
