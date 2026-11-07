package com.planora.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.planora.app.data.AppDb;
import com.planora.app.data.Repo;
import com.planora.app.data.TaskEntity;
import com.planora.app.databinding.ActivityTaskReminderBinding;
import com.planora.app.reminder.Alarms;
import com.planora.app.reminder.ReminderSoundPlayer;
import com.planora.app.util.Util;

public class TaskReminderActivity extends AppCompatActivity {
    private ActivityTaskReminderBinding b;
    private String taskId;
    private TaskEntity task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityTaskReminderBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        taskId = getIntent().getStringExtra("task_id");
        if (taskId == null) {
            finish();
            return;
        }

        loadTask();

        b.btnComplete.setOnClickListener(v -> completeTask());
        b.btnDismiss.setOnClickListener(v -> dismissReminder());
        b.btnSnooze.setOnClickListener(v -> snoozeReminder());
    }

    private void loadTask() {
        Repo.IO.execute(() -> {
            task = AppDb.get(getApplicationContext()).taskDao().get(taskId);
            if (task == null) {
                runOnUiThread(this::finish);
                return;
            }
            runOnUiThread(() -> {
                b.tvTaskTitle.setText(task.title);
                if (task.description != null && !task.description.isEmpty()) {
                    b.tvTaskDesc.setVisibility(View.VISIBLE);
                    b.tvTaskDesc.setText(task.description);
                } else {
                    b.tvTaskDesc.setVisibility(View.GONE);
                }
                String timeStr = task.dueAt > 0 ? "সময়: " + Util.dateTime(task.dueAt) : "কোন নির্দিষ্ট সময় নেই";
                b.tvTaskTime.setText(timeStr);
                String pStr = task.priority == 2 ? "High Priority" : (task.priority == 0 ? "Low Priority" : "Medium Priority");
                b.tvTaskPriority.setText(pStr);
            });
        });
    }

    private void completeTask() {
        ReminderSoundPlayer.stopIfMatching(taskId);
        NotificationManagerCompat.from(this).cancel(taskId.hashCode());
        Alarms.cancelRepeat(this, taskId);
        Repo.setDone(this, taskId, true);
        Toast.makeText(this, "টাস্কটি সম্পূর্ণ করা হয়েছে!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void dismissReminder() {
        ReminderSoundPlayer.stopIfMatching(taskId);
        NotificationManagerCompat.from(this).cancel(taskId.hashCode());
        Alarms.cancelRepeat(this, taskId);
        Toast.makeText(this, "নোটিফিকেশন বন্ধ করা হলো।", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void snoozeReminder() {
        int selectedId = b.chipGroupSnooze.getCheckedChipId();
        long minutes = 15; // default 15 mins
        if (selectedId == b.chip5m.getId()) {
            minutes = 5;
        } else if (selectedId == b.chip15m.getId()) {
            minutes = 15;
        } else if (selectedId == b.chip30m.getId()) {
            minutes = 30;
        } else if (selectedId == b.chip1h.getId()) {
            minutes = 60;
        }

        long nextAt = System.currentTimeMillis() + (minutes * 60_000L);
        ReminderSoundPlayer.stopIfMatching(taskId);
        NotificationManagerCompat.from(this).cancel(taskId.hashCode());

        String title = task != null ? task.title : "Task reminder";
        Alarms.scheduleRepeat(this, 0, taskId, title, 1, nextAt);

        Toast.makeText(this, minutes + " মিনিট পর আবার মনে করিয়ে দেওয়া হবে।", Toast.LENGTH_SHORT).show();
        finish();
    }
}
