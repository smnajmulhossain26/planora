package com.planora.app.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

public final class Util {
    private Util() {}

    public static long dayStart(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long addDays(long dayStart, int days) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dayStart);
        c.add(Calendar.DAY_OF_YEAR, days);
        return c.getTimeInMillis();
    }

    public static String fmt(long t, String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(t));
    }

    public static String date(long t) { return fmt(t, "EEE, d MMM yyyy"); }
    public static String time(long t) { return fmt(t, "h:mm a"); }
    public static String dateTime(long t) { return fmt(t, "d MMM, h:mm a"); }

    public static boolean online(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            if (n == null) return false;
            NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return false;
        }
    }

    public static void toast(Context c, String m) {
        Toast.makeText(c, m, Toast.LENGTH_LONG).show();
    }

    public static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }

    public static String offsetLabel(int m) {
        if (m == 0) return "At time";
        if (m % 60 == 0) return (m / 60) + " hour before";
        return m + " min before";
    }

    public static void input(Context c, String title, String hint, String init, int inputType,
                             Consumer<String> cb) {
        EditText et = new EditText(c);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setText(init == null ? "" : init);
        FrameLayout f = new FrameLayout(c);
        int p = dp(c, 20);
        f.setPadding(p, dp(c, 8), p, 0);
        f.addView(et);
        new MaterialAlertDialogBuilder(c).setTitle(title).setView(f)
                .setPositiveButton("OK", (d, w) -> cb.accept(et.getText().toString().trim()))
                .setNegativeButton("Cancel", null).show();
    }

    public static String authError(Exception e) {
        if (e instanceof com.google.firebase.FirebaseNetworkException) return "No internet connection. Please try again.";
        if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) return "No account found with this email.";
        if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) return "Invalid email or wrong password.";
        if (e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) return "An account already exists with this email.";
        if (e instanceof com.google.firebase.auth.FirebaseAuthWeakPasswordException) return "Password is too weak (min 6 characters).";
        if (e instanceof com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) return "Please log in again and retry.";
        return e == null || e.getMessage() == null ? "Something went wrong." : e.getMessage();
    }
}
