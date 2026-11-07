package com.planora.app.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.planora.app.R;
import com.planora.app.data.Cloud;
import com.planora.app.data.Repo;
import com.planora.app.databinding.FragmentProfileBinding;
import com.planora.app.reminder.Alarms;
import com.planora.app.util.Util;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding b;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private GoogleSignInClient google;

    private final ActivityResultLauncher<PickVisualMediaRequest> picker = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), this::uploadAvatar);

    private final ActivityResultLauncher<Intent> googleReauth = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                try {
                    GoogleSignInAccount a = GoogleSignIn.getSignedInAccountFromIntent(r.getData()).getResult(ApiException.class);
                    AuthCredential cred = GoogleAuthProvider.getCredential(a.getIdToken(), null);
                    FirebaseUser u = auth.getCurrentUser();
                    if (u != null) u.reauthenticate(cred).addOnSuccessListener(x -> performDelete(u))
                            .addOnFailureListener(e -> Util.toast(requireContext(), Util.authError(e)));
                } catch (ApiException e) {
                    Util.toast(requireContext(), "Verification cancelled.");
                }
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentProfileBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        google = GoogleSignIn.getClient(requireActivity(), gso);
        bindUser();
        b.ivAvatar.setOnClickListener(x -> picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        b.btnEdit.setOnClickListener(x -> editName());
        b.btnPassword.setOnClickListener(x -> changePassword());
        b.btnPerms.setOnClickListener(x -> startActivity(new Intent(getContext(), PermissionCenterActivity.class)));
        b.btnSettings.setOnClickListener(x -> startActivity(new Intent(getContext(), SettingsActivity.class)));
        b.btnLogout.setOnClickListener(x -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out?").setMessage("Your data stays safe in the cloud.")
                .setPositiveButton("Log out", (d, w) -> logout()).setNegativeButton("Cancel", null).show());
        b.btnDelete.setOnClickListener(x -> confirmDelete());
    }

    private void bindUser() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) return;
        b.tvName.setText(u.getDisplayName() == null || u.getDisplayName().isEmpty() ? "Planora user" : u.getDisplayName());
        b.tvEmail.setText(u.getEmail());
        b.tvType.setText("Account: " + (Cloud.isGoogle(u) ? "Google" : "Email & Password"));
        b.btnPassword.setVisibility(Cloud.isGoogle(u) ? View.GONE : View.VISIBLE);
        if (u.getPhotoUrl() != null) Glide.with(this).load(u.getPhotoUrl()).circleCrop().into(b.ivAvatar);
    }

    private void editName() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) return;
        Util.input(requireContext(), "Edit name", "Full name", u.getDisplayName(), InputType.TYPE_CLASS_TEXT, name -> {
            if (name.isEmpty()) return;
            u.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build())
                    .addOnSuccessListener(x -> {
                        Cloud.userDoc(u.getUid()).update("name", name);
                        bindUser();
                    }).addOnFailureListener(e -> Util.toast(requireContext(), Util.authError(e)));
        });
    }

    private void changePassword() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null || u.getEmail() == null) return;
        auth.sendPasswordResetEmail(u.getEmail())
                .addOnSuccessListener(x -> Util.toast(requireContext(), "Password change link sent to " + u.getEmail()))
                .addOnFailureListener(e -> Util.toast(requireContext(), Util.authError(e)));
    }

    private void uploadAvatar(Uri uri) {
        FirebaseUser u = auth.getCurrentUser();
        if (uri == null || u == null) return;
        if (!Util.online(requireContext())) { Util.toast(requireContext(), "Internet needed to upload a profile picture."); return; }
        Glide.with(this).load(uri).circleCrop().into(b.ivAvatar);
        com.google.firebase.storage.StorageReference ref = FirebaseStorage.getInstance()
                .getReference("users/" + u.getUid() + "/profile.jpg");
        ref.putFile(uri).continueWithTask(t -> ref.getDownloadUrl()).addOnSuccessListener(url -> {
            u.updateProfile(new UserProfileChangeRequest.Builder().setPhotoUri(url).build());
            Cloud.userDoc(u.getUid()).update("photoUrl", url.toString());
        }).addOnFailureListener(e -> Util.toast(requireContext(), "Upload failed: " + e.getMessage()));
    }

    private void logout() {
        Repo.IO.execute(() -> {
            Alarms.cancelAll(requireContext().getApplicationContext());
            com.planora.app.data.AppDb.get(requireContext()).reminderDao().deleteAll();
        });
        auth.signOut();
        google.signOut();
        goLogin();
    }

    private void goLogin() {
        startActivity(new Intent(getContext(), LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    // ---------- Delete account (re-authentication required) ----------
    private void confirmDelete() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) return;
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete account?")
                .setMessage("This permanently deletes your account, cloud data and local data. You must verify your identity first.")
                .setPositiveButton("Continue", (d, w) -> {
                    if (Cloud.isGoogle(u)) {
                        google.signOut().addOnCompleteListener(t -> googleReauth.launch(google.getSignInIntent()));
                    } else {
                        Util.input(requireContext(), "Enter your password", "Password",
                                "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, pass -> {
                                    AuthCredential cred = EmailAuthProvider.getCredential(u.getEmail(), pass);
                                    u.reauthenticate(cred).addOnSuccessListener(x -> performDelete(u))
                                            .addOnFailureListener(e -> Util.toast(requireContext(), Util.authError(e)));
                                });
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    @SuppressWarnings("deprecation")
    private void performDelete(FirebaseUser u) {
        final android.content.Context app = requireContext().getApplicationContext();
        ProgressDialog pd = ProgressDialog.show(requireContext(), null, "Deleting account…", true, false);
        final String uid = u.getUid();
        Repo.IO.execute(() -> {
            try {
                deleteCollection(Cloud.userDoc(uid).collection("tasks"));
                deleteCollection(Cloud.userDoc(uid).collection("categories"));
                Cloud.await(Cloud.userDoc(uid).delete());
                try { Cloud.await(FirebaseStorage.getInstance().getReference("users/" + uid + "/profile.jpg").delete()); }
                catch (Exception ignored) { }
                Cloud.await(u.delete());
                Repo.wipeLocal(app, uid);
                requireActivity().runOnUiThread(() -> { pd.dismiss(); google.signOut(); goLogin(); });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    pd.dismiss();
                    Util.toast(requireContext(), "Could not delete account. Check your internet and try again.");
                });
            }
        });
    }

    private void deleteCollection(CollectionReference col) throws Exception {
        QuerySnapshot qs = Cloud.await(col.get());
        WriteBatch batch = Cloud.fs().batch();
        int n = 0;
        for (DocumentSnapshot d : qs.getDocuments()) {
            batch.delete(d.getReference());
            if (++n == 400) { Cloud.await(batch.commit()); batch = Cloud.fs().batch(); n = 0; }
        }
        if (n > 0) Cloud.await(batch.commit());
    }
}
