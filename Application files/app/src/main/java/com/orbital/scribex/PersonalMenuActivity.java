package com.orbital.scribex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class PersonalMenu extends AppCompatActivity {

    private RecyclerView recViewDocs;
    private recViewDocsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_menu);

        adapter = new recViewDocsAdapter(this);
        recViewDocs = findViewById(R.id.recViewDocs);

        recViewDocs.setAdapter(adapter);
        recViewDocs.setLayoutManager(new LinearLayoutManager(this));

        List<Document> docs = new ArrayList<>();
        docs.add(new Document(1, "document 1", "datetime", "text for document 1", "text for document 1"));
        docs.add(new Document(2, "document 2", "datetime", "text for document 2", "text for document 2"));
        docs.add(new Document(3, "document 3", "datetime", "text for document 3", "text for document 3"));
        docs.add(new Document(4, "document 4", "datetime", "text for document 4", "text for document 4"));
        docs.add(new Document(5, "document 5", "datetime", "text for document 5", "text for document 5"));
        docs.add(new Document(6, "document 6", "datetime", "text for document 6", "text for document 6"));

    }
}