package com.planora.app.data;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.planora.app.util.Prefs;
import com.planora.app.util.Util;

import java.util.List;

/** Combines connectivity + queue size + worker state into one label for the UI. */
public final class SyncStatus {
    private SyncStatus() {}

    public static class State {
        public String label;
        public boolean canRetry;
    }

    public static LiveData<State> observe(Context ctx) {
        final Context c = ctx.getApplicationContext();
        final MediatorLiveData<State> m = new MediatorLiveData<>();
        final LiveData<Integer> cnt = AppDb.get(c).syncQueueDao().observeCount();
        final LiveData<List<WorkInfo>> work =
                WorkManager.getInstance(c).getWorkInfosForUniqueWorkLiveData(SyncWorker.UNIQUE_NOW);
        Runnable compute = () -> {
            Integer v = cnt.getValue();
            int n = v == null ? 0 : v;
            boolean running = false;
            List<WorkInfo> l = work.getValue();
            if (l != null) for (WorkInfo i : l) if (i.getState() == WorkInfo.State.RUNNING) running = true;
            boolean online = Util.online(c);
            boolean failed = new Prefs(c).syncFailed();
            State s = new State();
            if (running) s.label = "Syncing…";
            else if (!online) s.label = n > 0 ? "Offline • " + n + " pending sync" : "Offline • all data saved on device";
            else if (failed && n > 0) { s.label = "Sync failed • " + n + " pending"; s.canRetry = true; }
            else if (n > 0) { s.label = "Sync pending • " + n; s.canRetry = true; }
            else s.label = "Online • All data synced";
            m.setValue(s);
        };
        m.addSource(cnt, x -> compute.run());
        m.addSource(work, x -> compute.run());
        return m;
    }
}
