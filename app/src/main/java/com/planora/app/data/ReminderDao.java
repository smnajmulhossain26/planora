package com.planora.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReminderDao {
    @Insert
    long insert(ReminderEntity r);

    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    List<ReminderEntity> forTask(String taskId);

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    void deleteForTask(String taskId);

    @Query("SELECT * FROM reminders WHERE triggerAt > :now")
    List<ReminderEntity> future(long now);

    @Query("SELECT * FROM reminders")
    List<ReminderEntity> all();

    @Query("DELETE FROM reminders")
    void deleteAll();
}
