package com.planora.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CategoryEntity c);

    @Query("SELECT * FROM categories WHERE id = :id")
    CategoryEntity get(String id);

    @Query("SELECT * FROM categories WHERE userId = :uid AND isDeleted = 0 ORDER BY createdAt ASC")
    LiveData<List<CategoryEntity>> observe(String uid);

    @Query("SELECT COUNT(*) FROM categories WHERE userId = :uid")
    int countAll(String uid);

    @Query("DELETE FROM categories WHERE userId = :uid")
    void wipe(String uid);
}
