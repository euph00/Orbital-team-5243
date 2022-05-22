package com.orbital.scribex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class PersonalMenuActivity extends AppCompatActivity {

    private recViewDocsAdapter adapter;

    private RecyclerView recViewDocs;
    private TextView txtUsername;
    private ImageView imgView_icon;
    private Button btnNewDoc;
    private Button btnEditProfile;
    private Button btnSignOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_menu);

        //init view elements
        recViewDocs = findViewById(R.id.recViewDocs);
        txtUsername = findViewById(R.id.textViewUsername);
        imgView_icon = findViewById(R.id.imgView_icon);
        btnNewDoc = findViewById(R.id.btnNewDoc);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnSignOut = findViewById(R.id.btnSignOut);

        //onClickListeners for buttons
        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        //retrieve id token
        Intent intent = this.getIntent();
        String idToken = intent.getStringExtra("idToken");
        Toast.makeText(this, idToken, Toast.LENGTH_LONG).show();
        txtUsername.setText(idToken);

        //recyclerview test code
        adapter = new recViewDocsAdapter(this);
        recViewDocs.setAdapter(adapter);
        recViewDocs.setLayoutManager(new LinearLayoutManager(this));

        List<Document> docs = new ArrayList<>();
        docs.add(new Document(1, "document 1", "datetime", "text for document 1", "text for document 1"));
        docs.add(new Document(2, "document 2", "datetime", "text for document 2", "text for document 2"));
        docs.add(new Document(3, "document 3", "datetime", "text for document 3", "text for document 3"));
        docs.add(new Document(4, "document 4", "datetime", "text for document 4", "text for document 4"));
        docs.add(new Document(5, "document 5", "datetime", "text for document 5", "text for document 5"));
        docs.add(new Document(6, "document 6", "datetime", "text for document 6", "text for document 6"));

        adapter.setDocs(docs);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent signOutIntent = new Intent(this, MainActivity.class);
        startActivity(signOutIntent);
    }
}