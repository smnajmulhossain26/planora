package com.planora.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.planora.app.R;
import com.planora.app.data.AppDb;
import com.planora.app.data.CategoryEntity;
import com.planora.app.data.SyncStatus;
import com.planora.app.data.SyncWorker;
import com.planora.app.data.TaskEntity;
import com.planora.app.databinding.FragmentHomeBinding;
import com.planora.app.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding b;
    private TaskAdapter high, today;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentHomeBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        String name = u.getDisplayName() == null || u.getDisplayName().isEmpty() ? "there" : u.getDisplayName().split(" ")[0];
        b.tvGreeting.setText("Hello, " + name + " 👋");
        b.tvDate.setText(Util.fmt(System.currentTimeMillis(), "EEEE, d MMMM yyyy"));
        if (u.getPhotoUrl() != null) Glide.with(this).load(u.getPhotoUrl()).circleCrop().into(b.ivAvatar);

        high = new TaskAdapter(TaskUi.listener(requireContext()));
        today = new TaskAdapter(TaskUi.listener(requireContext()));
        b.rvHigh.setLayoutManager(new LinearLayoutManager(getContext()));
        b.rvHigh.setAdapter(high);
        b.rvHigh.setNestedScrollingEnabled(false);
        b.rvToday.setLayoutManager(new LinearLayoutManager(getContext()));
        b.rvToday.setAdapter(today);
        b.rvToday.setNestedScrollingEnabled(false);

        AppDb db = AppDb.get(requireContext());
        db.categoryDao().observe(u.getUid()).observe(getViewLifecycleOwner(), list -> {
            Map<String, String> m = new HashMap<>();
            for (CategoryEntity c : list) m.put(c.id, c.name);
            high.setCategories(m);
            today.setCategories(m);
        });
        db.taskDao().observe(u.getUid()).observe(getViewLifecycleOwner(), this::bind);
        SyncStatus.observe(requireContext()).observe(getViewLifecycleOwner(), st -> {
            b.tvSync.setText(st.label);
            b.btnRetry.setVisibility(st.canRetry ? View.VISIBLE : View.GONE);
        });
        b.btnRetry.setOnClickListener(x -> SyncWorker.enqueueNow(requireContext()));
    }

    private void bind(List<TaskEntity> list) {
        long now = System.currentTimeMillis();
        long ds = Util.dayStart(now), de = Util.addDays(ds, 1);
        int total = list.size(), done = 0, upcoming = 0;
        List<TaskEntity> hi = new ArrayList<>(), td = new ArrayList<>();
        for (TaskEntity t : list) {
            if (t.status == 1) { done++; continue; }
            if (t.dueAt >= de) upcoming++;
            if (t.priority == 2) hi.add(t);
            if (t.dueAt > 0 && t.dueAt < de) td.add(t); // today + overdue
        }
        int pending = total - done;
        int pct = total == 0 ? 0 : done * 100 / total;
        b.tvTotal.setText(String.valueOf(total));
        b.tvDone.setText(String.valueOf(done));
        b.tvPending.setText(String.valueOf(pending));
        b.tvUpcoming.setText(String.valueOf(upcoming));
        b.tvHigh.setText(String.valueOf(hi.size()));
        b.progressRing.setProgress(pct);
        b.tvPercent.setText(pct + "%");
        b.tvSummary.setText(done + " of " + total + " tasks done");
        high.submitList(hi);
        today.submitList(td);
        b.tvEmptyHigh.setVisibility(hi.isEmpty() ? View.VISIBLE : View.GONE);
        b.tvEmptyToday.setVisibility(td.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
