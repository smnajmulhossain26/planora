package com.planora.app.locker;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.planora.app.App;
import com.planora.app.R;
import com.planora.app.databinding.OverlayLockBinding;
import com.planora.app.reminder.BootReceiver;

/**
 * App Locker engine.
 * 1) A foreground service watches which app is in front (UsageStatsManager events).
 * 2) When a locked app comes to the front, a full-screen password overlay is drawn on top of it
 *    (TYPE_APPLICATION_OVERLAY – needs "Display over other apps", and is NOT blocked by
 *    Android's background-activity-start rules).
 * 3) Correct password -> overlay disappears and the app stays open until the user leaves it
 *    (Home / another app / screen off). Opening it again asks for the password again.
 */
public class LockService extends Service {
    private static volatile String unlockedPkg;
    private static volatile boolean RUNNING;

    public static boolean isRunning() { return RUNNING; }

    private HandlerThread thread;
    private Handler bg, ui;
    private LockPrefs prefs;
    private UsageStatsManager usm;
    private WindowManager wm;
    private InputMethodManager imm;

    private volatile String current;      // package currently in front
    private volatile String overlayPkg;   // package the overlay is showing for (null = no overlay)
    private long lastEventTs;
    private volatile boolean running;

    private FrameLayout overlayRoot;
    private OverlayLockBinding ob;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            unlockedPkg = null;   // lock everything again when screen turns off or locks
            current = null;       // so that next "resume" of the same app is detected
            if (ui != null) ui.post(LockService.this::hideOverlay);
        }
    };

    // ---------------- permission helpers ----------------
    public static boolean hasUsageAccess(Context c) {
        try {
            AppOpsManager ops = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
            int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.getPackageName());
            if (mode == AppOpsManager.MODE_DEFAULT) {
                return c.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED;
            }
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasOverlay(Context c) { return Settings.canDrawOverlays(c); }

    public static boolean hasPermissions(Context c) { return hasUsageAccess(c) && hasOverlay(c); }

    public static void markUnlocked(String pkg) { unlockedPkg = pkg; }

    // ---------------- lifecycle ----------------
    @Override
    public void onCreate() {
        super.onCreate();
        RUNNING = true;
        prefs = new LockPrefs(this);
        usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        ui = new Handler(Looper.getMainLooper());
        thread = new HandlerThread("planora-locker");
        thread.start();
        bg = new Handler(thread.getLooper());

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = new NotificationCompat.Builder(this, App.CH_LOCKER)
                .setSmallIcon(R.drawable.ic_lock)
                .setContentTitle("Planora App Locker is active")
                .setContentText("Your selected apps are protected")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                ServiceCompat.startForeground(this, 77, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(77, n);
            }
        } catch (Exception e) {
            ui.post(() -> Toast.makeText(getApplicationContext(),
                    "App Lock could not start. Please open Planora and turn it on again.", Toast.LENGTH_LONG).show());
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!running) {
            running = true;
            bg.post(loop);
        }

        scheduleWatchdog();
        return START_STICKY;
    }

    private void scheduleWatchdog() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                Intent i = new Intent(this, BootReceiver.class).setAction("com.planora.app.RESTART_LOCKER");
                PendingIntent pi = PendingIntent.getBroadcast(this, 9999, i,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                long nextCheck = System.currentTimeMillis() + 15 * 60 * 1000L; // Check every 15 minutes
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextCheck, pi);
            }
        } catch (Exception ignored) {}
    }

    private final Runnable loop = new Runnable() {
        @Override public void run() {
            if (!running) return;
            try { tick(); } catch (Exception ignored) { }
            bg.postDelayed(this, 250);
        }
    };

    // ---------------- detection ----------------
    private void tick() {
        if (!hasPermissions(this)) {
            if (overlayPkg != null) ui.post(this::hideOverlay);
            return;
        }
        long now = System.currentTimeMillis();
        long from = lastEventTs == 0 ? now - 10000 : lastEventTs - 1000;
        if (from > now) from = now - 10000;

        UsageEvents ev = usm.queryEvents(from, now);
        if (ev != null) {
            UsageEvents.Event e = new UsageEvents.Event();
            while (ev.hasNextEvent()) {
                ev.getNextEvent(e);
                long ts = e.getTimeStamp();
                if (ts <= lastEventTs) continue;      // already handled
                lastEventTs = ts;
                int type = e.getEventType();
                if (type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    String pkg = e.getPackageName();
                    if (pkg != null && !pkg.equals(current)) {
                        if (unlockedPkg != null && !unlockedPkg.equals(pkg)) unlockedPkg = null; // user left the unlocked app
                        current = pkg;
                    }
                }
            }
        }

        if (current == null) {
            current = getForegroundPackageFallback(now);
        }

        String cur = current;
        if (cur != null && !cur.equals(getPackageName()) && !cur.equals(unlockedPkg) && isLocked(cur, now)) {
            if (!cur.equals(overlayPkg)) ui.post(() -> showOverlay(cur));
        } else if (overlayPkg != null) {
            ui.post(this::hideOverlay);
        }
    }

    private String getForegroundPackageFallback(long now) {
        try {
            UsageEvents ev = usm.queryEvents(now - 10000, now);
            String lastPkg = null;
            long lastTs = 0;
            if (ev != null) {
                UsageEvents.Event e = new UsageEvents.Event();
                while (ev.hasNextEvent()) {
                    ev.getNextEvent(e);
                    int type = e.getEventType();
                    if (type == UsageEvents.Event.MOVE_TO_FOREGROUND && e.getTimeStamp() > lastTs) {
                        lastTs = e.getTimeStamp();
                        lastPkg = e.getPackageName();
                    }
                }
            }
            return lastPkg;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isLocked(String pkg, long now) {
        return prefs != null && prefs.isLocked(pkg);
    }

    // ---------------- overlay ----------------
    private void showOverlay(String pkg) {
        if (overlayRoot != null && pkg.equals(overlayPkg)) return;
        removeOverlayView();
        try {
            Context themed = new ContextThemeWrapper(this, R.style.Theme_Planora);
            ob = OverlayLockBinding.inflate(LayoutInflater.from(themed));
            try {
                PackageManager pm = getPackageManager();
                ob.ivIcon.setPadding(0, 0, 0, 0);
                ob.ivIcon.setImageDrawable(pm.getApplicationIcon(pkg));
                ob.tvTitle.setText(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)) + " is locked");
            } catch (Exception e) {
                ob.tvTitle.setText("This app is locked");
            }
            overlayRoot = new FrameLayout(themed) {
                @Override public boolean dispatchKeyEvent(KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                        if (event.getAction() == KeyEvent.ACTION_UP) goHome();
                        return true;
                    }
                    return super.dispatchKeyEvent(event);
                }
            };
            overlayRoot.addView(ob.getRoot(), new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, type,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                            | WindowManager.LayoutParams.FLAG_SECURE,
                    PixelFormat.OPAQUE);
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
            wm.addView(overlayRoot, lp);
            overlayPkg = pkg;

            ob.btnUnlock.setOnClickListener(v -> tryUnlock());
            ob.etPass.setOnEditorActionListener((v, actionId, e) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                    tryUnlock();
                    return true;
                }
                return false;
            });
            ob.tvForgot.setOnClickListener(v -> {
                hideOverlay();
                try {
                    startActivity(new Intent(this, LockerResetActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (Exception ignored) { }
            });
            ob.etPass.postDelayed(() -> {
                if (ob == null) return;
                ob.etPass.requestFocus();
                imm.showSoftInput(ob.etPass, InputMethodManager.SHOW_IMPLICIT);
            }, 250);
        } catch (Exception e) {
            // Could not draw overlay -> fall back to lock screen activity
            overlayRoot = null;
            ob = null;
            overlayPkg = null;
            try {
                startActivity(new Intent(this, LockScreenActivity.class).putExtra("pkg", pkg)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (Exception ignored) { }
        }
    }

    private void tryUnlock() {
        if (ob == null || overlayPkg == null) return;
        String p = ob.etPass.getText() == null ? "" : ob.etPass.getText().toString();
        if (prefs.check(p)) {
            unlockedPkg = overlayPkg;
            hideOverlay();
        } else {
            ob.tilPass.setError("Wrong password");
            ob.etPass.setText("");
        }
    }

    private void goHome() {
        hideOverlay();
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) { }
    }

    private void hideOverlay() {
        removeOverlayView();
        overlayPkg = null;
    }

    private void removeOverlayView() {
        if (overlayRoot != null) {
            try {
                if (ob != null) imm.hideSoftInputFromWindow(ob.etPass.getWindowToken(), 0);
                wm.removeView(overlayRoot);
            } catch (Exception ignored) { }
        }
        overlayRoot = null;
        ob = null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        triggerRestart();
    }

    @Override
    public void onDestroy() {
        running = false;
        RUNNING = false;
        try { unregisterReceiver(screenReceiver); } catch (Exception ignored) { }
        ui.removeCallbacksAndMessages(null);
        hideOverlay();
        if (thread != null) thread.quitSafely();
        triggerRestart();
        super.onDestroy();
    }

    private void triggerRestart() {
        if (prefs != null && prefs.enabled() && hasPermissions(this)) {
            try {
                Intent restartIntent = new Intent(this, BootReceiver.class).setAction("com.planora.app.RESTART_LOCKER");
                PendingIntent pi = PendingIntent.getBroadcast(this, 8888, restartIntent,
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pi);
                }
            } catch (Exception ignored) {}

            try {
                ContextCompat.startForegroundService(getApplicationContext(), new Intent(getApplicationContext(), LockService.class));
            } catch (Exception ignored) {}
        }
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
