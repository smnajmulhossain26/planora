package com.planora.app.data;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.planora.app.reminder.Alarms;

import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** All local writes go through here: Room first, then the sync queue. */
public final class Repo {
    private Repo() {}

    public static final ExecutorService IO = Executors.newFixedThreadPool(2);

    public static String uid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u == null ? null : u.getUid();
    }

    // ---------- Tasks ----------
    public static void saveTask(Context c, TaskEntity t) {
        final Context app = c.getApplicationContext();
        IO.execute(() -> saveTaskSync(app, t));
    }

    static void saveTaskSync(Context c, TaskEntity t) {
        AppDb db = AppDb.get(c);
        long now = System.currentTimeMillis();
        if (t.createdAt == 0) t.createdAt = now;
        t.updatedAt = now;
        t.syncStatus = 1;
        db.taskDao().insert(t);
        rebuildReminders(c, t);
        enqueue(c, "task", t.id);
    }

    public static void setDone(Context c, String taskId, boolean done) {
        final Context app = c.getApplicationContext();
        IO.execute(() -> {
            TaskEntity t = AppDb.get(app).taskDao().get(taskId);
            if (t == null) return;
            t.status = done ? 1 : 0;
            t.completedAt = done ? System.currentTimeMillis() : 0;
            saveTaskSync(app, t);
            if (done && t.repeatType != 0 && t.dueAt != 0) {
                TaskEntity n = copyOf(t);
                n.dueAt = nextDue(t);
                saveTaskSync(app, n);
            }
        });
    }

    public static void deleteTask(Context c, String taskId) {
        final Context app = c.getApplicationContext();
        IO.execute(() -> {
            TaskEntity t = AppDb.get(app).taskDao().get(taskId);
            if (t == null) return;
            t.isDeleted = true;
            saveTaskSync(app, t);
        });
    }

    public static void duplicate(Context c, TaskEntity src) {
        final Context app = c.getApplicationContext();
        IO.execute(() -> {
            TaskEntity n = copyOf(src);
            n.title = src.title + " (copy)";
            saveTaskSync(app, n);
        });
    }

    private static TaskEntity copyOf(TaskEntity s) {
        TaskEntity n = new TaskEntity();
        n.id = UUID.randomUUID().toString();
        n.userId = s.userId;
        n.title = s.title;
        n.description = s.description;
        n.categoryId = s.categoryId;
        n.priority = s.priority;
        n.dueAt = s.dueAt;
        n.reminderOffsets = s.reminderOffsets;
        n.repeatType = s.repeatType;
        n.repeatEvery = s.repeatEvery;
        n.attachmentUri = s.attachmentUri;
        n.attachmentName = s.attachmentName;
        n.status = 0;
        return n;
    }

    private static long nextDue(TaskEntity t) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t.dueAt);
        switch (t.repeatType) {
            case 1: cal.add(Calendar.DAY_OF_YEAR, 1); break;
            case 2: cal.add(Calendar.WEEK_OF_YEAR, 1); break;
            case 3: cal.add(Calendar.MONTH, 1); break;
            case 4: cal.add(Calendar.YEAR, 1); break;
            default: cal.add(Calendar.DAY_OF_YEAR, Math.max(1, t.repeatEvery)); break;
        }
        return cal.getTimeInMillis();
    }

    /** Recreates the reminder rows and AlarmManager alarms for one task. */
    public static void rebuildReminders(Context c, TaskEntity t) {
        AppDb db = AppDb.get(c);
        for (ReminderEntity r : db.reminderDao().forTask(t.id)) Alarms.cancel(c, r.id);
        db.reminderDao().deleteForTask(t.id);
        Alarms.cancelRepeat(c, t.id);
        com.planora.app.reminder.ReminderSoundPlayer.stopIfMatching(t.id);
        if (t.isDeleted || t.status == 1 || t.dueAt == 0
                || t.reminderOffsets == null || t.reminderOffsets.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (String s : t.reminderOffsets.split(",")) {
            int m;
            try { m = Integer.parseInt(s.trim()); } catch (Exception e) { continue; }
            long trig = t.dueAt - m * 60000L;
            if (trig <= now) continue;
            ReminderEntity r = new ReminderEntity();
            r.userId = t.userId;
            r.taskId = t.id;
            r.triggerAt = trig;
            r.id = (int) db.reminderDao().insert(r);
            Alarms.schedule(c, r.id, t.id, t.title, trig);
        }
    }

    // ---------- Categories ----------
    public static void saveCategory(Context c, String idOrNull, String uid, String name) {
        final Context app = c.getApplicationContext();
        IO.execute(() -> {
            AppDb db = AppDb.get(app);
            CategoryEntity cat = idOrNull == null ? null : db.categoryDao().get(idOrNull);
            long now = System.currentTimeMillis();
            if (cat == null) {
                cat = new CategoryEntity();
                cat.id = UUID.randomUUID().toString();
                cat.userId = uid;
                cat.createdAt = now;
            }
            cat.name = name;
            cat.updatedAt = now;
            db.categoryDao().insert(cat);
            enqueue(app, "category", cat.id);
        });
    }

    public static void deleteCategory(Context c, String id) {
        final Context app = c.getApplicationContext();
        IO.execute(() -> {
            AppDb db = AppDb.get(app);
            CategoryEntity cat = db.categoryDao().get(id);
            if (cat == null) return;
            cat.isDeleted = true;
            cat.updatedAt = System.currentTimeMillis();
            db.categoryDao().insert(cat);
            enqueue(app, "category", cat.id);
        });
    }

    public static void seedDefaults(Context c, String uid) {
        AppDb db = AppDb.get(c);
        if (db.categoryDao().countAll(uid) > 0) return;
        String[][] d = {{"study", "Study"}, {"personal", "Personal"}, {"office", "Office"},
                {"shopping", "Shopping"}, {"health", "Health"}, {"family", "Family"}, {"other", "Other"}};
        long now = System.currentTimeMillis();
        for (int i = 0; i < d.length; i++) {
            CategoryEntity cat = new CategoryEntity();
            cat.id = uid + "_" + d[i][0];
            cat.userId = uid;
            cat.name = d[i][1];
            cat.createdAt = now + i;
            cat.updatedAt = now;
            db.categoryDao().insert(cat);
            enqueue(c, "category", cat.id);
        }
    }

    // ---------- Sync queue ----------
    static void enqueue(Context c, String type, String id) {
        SyncQueueEntity q = new SyncQueueEntity();
        q.entityType = type;
        q.entityId = id;
        q.op = "upsert";
        q.createdAt = System.currentTimeMillis();
        AppDb.get(c).syncQueueDao().insert(q);
        SyncWorker.enqueueNow(c);
    }

    /** Wipes everything that belongs to one user from the device. */
    public static void wipeLocal(Context c, String uid) {
        AppDb db = AppDb.get(c);
        Alarms.cancelAll(c);
        db.reminderDao().deleteAll();
        db.taskDao().wipe(uid);
        db.categoryDao().wipe(uid);
        db.syncQueueDao().clear();
    }
}
