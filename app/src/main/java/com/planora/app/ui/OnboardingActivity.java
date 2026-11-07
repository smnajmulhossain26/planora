package com.planora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.planora.app.R;
import com.planora.app.databinding.ActivityOnboardingBinding;
import com.planora.app.databinding.ItemOnboardBinding;
import com.planora.app.util.Prefs;
import com.planora.app.util.Util;

public class OnboardingActivity extends AppCompatActivity {
    private static final int[] ICONS = {R.drawable.ic_list, R.drawable.ic_cloud, R.drawable.ic_bell, R.drawable.ic_sync};
    private static final String[] TITLES = {"Manage tasks easily", "Works without internet",
            "Never miss a reminder", "Always in sync"};
    private static final String[] DESC = {
            "Plan your study, office and personal work in one beautiful place.",
            "Add, edit and finish tasks offline. Everything is saved on your phone first.",
            "Get timely reminders and alarms for your important tasks.",
            "When you are online, your data is backed up to the cloud automatically."};

    private ActivityOnboardingBinding b;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.pager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                ItemOnboardBinding ib = ItemOnboardBinding.inflate(LayoutInflater.from(p.getContext()), p, false);
                return new RecyclerView.ViewHolder(ib.getRoot()) { };
            }
            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int i) {
                ItemOnboardBinding ib = ItemOnboardBinding.bind(h.itemView);
                ib.ivImg.setImageResource(ICONS[i]);
                ib.tvTitle.setText(TITLES[i]);
                ib.tvDesc.setText(DESC[i]);
            }
            @Override public int getItemCount() { return TITLES.length; }
        });
        buildDots(0);
        b.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                buildDots(pos);
                b.btnNext.setText(pos == TITLES.length - 1 ? "Get Started" : "Next");
            }
        });
        b.btnNext.setOnClickListener(v -> {
            int p = b.pager.getCurrentItem();
            if (p < TITLES.length - 1) b.pager.setCurrentItem(p + 1);
            else finishOnboarding();
        });
        b.btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void buildDots(int active) {
        b.dots.removeAllViews();
        for (int i = 0; i < TITLES.length; i++) {
            ImageView d = new ImageView(this);
            d.setImageResource(i == active ? R.drawable.dot_on : R.drawable.dot_off);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Util.dp(this, 10), Util.dp(this, 10));
            lp.setMargins(Util.dp(this, 4), 0, Util.dp(this, 4), 0);
            b.dots.addView(d, lp);
        }
    }

    private void finishOnboarding() {
        new Prefs(this).setOnboardingDone();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
