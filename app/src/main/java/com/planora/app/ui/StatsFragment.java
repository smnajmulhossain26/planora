package com.planora.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.planora.app.R;
import com.planora.app.data.AppDb;
import com.planora.app.data.CategoryEntity;
import com.planora.app.data.TaskEntity;
import com.planora.app.databinding.FragmentStatsBinding;
import com.planora.app.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsFragment extends Fragment {
    private FragmentStatsBinding b;
    private TaskAdapter history;
    private List<TaskEntity> all = new ArrayList<>();
    private int mode = 0; // 0 daily (7 days), 1 weekly (4 weeks), 2 monthly (6 months)

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentStatsBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        history = new TaskAdapter(TaskUi.listener(requireContext()), true);
        b.rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        b.rvHistory.setAdapter(history);
        b.rvHistory.setNestedScrollingEnabled(false);
        AppDb db = AppDb.get(requireContext());
        db.categoryDao().observe(uid).observe(getViewLifecycleOwner(), list -> {
            Map<String, String> m = new HashMap<>();
            for (CategoryEntity c : list) m.put(c.id, c.name);
            history.setCategories(m);
        });
        db.taskDao().observe(uid).observe(getViewLifecycleOwner(), list -> { all = list; refresh(); });
        b.toggle.check(R.id.btnD);
        b.toggle.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            mode = id == R.id.btnW ? 1 : id == R.id.btnM ? 2 : 0;
            refresh();
        });
    }

    private int countBetween(long from, long to) {
        int n = 0;
        for (TaskEntity t : all) if (t.status == 1 && t.completedAt >= from && t.completedAt < to) n++;
        return n;
    }

    private void refresh() {
        if (b == null) return;
        long now = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        long ds = Util.dayStart(now);
        c.setTimeInMillis(ds);
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        long weekStart = c.getTimeInMillis();
        c.setTimeInMillis(ds);
        c.set(Calendar.DAY_OF_MONTH, 1);
        long monthStart = c.getTimeInMillis();

        b.tvToday.setText(String.valueOf(countBetween(ds, Long.MAX_VALUE)));
        b.tvWeek.setText(String.valueOf(countBetween(weekStart, Long.MAX_VALUE)));
        b.tvMonth.setText(String.valueOf(countBetween(monthStart, Long.MAX_VALUE)));

        int done = 0;
        List<TaskEntity> doneList = new ArrayList<>();
        for (TaskEntity t : all) if (t.status == 1) { done++; doneList.add(t); }
        int pct = all.isEmpty() ? 0 : done * 100 / all.size();
        b.progressRing.setProgress(pct);
        b.tvPercent.setText(pct + "%");
        Collections.sort(doneList, (x, y) -> Long.compare(y.completedAt, x.completedAt));
        history.submitList(doneList);
        b.tvEmpty.setVisibility(doneList.isEmpty() ? View.VISIBLE : View.GONE);

        // chart buckets
        int n = mode == 0 ? 7 : mode == 1 ? 4 : 6;
        String[] labels = new String[n];
        int[] vals = new int[n];
        for (int i = 0; i < n; i++) {
            int back = n - 1 - i;
            long from, to;
            Calendar x = Calendar.getInstance();
            if (mode == 0) {
                x.setTimeInMillis(ds);
                x.add(Calendar.DAY_OF_YEAR, -back);
                from = x.getTimeInMillis();
                x.add(Calendar.DAY_OF_YEAR, 1);
                to = x.getTimeInMillis();
                labels[i] = Util.fmt(from, "EEE");
            } else if (mode == 1) {
                x.setTimeInMillis(weekStart);
                x.add(Calendar.WEEK_OF_YEAR, -back);
                from = x.getTimeInMillis();
                x.add(Calendar.WEEK_OF_YEAR, 1);
                to = x.getTimeInMillis();
                labels[i] = Util.fmt(from, "d MMM");
            } else {
                x.setTimeInMillis(monthStart);
                x.add(Calendar.MONTH, -back);
                from = x.getTimeInMillis();
                x.add(Calendar.MONTH, 1);
                to = x.getTimeInMillis();
                labels[i] = Util.fmt(from, "MMM");
            }
            vals[i] = countBetween(from, to);
        }
        b.chart.setData(labels, vals);
    }
}
