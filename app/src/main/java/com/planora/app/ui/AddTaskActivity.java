package com.planora.app.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.database.Cursor;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.planora.app.R;
import com.planora.app.data.AppDb;
import com.planora.app.data.CategoryEntity;
import com.planora.app.data.Repo;
import com.planora.app.data.TaskEntity;
import com.planora.app.databinding.ActivityAddTaskBinding;
import com.planora.app.reminder.Alarms;
import com.planora.app.util.Prefs;
import com.planora.app.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

public class AddTaskActivity extends AppCompatActivity {
    private static final int[] PRESETS = {0, 5, 15, 30, 60};
    private ActivityAddTaskBinding b;
    private String uid;
    private TaskEntity editing;           // null => new task
    private Calendar due;                 // null => no due date
    private final TreeSet<Integer> offsets = new TreeSet<>();
    private final List<CategoryEntity> cats = new ArrayList<>();
    private String pendingCategoryId;
    private String attachUri, attachName;

    private final ActivityResultLauncher<String> notifLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) Util.toast(this, "Notifications are off. Reminders will be saved but cannot be shown.");
                exactCheckThenSave();
            });
    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (Exception ignored) { }
                attachUri = uri.toString();
                attachName = nameOf(uri);
                b.tvAttach.setText(attachName);
            });

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityAddTaskBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { finish(); return; }
        b.toolbar.setNavigationOnClickListener(v -> finish());

        b.spRepeat.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"No repeat", "Every day", "Every week", "Every month", "Every year", "Custom (every N days)"}));
        b.spRepeat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                b.tilEvery.setVisibility(pos == 5 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        AppDb.get(this).categoryDao().observe(uid).observe(this, list -> {
            cats.clear();
            cats.addAll(list);
            List<String> names = new ArrayList<>();
            for (CategoryEntity c : list) names.add(c.name);
            b.spCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
            if (pendingCategoryId != null) selectCategory(pendingCategoryId);
        });

        b.btnDate.setOnClickListener(v -> pickDate());
        b.btnTime.setOnClickListener(v -> pickTime());
        b.btnReminder.setOnClickListener(v -> pickReminders());
        b.btnAttach.setOnClickListener(v -> filePicker.launch(new String[]{"*/*"})); // Storage Access Framework, no storage permission
        b.btnSave.setOnClickListener(v -> onSave());

        String id = getIntent().getStringExtra("task_id");
        if (id != null) {
            b.toolbar.setTitle("Edit task");
            Repo.IO.execute(() -> {
                TaskEntity t = AppDb.get(this).taskDao().get(id);
                runOnUiThread(() -> { if (t == null) finish(); else populate(t); });
            });
        } else {
            b.chipMed.setChecked(true);
        }
    }

    private void populate(TaskEntity t) {
        editing = t;
        b.etTitle.setText(t.title);
        b.etDesc.setText(t.description);
        if (t.dueAt > 0) { due = Calendar.getInstance(); due.setTimeInMillis(t.dueAt); }
        refreshDue();
        (t.priority == 2 ? b.chipHigh : t.priority == 0 ? b.chipLow : b.chipMed).setChecked(true);
        pendingCategoryId = t.categoryId;
        selectCategory(t.categoryId);
        offsets.clear();
        if (t.reminderOffsets != null && !t.reminderOffsets.isEmpty())
            for (String x : t.reminderOffsets.split(",")) try { offsets.add(Integer.parseInt(x.trim())); } catch (Exception ignored) { }
        refreshReminders();
        b.spRepeat.setSelection(t.repeatType);
        if (t.repeatType == 5) b.etEvery.setText(String.valueOf(t.repeatEvery));
        attachUri = t.attachmentUri;
        attachName = t.attachmentName;
        if (attachName != null) b.tvAttach.setText(attachName);
    }

    private void selectCategory(String id) {
        for (int i = 0; i < cats.size(); i++) if (cats.get(i).id.equals(id)) { b.spCategory.setSelection(i); return; }
    }

    private void pickDate() {
        Calendar c = due != null ? due : Calendar.getInstance();
        new DatePickerDialog(this, (dp, y, m, d) -> {
            if (due == null) { due = Calendar.getInstance(); due.set(Calendar.HOUR_OF_DAY, 9); due.set(Calendar.MINUTE, 0); }
            due.set(y, m, d);
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);
            refreshDue();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime() {
        Calendar c = due != null ? due : Calendar.getInstance();
        new TimePickerDialog(this, (tp, h, m) -> {
            if (due == null) due = Calendar.getInstance();
            due.set(Calendar.HOUR_OF_DAY, h);
            due.set(Calendar.MINUTE, m);
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);
            refreshDue();
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
    }

    private void refreshDue() {
        b.btnDate.setText(due == null ? "Set date" : Util.date(due.getTimeInMillis()));
        b.btnTime.setText(due == null ? "Set time" : Util.time(due.getTimeInMillis()));
    }

    private void pickReminders() {
        List<Integer> options = new ArrayList<>();
        for (int p : PRESETS) options.add(p);
        for (int o : offsets) if (!options.contains(o)) options.add(o);
        String[] labels = new String[options.size()];
        boolean[] checked = new boolean[options.size()];
        for (int i = 0; i < labels.length; i++) { labels[i] = Util.offsetLabel(options.get(i)); checked[i] = offsets.contains(options.get(i)); }
        new MaterialAlertDialogBuilder(this).setTitle("Reminders")
                .setMultiChoiceItems(labels, checked, (d, w, c) -> {
                    if (c) offsets.add(options.get(w)); else offsets.remove(options.get(w));
                })
                .setPositiveButton("Done", (d, w) -> refreshReminders())
                .setNeutralButton("Custom…", (d, w) -> Util.input(this, "Minutes before", "e.g. 45",
                        "", android.text.InputType.TYPE_CLASS_NUMBER, txt -> {
                            try { offsets.add(Math.max(0, Integer.parseInt(txt))); } catch (Exception ignored) { }
                            refreshReminders();
                        }))
                .setNegativeButton("Cancel", null).show();
    }

    private void refreshReminders() {
        if (offsets.isEmpty()) { b.tvReminders.setText("No reminder"); return; }
        StringBuilder sb = new StringBuilder();
        for (int o : offsets) { if (sb.length() > 0) sb.append(", "); sb.append(Util.offsetLabel(o)); }
        b.tvReminders.setText(sb.toString());
    }

    private String nameOf(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) { }
        return "Attachment";
    }

    // ---------- save flow: validate -> permissions (explained) -> save ----------
    private void onSave() {
        String title = b.etTitle.getText() == null ? "" : b.etTitle.getText().toString().trim();
        b.tilTitle.setError(null);
        if (title.isEmpty()) { b.tilTitle.setError("Title is required"); return; }
        if (!offsets.isEmpty() && due == null) { Util.toast(this, "Set a date/time to use reminders."); return; }
        final long dueAt = due == null ? 0 : due.getTimeInMillis();
        final String exceptId = editing == null ? "" : editing.id;
        Repo.IO.execute(() -> {
            int dup = AppDb.get(this).taskDao().countDuplicate(uid, title, dueAt, exceptId);
            runOnUiThread(() -> {
                if (dup > 0) Util.toast(this, "A pending task with the same title and time already exists.");
                else if (offsets.isEmpty()) doSave();
                else notificationCheck();
            });
        });
    }

    private void notificationCheck() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            new MaterialAlertDialogBuilder(this).setTitle("Allow notifications?")
                    .setMessage("Planora needs notification permission to show your task reminders.")
                    .setPositiveButton("Continue", (d, w) -> {
                        new Prefs(this).setNotifAsked();
                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                    })
                    .setNegativeButton("Not now", (d, w) -> exactCheckThenSave()).show();
        } else exactCheckThenSave();
    }

    private void exactCheckThenSave() {
        if (!Alarms.canExact(this)) {
            new MaterialAlertDialogBuilder(this).setTitle("Exact alarms")
                    .setMessage("To fire reminders exactly on time, allow \"Alarms & reminders\" for Planora. Without it, reminders may be a little late.")
                    .setPositiveButton("Open settings", (d, w) -> {
                        try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()))); }
                        catch (Exception ignored) { }
                        doSave();
                    })
                    .setNegativeButton("Skip", (d, w) -> doSave()).show();
        } else doSave();
    }

    private void doSave() {
        TaskEntity t = editing != null ? editing : new TaskEntity();
        if (editing == null) { t.id = UUID.randomUUID().toString(); t.userId = uid; }
        t.title = b.etTitle.getText().toString().trim();
        t.description = b.etDesc.getText() == null ? "" : b.etDesc.getText().toString().trim();
        t.priority = b.chipHigh.isChecked() ? 2 : b.chipLow.isChecked() ? 0 : 1;
        int ci = b.spCategory.getSelectedItemPosition();
        t.categoryId = ci >= 0 && ci < cats.size() ? cats.get(ci).id : null;
        t.dueAt = due == null ? 0 : due.getTimeInMillis();
        StringBuilder sb = new StringBuilder();
        for (int o : offsets) { if (sb.length() > 0) sb.append(','); sb.append(o); }
        t.reminderOffsets = sb.toString();
        t.repeatType = b.spRepeat.getSelectedItemPosition();
        t.repeatEvery = 1;
        if (t.repeatType == 5) {
            try { t.repeatEvery = Math.max(1, Integer.parseInt(b.etEvery.getText().toString().trim())); } catch (Exception ignored) { }
        }
        t.attachmentUri = attachUri;
        t.attachmentName = attachName;
        Repo.saveTask(this, t);
        Util.toast(this, "Saved");
        finish();
    }
}
