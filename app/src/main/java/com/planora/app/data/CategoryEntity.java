package com.planora.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "categories", indices = {@Index("userId")})
public class CategoryEntity {
    @PrimaryKey @NonNull public String id = "";
    public String userId;
    public String name;
    public long createdAt;
    public long updatedAt;
    public boolean isDeleted;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("categoryName", name);
        m.put("createdAt", createdAt);
        m.put("updatedAt", updatedAt);
        m.put("isDeleted", isDeleted);
        return m;
    }

    public static CategoryEntity fromDoc(DocumentSnapshot d, String uid) {
        CategoryEntity c = new CategoryEntity();
        c.id = d.getId();
        c.userId = uid;
        c.name = d.getString("categoryName");
        c.createdAt = TaskEntity.l(d, "createdAt", 0);
        c.updatedAt = TaskEntity.l(d, "updatedAt", 0);
        Boolean del = d.getBoolean("isDeleted");
        c.isDeleted = del != null && del;
        return c;
    }
}
