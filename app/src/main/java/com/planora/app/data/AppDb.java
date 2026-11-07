package com.planora.app.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {TaskEntity.class, CategoryEntity.class, ReminderEntity.class,
        SyncQueueEntity.class}, version = 1, exportSchema = false)
public abstract class AppDb extends RoomDatabase {
    private static volatile AppDb INSTANCE;

    public abstract TaskDao taskDao();
    public abstract CategoryDao categoryDao();
    public abstract ReminderDao reminderDao();
    public abstract SyncQueueDao syncQueueDao();

    public static AppDb get(Context c) {
        if (INSTANCE == null) {
            synchronized (AppDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(c.getApplicationContext(), AppDb.class, "planora.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
