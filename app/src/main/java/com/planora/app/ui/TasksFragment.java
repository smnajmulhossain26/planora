package com.planora.app.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.planora.app.R;
import com.planora.app.data.AppDb;
import com.planora.app.data.CategoryEntity;
import com.planora.app.data.TaskEntity;
import com.planora.app.databinding.FragmentTasksBinding;
import com.planora.app.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TasksFragment extends Fragment {
    private FragmentTasksBinding b;
    private TaskAdapter adapter;
    private List<TaskEntity> all = new ArrayList<>();
    private final List<CategoryEntity> cats = new ArrayList<>();
    private int chip = R.id.chipAll;
    private String categoryId;
    private long dateFilter;
    private String query = "";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentTasksBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        adapter = new TaskAdapter(TaskUi.listener(requireContext()));
        b.rv.setLayoutManager(new LinearLayoutManager(getContext()));
        b.rv.setAdapter(adapter);

        AppDb db = AppDb.get(requireContext());
        db.categoryDao().observe(uid).observe(getViewLifecycleOwner(), list -> {
            cats.clear();
            cats.addAll(list);
            Map<String, String> m = new HashMap<>();
            List<String> names = new ArrayList<>();
            names.add("All categories");
            for (CategoryEntity c : list) { m.put(c.id, c.name); names.add(c.name); }
            adapter.setCategories(m);
            b.spCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, names));
        });
        db.taskDao().observe(uid).observe(getViewLifecycleOwner(), list -> { all = list; apply(); });

        b.spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                categoryId = pos == 0 || pos - 1 >= cats.size() ? null : cats.get(pos - 1).id;
                apply();
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
        b.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { query = s.toString().trim().toLowerCase(); apply(); }
            @Override public void afterTextChanged(Editable e) { }
        });
        b.chipGroup.setOnCheckedStateChangeListener((g, ids) -> {
            chip = ids.isEmpty() ? R.id.chipAll : ids.get(0);
            if (chip == R.id.chipDate) pickDate(); else { dateFilter = 0; apply(); }
        });
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
            Calendar x = Calendar.getInstance();
            x.set(y, m, d, 0, 0, 0);
            dateFilter = Util.dayStart(x.getTimeInMillis());
            b.chipDate.setText(Util.fmt(dateFilter, "d MMM"));
            apply();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void apply() {
        if (b == null) return;
        long ds = Util.dayStart(System.currentTimeMillis()), de = Util.addDays(ds, 1);
        List<TaskEntity> out = new ArrayList<>();
        for (TaskEntity t : all) {
            if (!query.isEmpty() && (t.title == null || !t.title.toLowerCase().contains(query))) continue;
            if (categoryId != null && !categoryId.equals(t.categoryId)) continue;
            boolean ok;
            if (chip == R.id.chipToday) ok = t.dueAt >= ds && t.dueAt < de;
            else if (chip == R.id.chipUpcoming) ok = t.status == 0 && t.dueAt >= de;
            else if (chip == R.id.chipCompleted) ok = t.status == 1;
            else if (chip == R.id.chipPending) ok = t.status == 0;
            else if (chip == R.id.chipHigh) ok = t.priority == 2;
            else if (chip == R.id.chipMedium) ok = t.priority == 1;
            else if (chip == R.id.chipLow) ok = t.priority == 0;
            else if (chip == R.id.chipDate) ok = dateFilter != 0 && t.dueAt >= dateFilter && t.dueAt < Util.addDays(dateFilter, 1);
            else ok = true;
            if (ok) out.add(t);
        }
        adapter.submitList(out);
        b.tvEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
