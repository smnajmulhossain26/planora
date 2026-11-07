package com.planora.app.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.planora.app.data.AppDb;
import com.planora.app.data.CategoryEntity;
import com.planora.app.data.Repo;
import com.planora.app.databinding.ActivityCategoriesBinding;
import com.planora.app.databinding.ItemCategoryBinding;
import com.planora.app.util.Util;

import java.util.ArrayList;
import java.util.List;

public class CategoriesActivity extends AppCompatActivity {
    private final List<CategoryEntity> items = new ArrayList<>();
    private RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;
    private String uid;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        ActivityCategoriesBinding b = ActivityCategoriesBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { finish(); return; }
        b.toolbar.setNavigationOnClickListener(v -> finish());
        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                return new RecyclerView.ViewHolder(ItemCategoryBinding.inflate(LayoutInflater.from(p.getContext()), p, false).getRoot()) { };
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int i) {
                ItemCategoryBinding ib = ItemCategoryBinding.bind(h.itemView);
                CategoryEntity c = items.get(i);
                ib.tvName.setText(c.name);
                ib.btnEdit.setOnClickListener(v -> Util.input(CategoriesActivity.this, "Edit category", "Name", c.name,
                        InputType.TYPE_CLASS_TEXT, n -> { if (!n.isEmpty()) Repo.saveCategory(CategoriesActivity.this, c.id, uid, n); }));
                ib.btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(CategoriesActivity.this)
                        .setTitle("Delete \"" + c.name + "\"?")
                        .setPositiveButton("Delete", (d, w) -> Repo.deleteCategory(CategoriesActivity.this, c.id))
                        .setNegativeButton("Cancel", null).show());
            }
            @Override public int getItemCount() { return items.size(); }
        };
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        AppDb.get(this).categoryDao().observe(uid).observe(this, list -> {
            items.clear();
            items.addAll(list);
            adapter.notifyDataSetChanged();
        });
        b.fab.setOnClickListener(v -> Util.input(this, "New category", "Name", "", InputType.TYPE_CLASS_TEXT,
                n -> { if (!n.isEmpty()) Repo.saveCategory(this, null, uid, n); }));
    }
}
