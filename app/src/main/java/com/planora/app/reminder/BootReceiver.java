package com.planora.app.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.planora.app.data.Repo;
import com.planora.app.locker.LockPrefs;
import com.planora.app.locker.LockService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        final PendingResult pr = goAsync();
        final Context app = c.getApplicationContext();
        Repo.IO.execute(() -> {
            try {
                Alarms.rescheduleAll(app);
                LockPrefs prefs = new LockPrefs(app);
                if (prefs.enabled() && LockService.hasPermissions(app)) {
                    if (!LockService.isRunning() || "com.planora.app.RESTART_LOCKER".equals(intent.getAction())) {
                        ContextCompat.startForegroundService(app, new Intent(app, LockService.class));
                    }
                }
            } catch (Exception ignored) {
            } finally {
                pr.finish();
            }
        });
    }
}
