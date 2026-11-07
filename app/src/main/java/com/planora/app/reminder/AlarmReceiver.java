package com.planora.app.reminder;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.planora.app.App;
import com.planora.app.R;
import com.planora.app.data.AppDb;
import com.planora.app.data.Repo;
import com.planora.app.data.TaskEntity;
import com.planora.app.ui.AddTaskActivity;
import com.planora.app.util.Util;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        final PendingResult pr = goAsync();
        final String taskId = intent.getStringExtra("taskId");
        final String action = intent.getAction();
        final int stage = intent.getIntExtra("stage", 1);
        final int rid = intent.getIntExtra("rid", 0);
        final Context app = c.getApplicationContext();

        Repo.IO.execute(() -> {
            try {
                if (taskId == null) return;

                if (Alarms.ACTION_DISMISS.equals(action)) {
                    ReminderSoundPlayer.stopIfMatching(taskId);
                    NotificationManagerCompat.from(app).cancel(taskId.hashCode());
                    Alarms.cancelRepeat(app, taskId);
                    return;
                }

                if (Alarms.ACTION_DONE.equals(action)) {
                    ReminderSoundPlayer.stopIfMatching(taskId);
                    NotificationManagerCompat.from(app).cancel(taskId.hashCode());
                    Alarms.cancelRepeat(app, taskId);
                    Repo.setDone(app, taskId, true);
                    return;
                }

                if (Alarms.ACTION_REMINDER.equals(action)) {
                    TaskEntity t = AppDb.get(app).taskDao().get(taskId);
                    if (t == null || t.isDeleted || t.status == 1) {
                        Alarms.cancelRepeat(app, taskId);
                        NotificationManagerCompat.from(app).cancel(taskId.hashCode());
                        ReminderSoundPlayer.stopIfMatching(taskId);
                        return;
                    }

                    // Sound & Vibration duration rule:
                    // 1st trigger (stage 1): 5s
                    // 2nd trigger (stage 2, 1 min later): 5s
                    // Subsequent triggers (stage 3+, every 5 mins): 10s
                    int durationMs = (stage == 1 || stage == 2) ? 5000 : 10000;
                    ReminderSoundPlayer.play(app, taskId, durationMs);

                    // Display notification with Dismiss and Mark done actions
                    show(app, t, stage);

                    // Schedule next repeat trigger:
                    // Stage 1 -> 1 minute later (Stage 2)
                    // Stage 2+ -> 5 minutes later (Stage N+1)
                    long now = System.currentTimeMillis();
                    long nextAt = (stage == 1) ? (now + 60_000L) : (now + 300_000L);
                    int nextStage = stage + 1;
                    Alarms.scheduleRepeat(app, rid, taskId, t.title, nextStage, nextAt);
                }
            } catch (Exception ignored) {
            } finally {
                pr.finish();
            }
        });
    }

    private void show(Context c, TaskEntity t, int stage) {
        Intent open = new Intent(c, com.planora.app.ui.TaskReminderActivity.class).putExtra("task_id", t.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pOpen = PendingIntent.getActivity(c, t.id.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent dismiss = new Intent(c, AlarmReceiver.class).setAction(Alarms.ACTION_DISMISS)
                .putExtra("taskId", t.id);
        PendingIntent pDismiss = PendingIntent.getBroadcast(c, t.id.hashCode() + 1000, dismiss,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent done = new Intent(c, AlarmReceiver.class).setAction(Alarms.ACTION_DONE)
                .putExtra("taskId", t.id);
        PendingIntent pDone = PendingIntent.getBroadcast(c, t.id.hashCode() + 2000, done,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = t.dueAt > 0 ? "Due " + Util.dateTime(t.dueAt) : "Task reminder";
        if (t.description != null && !t.description.isEmpty()) text += " • " + t.description;

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, App.CH_REMINDERS)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(t.title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setAutoCancel(true)
                .setContentIntent(pOpen)
                .setDeleteIntent(pDismiss)
                .addAction(0, "Dismiss", pDismiss)
                .addAction(0, "Mark done", pDone);
        try {
            NotificationManagerCompat.from(c).notify(t.id.hashCode(), b.build());
        } catch (SecurityException ignored) {
            // Notification permission denied – app must not crash
        }
    }
}
