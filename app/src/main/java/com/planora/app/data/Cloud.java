package com.planora.app.data;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Cloud {
    private Cloud() {}

    public static <T> T await(Task<T> t) throws Exception {
        return Tasks.await(t, 30, TimeUnit.SECONDS);
    }

    public static FirebaseFirestore fs() {
        return FirebaseFirestore.getInstance();
    }

    public static DocumentReference userDoc(String uid) {
        return fs().collection("users").document(uid);
    }

    /** Room is the source of truth, so Firestore's own disk cache is turned off. */
    public static void initFirestore() {
        try {
            fs().setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(false).build());
        } catch (Exception ignored) {
        }
    }

    public static boolean isGoogle(FirebaseUser u) {
        for (UserInfo i : u.getProviderData()) {
            if ("google.com".equals(i.getProviderId())) return true;
        }
        return false;
    }

    public static Map<String, Object> userMap(FirebaseUser u) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", u.getDisplayName() == null ? "" : u.getDisplayName());
        m.put("email", u.getEmail() == null ? "" : u.getEmail());
        m.put("photoUrl", u.getPhotoUrl() == null ? "" : u.getPhotoUrl().toString());
        m.put("authProvider", isGoogle(u) ? "google" : "password");
        long created = u.getMetadata() != null ? u.getMetadata().getCreationTimestamp()
                : System.currentTimeMillis();
        m.put("createdAt", created);
        m.put("updatedAt", System.currentTimeMillis());
        return m;
    }

    /** Called right after a successful login. */
    public static void afterLogin(Context c, FirebaseUser u) {
        final Context app = c.getApplicationContext();
        try {
            userDoc(u.getUid()).set(userMap(u), SetOptions.merge());
        } catch (Exception ignored) {
        }
        Repo.IO.execute(() -> {
            Repo.seedDefaults(app, u.getUid());
            com.planora.app.reminder.Alarms.rebuildAll(app, u.getUid());
            SyncWorker.enqueueNow(app);
        });
    }
}
