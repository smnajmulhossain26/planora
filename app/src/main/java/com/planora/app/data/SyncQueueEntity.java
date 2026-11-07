package com.planora.app.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** One pending change that still has to be pushed to Firestore. */
@Entity(tableName = "sync_queue",
        indices = {@Index(value = {"entityType", "entityId"}, unique = true)})
public class SyncQueueEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String entityType; // "task" | "category"
    public String entityId;
    public String op;         // "upsert" (deletes are soft: isDeleted = true)
    public long createdAt;
}
