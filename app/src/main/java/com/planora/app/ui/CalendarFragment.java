package com.planora.app.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.planora.app.R;
import com.planora.app.data.AppDb;
import com.planora.app.data.CategoryEntity;
import com.planora.app.data.TaskEntity;
import com.planora.app.databinding.FragmentCalendarBinding;
import com.planora.app.databinding.ItemDayBinding;
import com.planora.app.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Offline month calendar with task indicators + Day / Week / Month task lists. */
public class CalendarFragment extends Fragment {
    private FragmentCalendarBinding b;
    private TaskAdapter adapter;
    private DayAdapter days;
    private List<TaskEntity> all = new ArrayList<>();
    private final Calendar month = Calendar.getInstance();
    private long selected = Util.dayStart(System.currentTimeMillis());
    private int mode = 0; // 0 day, 1 week, 2 month

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentCalendarBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        month.set(Calendar.DAY_OF_MONTH, 1);
        adapter = new TaskAdapter(TaskUi.listener(requireContext()));
        b.rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        b.rvTasks.setAdapter(adapter);
        b.rvTasks.setNestedScrollingEnabled(false);
        days = new DayAdapter();
        b.rvDays.setLayoutManager(new GridLayoutManager(getContext(), 7));
        b.rvDays.setAdapter(days);

        AppDb db = AppDb.get(requireContext());
        db.categoryDao().observe(uid).observe(getViewLifecycleOwner(), list -> {
            Map<String, String> m = new HashMap<>();
            for (CategoryEntity c : list) m.put(c.id, c.name);
            adapter.setCategories(m);
        });
        db.taskDao().observe(uid).observe(getViewLifecycleOwner(), list -> { all = list; refresh(); });

        b.btnPrev.setOnClickListener(x -> { month.add(Calendar.MONTH, -1); refresh(); });
        b.btnNext.setOnClickListener(x -> { month.add(Calendar.MONTH, 1); refresh(); });
        b.toggle.check(R.id.btnDay);
        b.toggle.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            mode = id == R.id.btnWeek ? 1 : id == R.id.btnMonth ? 2 : 0;
            refresh();
        });
    }

    private void refresh() {
        if (b == null) return;
        b.tvMonth.setText(Util.fmt(month.getTimeInMillis(), "MMMM yyyy"));
        days.rebuild();

        long from, to;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selected);
        if (mode == 0) { from = selected; to = Util.addDays(selected, 1); }
        else if (mode == 1) {
            c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
            from = Util.dayStart(c.getTimeInMillis());
            to = Util.addDays(from, 7);
        } else {
            c.set(Calendar.DAY_OF_MONTH, 1);
            from = Util.dayStart(c.getTimeInMillis());
            c.add(Calendar.MONTH, 1);
            to = c.getTimeInMillis();
        }
        List<TaskEntity> out = new ArrayList<>();
        for (TaskEntity t : all) if (t.dueAt >= from && t.dueAt < to) out.add(t);
        adapter.submitList(out);
        b.tvEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    class DayAdapter extends RecyclerView.Adapter<DayAdapter.VH> {
        private final List<Long> cells = new ArrayList<>();
        private Set<Long> marks = new HashSet<>();

        void rebuild() {
            cells.clear();
            marks = new HashSet<>();
            for (TaskEntity t : all) if (t.dueAt > 0) marks.add(Util.dayStart(t.dueAt));
            Calendar c = (Calendar) month.clone();
            c.set(Calendar.DAY_OF_MONTH, 1);
            int offset = (c.get(Calendar.DAY_OF_WEEK) - 1); // Sunday-first grid
            for (int i = 0; i < offset; i++) cells.add(0L);
            int n = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int d = 1; d <= n; d++) {
                c.set(Calendar.DAY_OF_MONTH, d);
                cells.add(Util.dayStart(c.getTimeInMillis()));
            }
            notifyDataSetChanged();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemDayBinding ib;
            VH(ItemDayBinding ib) { super(ib.getRoot()); this.ib = ib; }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(ItemDayBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            long d = cells.get(pos);
            if (d == 0) {
                h.ib.tvDay.setText("");
                h.ib.dot.setVisibility(View.INVISIBLE);
                h.ib.getRoot().setOnClickListener(null);
                h.ib.tvDay.setBackground(null);
                return;
            }
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(d);
            h.ib.tvDay.setText(String.valueOf(c.get(Calendar.DAY_OF_MONTH)));
            boolean sel = d == selected;
            h.ib.tvDay.setBackgroundResource(sel ? R.drawable.bg_circle : 0);
            h.ib.tvDay.setTextColor(sel ? Color.WHITE : com.google.android.material.color.MaterialColors.getColor(h.ib.tvDay, com.google.android.material.R.attr.colorOnSurface));
            h.ib.dot.setVisibility(marks.contains(d) ? View.VISIBLE : View.INVISIBLE);
            h.ib.getRoot().setOnClickListener(x -> { selected = d; refresh(); });
        }

        @Override public int getItemCount() { return cells.size(); }
    }
}
