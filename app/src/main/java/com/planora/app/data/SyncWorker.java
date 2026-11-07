package com.planora.app.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.planora.app.util.Prefs;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Push local changes, then pull remote changes (last-write-wins by updatedAt). */
public class SyncWorker extends Worker {
    public static final String UNIQUE_NOW = "sync_now";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    private static Constraints net() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    public static void enqueueNow(Context c) {
        OneTimeWorkRequest r = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(net())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(c.getApplicationContext())
                .enqueueUniqueWork(UNIQUE_NOW, ExistingWorkPolicy.APPEND_OR_REPLACE, r);
    }

    public static void schedulePeriodic(Context c) {
        PeriodicWorkRequest r = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(net()).build();
        WorkManager.getInstance(c.getApplicationContext())
                .enqueueUniquePeriodicWork("sync_periodic", ExistingPeriodicWorkPolicy.KEEP, r);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        String uid = Repo.uid();
        Prefs prefs = new Prefs(c);
        if (uid == null) return Result.success();
        try {
            AppDb db = AppDb.get(c);
            com.google.firebase.firestore.DocumentReference user = Cloud.userDoc(uid);
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
                Cloud.await(user.set(Cloud.userMap(
                        com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser()),
                        SetOptions.merge()));
            }

            // ---- PUSH ----
            List<SyncQueueEntity> items = db.syncQueueDao().all();
            for (SyncQueueEntity q : items) {
                if ("task".equals(q.entityType)) {
                    TaskEntity t = db.taskDao().get(q.entityId);
                    if (t != null) {
                        Cloud.await(user.collection("tasks").document(t.id).set(t.toMap()));
                        db.taskDao().markSynced(t.id, t.updatedAt);
                    }
                } else if ("category".equals(q.entityType)) {
                    CategoryEntity cat = db.categoryDao().get(q.entityId);
                    if (cat != null) {
                        Cloud.await(user.collection("categories").document(cat.id).set(cat.toMap()));
                    }
                }
                db.syncQueueDao().delete(q.id); // if the row was replaced meanwhile, this is a no-op
            }

            // ---- PULL ----
            long since = prefs.lastSync(uid);
            long newest = since;

            QuerySnapshot cs = Cloud.await(user.collection("categories")
                    .whereGreaterThan("updatedAt", since).get());
            for (DocumentSnapshot d : cs.getDocuments()) {
                CategoryEntity r = CategoryEntity.fromDoc(d, uid);
                CategoryEntity l = db.categoryDao().get(r.id);
                if (l == null || r.updatedAt > l.updatedAt) db.categoryDao().insert(r);
                newest = Math.max(newest, r.updatedAt);
            }

            QuerySnapshot ts = Cloud.await(user.collection("tasks")
                    .whereGreaterThan("updatedAt", since).get());
            for (DocumentSnapshot d : ts.getDocuments()) {
                TaskEntity r = TaskEntity.fromDoc(d, uid);
                TaskEntity l = db.taskDao().get(r.id);
                if (l == null || r.updatedAt > l.updatedAt) {
                    r.syncStatus = 0;
                    if (l != null) {
                        r.attachmentUri = l.attachmentUri;
                        r.attachmentName = l.attachmentName;
                    }
                    db.taskDao().insert(r);
                    Repo.rebuildReminders(c, r);
                }
                newest = Math.max(newest, r.updatedAt);
            }
            prefs.setLastSync(uid, newest);
            prefs.setSyncFailed(false);
            return Result.success();
        } catch (Exception e) {
            prefs.setSyncFailed(true);
            return Result.retry();
        }
    }
}
