package com.planora.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "tasks", indices = {@Index("userId")})
public class TaskEntity {
    @PrimaryKey @NonNull public String id = "";
    public String userId;
    public String title;
    public String description;
    public String categoryId;
    public int priority = 1;          // 0 = Low, 1 = Medium, 2 = High
    public long dueAt;                // millis (date + time). 0 = no due date
    public String reminderOffsets = ""; // CSV of minutes before due, e.g. "0,15,60"
    public int status;                // 0 = Pending, 1 = Completed
    public long completedAt;
    public int repeatType;            // 0 none,1 day,2 week,3 month,4 year,5 custom (every N days)
    public int repeatEvery = 1;
    public String attachmentUri;      // local only
    public String attachmentName;     // local only
    public long createdAt;
    public long updatedAt;
    public boolean isDeleted;
    public int syncStatus;            // 0 synced, 1 pending

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("title", title);
        m.put("description", description);
        m.put("priority", priority);
        m.put("categoryId", categoryId);
        m.put("dueAt", dueAt);
        m.put("reminderOffsets", reminderOffsets);
        m.put("status", status);
        m.put("completedAt", completedAt);
        m.put("repeatType", repeatType);
        m.put("repeatEvery", repeatEvery);
        m.put("createdAt", createdAt);
        m.put("updatedAt", updatedAt);
        m.put("isDeleted", isDeleted);
        return m;
    }

    public static TaskEntity fromDoc(DocumentSnapshot d, String uid) {
        TaskEntity t = new TaskEntity();
        t.id = d.getId();
        t.userId = uid;
        t.title = d.getString("title");
        t.description = d.getString("description");
        t.categoryId = d.getString("categoryId");
        t.priority = (int) l(d, "priority", 1);
        t.dueAt = l(d, "dueAt", 0);
        String ro = d.getString("reminderOffsets");
        t.reminderOffsets = ro == null ? "" : ro;
        t.status = (int) l(d, "status", 0);
        t.completedAt = l(d, "completedAt", 0);
        t.repeatType = (int) l(d, "repeatType", 0);
        t.repeatEvery = (int) l(d, "repeatEvery", 1);
        t.createdAt = l(d, "createdAt", 0);
        t.updatedAt = l(d, "updatedAt", 0);
        Boolean del = d.getBoolean("isDeleted");
        t.isDeleted = del != null && del;
        return t;
    }

    static long l(DocumentSnapshot d, String k, long def) {
        Long v = d.getLong(k);
        return v == null ? def : v;
    }
}
