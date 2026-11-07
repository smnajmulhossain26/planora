package com.planora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.planora.app.R;
import com.planora.app.data.Cloud;
import com.planora.app.databinding.ActivityLoginBinding;
import com.planora.app.util.Util;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding b;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private GoogleSignInClient google;

    private final ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                try {
                    GoogleSignInAccount acc = GoogleSignIn.getSignedInAccountFromIntent(r.getData())
                            .getResult(ApiException.class);
                    firebaseWithGoogle(acc.getIdToken());
                } catch (ApiException e) {
                    busy(false);
                    if (e.getStatusCode() != 12501) Util.toast(this, "Google sign-in failed (code " + e.getStatusCode() + ")");
                }
            });

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        google = GoogleSignIn.getClient(this, gso);

        b.btnLogin.setOnClickListener(v -> emailLogin());
        b.btnGoogle.setOnClickListener(v -> {
            if (!Util.online(this)) { Util.toast(this, "No internet connection."); return; }
            busy(true);
            google.signOut().addOnCompleteListener(t -> googleLauncher.launch(google.getSignInIntent()));
        });
        b.tvForgot.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        b.btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void emailLogin() {
        String email = b.etEmail.getText() == null ? "" : b.etEmail.getText().toString().trim();
        String pass = b.etPass.getText() == null ? "" : b.etPass.getText().toString();
        b.tilEmail.setError(null);
        b.tilPass.setError(null);
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.tilEmail.setError("Invalid email"); return; }
        if (pass.isEmpty()) { b.tilPass.setError("Enter your password"); return; }
        busy(true);
        auth.signInWithEmailAndPassword(email, pass).addOnSuccessListener(res -> {
            FirebaseUser u = res.getUser();
            if (u != null) done(u);
        }).addOnFailureListener(e -> {
            busy(false);
            Util.toast(this, Util.authError(e));
        });
    }

    private void firebaseWithGoogle(String idToken) {
        AuthCredential cred = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(cred).addOnSuccessListener(res -> done(res.getUser()))
                .addOnFailureListener(e -> {
                    busy(false);
                    Util.toast(this, Util.authError(e));
                });
    }

    private void done(FirebaseUser u) {
        Cloud.afterLogin(this, u);
        boolean first = !new com.planora.app.util.Prefs(this).permSetupDone();
        Intent i = new Intent(this, first ? PermissionCenterActivity.class : MainActivity.class)
                .putExtra("first_run", first)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void busy(boolean on) {
        b.progress.setVisibility(on ? View.VISIBLE : View.GONE);
        b.btnLogin.setEnabled(!on);
        b.btnGoogle.setEnabled(!on);
    }
}
