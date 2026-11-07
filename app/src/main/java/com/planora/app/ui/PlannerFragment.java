package com.planora.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.planora.app.R;
import com.planora.app.databinding.FragmentPlannerBinding;

/** One bottom-nav tab that holds Tasks, Calendar and Statistics. */
public class PlannerFragment extends Fragment {
    private FragmentPlannerBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentPlannerBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        b.toggle.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            Fragment f = id == R.id.btnTabCalendar ? new CalendarFragment()
                    : id == R.id.btnTabStats ? new StatsFragment() : new TasksFragment();
            getChildFragmentManager().beginTransaction().replace(R.id.plannerContainer, f).commit();
        });
        b.toggle.check(R.id.btnTabList);
    }
}
