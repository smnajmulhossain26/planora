package com.planora.app.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.planora.app.data.AppDb;
import com.planora.app.data.ReminderEntity;
import com.planora.app.data.Repo;
import com.planora.app.data.TaskEntity;

public final class Alarms {
    private Alarms() {}

    public static final String ACTION_REMINDER = "com.planora.app.REMINDER";
    public static final String ACTION_DISMISS = "com.planora.app.DISMISS";
    public static final String ACTION_DONE = "com.planora.app.DONE";

    public static boolean canExact(Context c) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    public static void schedule(Context c, int rid, String taskId, String title, long at) {
        scheduleRepeat(c, rid, taskId, title, 1, at);
    }

    public static void scheduleRepeat(Context c, int rid, String taskId, String title, int stage, long at) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_REMINDER);
        i.putExtra("taskId", taskId);
        i.putExtra("title", title);
        i.putExtra("stage", stage);
        i.putExtra("rid", rid);
        int reqCode = taskId != null ? taskId.hashCode() : rid;
        PendingIntent p = PendingIntent.getBroadcast(c, reqCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (canExact(c)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            }
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, at, p);
        }
    }

    public static void cancelRepeat(Context c, String taskId) {
        if (taskId == null) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_REMINDER);
        PendingIntent p = PendingIntent.getBroadcast(c, taskId.hashCode(), i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (p != null) {
            am.cancel(p);
            p.cancel();
        }
    }

    public static void cancel(Context c, int rid) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_REMINDER);
        PendingIntent p = PendingIntent.getBroadcast(c, rid, i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (p != null) {
            am.cancel(p);
            p.cancel();
        }
    }

    /** After reboot / time change / permission change: schedule all future reminders again. */
    public static void rescheduleAll(Context c) {
        AppDb db = AppDb.get(c);
        for (ReminderEntity r : db.reminderDao().future(System.currentTimeMillis())) {
            TaskEntity t = db.taskDao().get(r.taskId);
            if (t == null || t.isDeleted || t.status == 1) continue;
            schedule(c, r.id, t.id, t.title, r.triggerAt);
        }
    }

    public static void rebuildAll(Context c, String uid) {
        for (TaskEntity t : AppDb.get(c).taskDao().allPending(uid)) Repo.rebuildReminders(c, t);
    }

    public static void cancelAll(Context c) {
        for (ReminderEntity r : AppDb.get(c).reminderDao().all()) {
            cancel(c, r.id);
            cancelRepeat(c, r.taskId);
            ReminderSoundPlayer.stopIfMatching(r.taskId);
        }
    }
}
