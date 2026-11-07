package com.planora.app.locker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.planora.app.data.Repo;
import com.planora.app.databinding.DialogLockGuideBinding;
import com.planora.app.databinding.FragmentLockerBinding;
import com.planora.app.databinding.ItemAppBinding;
import com.planora.app.ui.MainActivity;
import com.planora.app.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** App Locker tab: password gate, app list, and a step-by-step permission guide. */
public class LockerFragment extends Fragment {
    static class AppItem { String pkg; String label; Drawable icon; }

    private FragmentLockerBinding b;
    private LockPrefs prefs;
    private final List<AppItem> all = new ArrayList<>();
    private final List<AppItem> shown = new ArrayList<>();
    private Adapter adapter;
    private boolean uiShown;
    private boolean enableAfterPermissions;
    private BottomSheetDialog guide;
    private DialogLockGuideBinding g;

    private final ActivityResultLauncher<Intent> setup = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> onAuth(r.getResultCode() == android.app.Activity.RESULT_OK));
    private final ActivityResultLauncher<Intent> gate = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> onAuth(r.getResultCode() == android.app.Activity.RESULT_OK));
    private final ActivityResultLauncher<Intent> change = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> { });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentLockerBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        prefs = new LockPrefs(requireContext());
        b.content.setVisibility(View.GONE);
        if (!prefs.hasPassword()) {
            Util.toast(requireContext(), "First, create an App Locker password.");
            setup.launch(new Intent(requireContext(), LockSetupActivity.class));
        } else if (LockerSession.authed) {
            showUi();
        } else {
            gate.launch(new Intent(requireContext(), LockScreenActivity.class).putExtra("gate", true));
        }
    }

    private void onAuth(boolean ok) {
        if (b == null) return;
        if (ok) { LockerSession.authed = true; showUi(); }
        else if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).selectHome();
    }

    private void showUi() {
        uiShown = true;
        b.content.setVisibility(View.VISIBLE);
        adapter = new Adapter();
        b.rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rv.setAdapter(adapter);
        b.btnGuide.setOnClickListener(x -> showGuide(false));
        b.btnChangePass.setOnClickListener(x -> change.launch(new Intent(requireContext(), LockSetupActivity.class)));
        b.switchLock.setOnCheckedChangeListener((cb, on) -> {
            if (!cb.isPressed()) return;
            if (on) {
                if (!LockService.hasPermissions(requireContext())) {
                    cb.setChecked(false);
                    showGuide(true);   // show how to allow each permission
                    return;
                }
                startLock();
            } else {
                prefs.setEnabled(false);
                requireContext().stopService(new Intent(requireContext(), LockService.class));
                refreshStatus();
            }
        });
        b.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable e) { }
        });
        loadApps();
        refreshStatus();
    }

    private void startLock() {
        prefs.setEnabled(true);
        ContextCompat.startForegroundService(requireContext(), new Intent(requireContext(), LockService.class));
        b.switchLock.setChecked(true);
        refreshStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!uiShown) return;
        refreshStatus();
        refreshGuide();
    }

    private void refreshStatus() {
        if (b == null) return;
        boolean u = LockService.hasUsageAccess(requireContext()), o = LockService.hasOverlay(requireContext());
        b.tvUsage.setText("Usage Access: " + (u ? "Allowed ✓" : "Not allowed ✗"));
        b.tvOverlay.setText("Display over other apps: " + (o ? "Allowed ✓" : "Not allowed ✗"));
        boolean on = prefs.enabled() && u && o;
        if (on && !LockService.isRunning()) {
            // service was killed by the phone (battery saver etc.) -> start it again
            ContextCompat.startForegroundService(requireContext(), new Intent(requireContext(), LockService.class));
        }
        b.switchLock.setChecked(on);
        b.tvState.setText(on ? "Active – " + prefs.locked().size() + " app(s) locked" : "Turned off");
    }

    // ---------- Permission guide ----------
    private void showGuide(boolean fromSwitch) {
        enableAfterPermissions = fromSwitch;
        g = DialogLockGuideBinding.inflate(getLayoutInflater());
        guide = new BottomSheetDialog(requireContext());
        guide.setContentView(g.getRoot());
        guide.getBehavior().setSkipCollapsed(true);
        guide.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        g.btnG1.setOnClickListener(x -> openSafely(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        g.btnG2.setOnClickListener(x -> openSafely(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + requireContext().getPackageName()))));
        g.btnG3.setOnClickListener(x -> openSafely(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        g.btnTipAppInfo.setOnClickListener(x -> openSafely(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + requireContext().getPackageName()))));
        g.btnGDone.setOnClickListener(x -> guide.dismiss());
        guide.setOnDismissListener(d -> { guide = null; g = null; });
        refreshGuide();
        guide.show();
    }

    private void openSafely(Intent i) {
        try { startActivity(i); }
        catch (Exception e) { Util.toast(requireContext(), "Could not open settings. Open Settings → Apps → Planora manually."); }
    }

    private void refreshGuide() {
        if (g == null || guide == null) return;
        boolean u = LockService.hasUsageAccess(requireContext()), o = LockService.hasOverlay(requireContext());
        mark(g.tvG1Status, g.btnG1, u);
        mark(g.tvG2Status, g.btnG2, o);
        g.tvG3Status.setText("Recommended");
        if (u && o && enableAfterPermissions) {
            enableAfterPermissions = false;
            guide.dismiss();
            startLock();
            Util.toast(requireContext(), "All set! App Lock is now ON. Select the apps you want to lock.");
        }
    }

    private void mark(android.widget.TextView tv, View btn, boolean ok) {
        tv.setText(ok ? "✓ Allowed" : "Not allowed yet");
        tv.setTextColor(ContextCompat.getColor(requireContext(),
                ok ? com.planora.app.R.color.p_low : com.planora.app.R.color.p_high));
        btn.setVisibility(ok ? View.GONE : View.VISIBLE);
    }

    // ---------- App list ----------
    private void loadApps() {
        final android.content.Context ctx = requireContext().getApplicationContext();
        Repo.IO.execute(() -> {
            PackageManager pm = ctx.getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<AppItem> list = new ArrayList<>();
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(ctx.getPackageName())) continue;
                AppItem a = new AppItem();
                a.pkg = pkg;
                a.label = String.valueOf(ri.loadLabel(pm));
                a.icon = ri.loadIcon(pm);
                list.add(a);
            }
            Collections.sort(list, (x, y) -> x.label.compareToIgnoreCase(y.label));
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (b == null) return;
                all.clear();
                all.addAll(list);
                filter(b.etSearch.getText() == null ? "" : b.etSearch.getText().toString());
            });
        });
    }

    private void filter(String q) {
        shown.clear();
        String s = q.toLowerCase().trim();
        for (AppItem a : all) if (s.isEmpty() || a.label.toLowerCase().contains(s)) shown.add(a);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final ItemAppBinding ib;
            VH(ItemAppBinding ib) { super(ib.getRoot()); this.ib = ib; }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(ItemAppBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            AppItem a = shown.get(pos);
            h.ib.ivIcon.setImageDrawable(a.icon);
            h.ib.tvLabel.setText(a.label);
            h.ib.sw.setOnCheckedChangeListener(null);
            h.ib.sw.setChecked(prefs.locked().contains(a.pkg));
            h.ib.sw.setOnCheckedChangeListener((v, on) -> { prefs.setLocked(a.pkg, on); refreshStatus(); });
        }

        @Override public int getItemCount() { return shown.size(); }
    }
}
