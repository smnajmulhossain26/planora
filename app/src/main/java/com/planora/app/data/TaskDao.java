package com.planora.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TaskEntity t);

    @Query("SELECT * FROM tasks WHERE id = :id")
    TaskEntity get(String id);

    @Query("SELECT * FROM tasks WHERE userId = :uid AND isDeleted = 0 ORDER BY status ASC, dueAt = 0, dueAt ASC")
    LiveData<List<TaskEntity>> observe(String uid);

    @Query("SELECT * FROM tasks WHERE userId = :uid AND isDeleted = 0 AND status = 0")
    List<TaskEntity> allPending(String uid);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :uid AND isDeleted = 0 AND status = 0 AND title = :title AND dueAt = :dueAt AND id != :exceptId")
    int countDuplicate(String uid, String title, long dueAt, String exceptId);

    @Query("UPDATE tasks SET syncStatus = 0 WHERE id = :id AND updatedAt = :updatedAt")
    void markSynced(String id, long updatedAt);

    @Query("DELETE FROM tasks WHERE userId = :uid")
    void wipe(String uid);
}
