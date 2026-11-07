package com.planora.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SyncQueueEntity q);

    @Query("SELECT * FROM sync_queue ORDER BY id ASC")
    List<SyncQueueEntity> all();

    @Query("DELETE FROM sync_queue WHERE id = :id")
    void delete(long id);

    @Query("DELETE FROM sync_queue")
    void clear();

    @Query("SELECT COUNT(*) FROM sync_queue")
    LiveData<Integer> observeCount();
}
