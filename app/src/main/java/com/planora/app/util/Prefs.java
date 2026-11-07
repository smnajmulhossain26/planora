package com.planora.app.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("planora_prefs", Context.MODE_PRIVATE);
    }

    /** 0 = system, 1 = light, 2 = dark */
    public int theme() { return sp.getInt("theme", 0); }
    public void setTheme(int t) { sp.edit().putInt("theme", t).apply(); }

    public boolean onboardingDone() { return sp.getBoolean("onboarding_done", false); }
    public void setOnboardingDone() { sp.edit().putBoolean("onboarding_done", true).apply(); }

    public long lastSync(String uid) { return sp.getLong("last_sync_" + uid, 0); }
    public void setLastSync(String uid, long t) { sp.edit().putLong("last_sync_" + uid, t).apply(); }

    public boolean syncFailed() { return sp.getBoolean("sync_failed", false); }
    public void setSyncFailed(boolean f) { sp.edit().putBoolean("sync_failed", f).apply(); }

    public boolean permSetupDone() { return sp.getBoolean("perm_setup_done", false); }
    public void setPermSetupDone() { sp.edit().putBoolean("perm_setup_done", true).apply(); }

    public boolean notifAsked() { return sp.getBoolean("notif_asked", false); }
    public void setNotifAsked() { sp.edit().putBoolean("notif_asked", true).apply(); }

    public String calcHistory() { return sp.getString("calc_history", ""); }
    public void setCalcHistory(String s) { sp.edit().putString("calc_history", s).apply(); }
}
