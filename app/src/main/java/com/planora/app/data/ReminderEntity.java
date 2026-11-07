package com.planora.app.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders", indices = {@Index("taskId"), @Index("userId")})
public class ReminderEntity {
    @PrimaryKey(autoGenerate = true) public int id;
    public String userId;
    public String taskId;
    public long triggerAt;
}
