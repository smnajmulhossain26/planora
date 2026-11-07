package com.planora.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.planora.app.data.Cloud;
import com.planora.app.data.SyncWorker;
import com.planora.app.util.Prefs;

public class App extends Application {
    public static final String CH_REMINDERS = "reminders";
    public static final String CH_LOCKER = "locker";

    @Override
    public void onCreate() {
        super.onCreate();
        applyTheme(new Prefs(this).theme());
        Cloud.initFirestore();
        createChannels();
        SyncWorker.schedulePeriodic(this);
    }

    public static void applyTheme(int t) {
        AppCompatDelegate.setDefaultNightMode(t == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                : t == 2 ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel r = new NotificationChannel(CH_REMINDERS, "Task reminders",
                NotificationManager.IMPORTANCE_HIGH);
        r.enableVibration(true);
        r.setDescription("Reminders for your tasks");
        nm.createNotificationChannel(r);
        NotificationChannel l = new NotificationChannel(CH_LOCKER, "App Locker",
                NotificationManager.IMPORTANCE_LOW);
        l.setDescription("Shown while App Locker is protecting your apps");
        nm.createNotificationChannel(l);
    }
}
