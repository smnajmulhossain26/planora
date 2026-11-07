package com.planora.app.locker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Locker settings stored with Android-Keystore backed encryption. The password is only stored as a salted hash. */
public class LockPrefs {
    private final SharedPreferences sp;

    public LockPrefs(Context c) {
        SharedPreferences s;
        try {
            MasterKey mk = new MasterKey.Builder(c.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            s = EncryptedSharedPreferences.create(c.getApplicationContext(), "planora_lock", mk,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            s = c.getApplicationContext().getSharedPreferences("planora_lock_fallback", Context.MODE_PRIVATE);
        }
        sp = s;
    }

    public boolean hasPassword() { return sp.contains("hash"); }

    public void setPassword(String p) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        sp.edit().putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("hash", hash(p, salt)).apply();
    }

    public boolean check(String p) {
        String salt = sp.getString("salt", null), h = sp.getString("hash", null);
        if (salt == null || h == null) return false;
        return MessageDigest.isEqual(h.getBytes(), hash(p, Base64.decode(salt, Base64.NO_WRAP)).getBytes());
    }

    private static String hash(String p, byte[] salt) {
        try {
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] out = f.generateSecret(new PBEKeySpec(p.toCharArray(), salt, 20000, 256)).getEncoded();
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // In-memory copy shared by the UI and the lock service (same process) -> changes apply instantly.
    private static volatile Set<String> CACHE;

    private Set<String> load() {
        Set<String> c = CACHE;
        if (c == null) {
            c = new HashSet<>(sp.getStringSet("locked", new HashSet<>()));
            CACHE = c;
        }
        return c;
    }

    public Set<String> locked() { return new HashSet<>(load()); }

    public boolean isLocked(String pkg) { return load().contains(pkg); }

    public void setLocked(String pkg, boolean on) {
        Set<String> s = locked();
        if (on) s.add(pkg); else s.remove(pkg);
        CACHE = new HashSet<>(s);
        sp.edit().putStringSet("locked", s).commit();
    }

    public boolean enabled() { return sp.getBoolean("enabled", false); }
    public void setEnabled(boolean e) { sp.edit().putBoolean("enabled", e).apply(); }
}
