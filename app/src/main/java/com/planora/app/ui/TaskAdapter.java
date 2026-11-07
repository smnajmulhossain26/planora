package com.planora.app.ui;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.planora.app.R;
import com.planora.app.data.TaskEntity;
import com.planora.app.databinding.ItemTaskBinding;
import com.planora.app.util.Util;

import java.util.HashMap;
import java.util.Map;

public class TaskAdapter extends ListAdapter<TaskEntity, TaskAdapter.VH> {
    public interface Listener {
        void onOpen(TaskEntity t);
        void onToggle(TaskEntity t, boolean done);
        void onMenu(TaskEntity t, View anchor);
    }

    private static final DiffUtil.ItemCallback<TaskEntity> DIFF = new DiffUtil.ItemCallback<TaskEntity>() {
        @Override public boolean areItemsTheSame(@NonNull TaskEntity a, @NonNull TaskEntity b) { return a.id.equals(b.id); }
        @Override public boolean areContentsTheSame(@NonNull TaskEntity a, @NonNull TaskEntity b) {
            return a.updatedAt == b.updatedAt && a.status == b.status;
        }
    };

    private final Listener l;
    private Map<String, String> cats = new HashMap<>();
    private boolean showCompletedDate;

    public TaskAdapter(Listener l) { super(DIFF); this.l = l; }

    public TaskAdapter(Listener l, boolean showCompletedDate) { this(l); this.showCompletedDate = showCompletedDate; }

    public void setCategories(Map<String, String> m) { cats = m; notifyDataSetChanged(); }

    public static int priorityColor(android.content.Context c, int p) {
        return ContextCompat.getColor(c, p == 2 ? R.color.p_high : p == 0 ? R.color.p_low : R.color.p_med);
    }

    class VH extends RecyclerView.ViewHolder {
        final ItemTaskBinding b;
        VH(ItemTaskBinding b) { super(b.getRoot()); this.b = b; }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(ItemTaskBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        TaskEntity t = getItem(pos);
        ItemTaskBinding b = h.b;
        int pc = priorityColor(b.getRoot().getContext(), t.priority);
        b.stripe.setBackgroundColor(pc);
        b.flag.setColorFilter(pc);
        b.title.setText(t.title);
        boolean done = t.status == 1;
        b.title.setPaintFlags(done ? b.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : b.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        b.title.setAlpha(done ? 0.55f : 1f);

        StringBuilder sb = new StringBuilder();
        String cn = t.categoryId == null ? null : cats.get(t.categoryId);
        if (cn != null) sb.append(cn);
        boolean overdue = !done && t.dueAt > 0 && t.dueAt < System.currentTimeMillis();
        if (showCompletedDate && done && t.completedAt > 0) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append("Completed ").append(Util.dateTime(t.completedAt));
        } else if (t.dueAt > 0) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(overdue ? "Overdue " : "").append(Util.dateTime(t.dueAt));
        }
        if (t.repeatType != 0) sb.append(" ↻");
        b.sub.setText(sb.length() == 0 ? "No due date" : sb.toString());
        b.sub.setTextColor(overdue ? Color.parseColor("#E5484D")
                : b.title.getTextColors().getDefaultColor());
        b.sub.setAlpha(overdue ? 1f : 0.65f);

        b.check.setOnCheckedChangeListener(null);
        b.check.setChecked(done);
        b.check.setOnCheckedChangeListener((v, c) -> l.onToggle(t, c));
        b.getRoot().setOnClickListener(v -> l.onOpen(t));
        b.menu.setOnClickListener(v -> l.onMenu(t, v));
    }
}
