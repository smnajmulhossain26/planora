package com.planora.app.ui;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.PopupMenu;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.planora.app.data.Repo;
import com.planora.app.data.TaskEntity;

public final class TaskUi {
    private TaskUi() {}

    public static TaskAdapter.Listener listener(Context c) {
        return new TaskAdapter.Listener() {
            @Override public void onOpen(TaskEntity t) {
                c.startActivity(new Intent(c, AddTaskActivity.class).putExtra("task_id", t.id));
            }
            @Override public void onToggle(TaskEntity t, boolean done) { Repo.setDone(c, t.id, done); }
            @Override public void onMenu(TaskEntity t, View anchor) {
                PopupMenu pm = new PopupMenu(c, anchor);
                pm.getMenu().add("Edit");
                pm.getMenu().add("Duplicate");
                pm.getMenu().add("Delete");
                pm.setOnMenuItemClickListener(i -> {
                    String s = String.valueOf(i.getTitle());
                    if (s.equals("Edit")) onOpen(t);
                    else if (s.equals("Duplicate")) Repo.duplicate(c, t);
                    else new MaterialAlertDialogBuilder(c).setTitle("Delete task?")
                            .setMessage(t.title)
                            .setPositiveButton("Delete", (d, w) -> Repo.deleteTask(c, t.id))
                            .setNegativeButton("Cancel", null).show();
                    return true;
                });
                pm.show();
            }
        };
    }
}
