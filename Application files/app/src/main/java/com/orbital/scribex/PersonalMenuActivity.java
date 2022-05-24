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
        docs.add(new Document(6, "document 6", "datetime", "On the other hand, we denounce with righteous indignation and dislike men who are so beguiled and demoralized by the charms of pleasure of the moment, so blinded by desire, that they cannot foresee the pain and trouble that are bound to ensue; and equal blame belongs to those who fail in their duty through weakness of will, which is the same as saying through shrinking from toil and pain. These cases are perfectly simple and easy to distinguish. In a free hour, when our power of choice is untrammelled and when nothing prevents our being able to do what we like best, every pleasure is to be welcomed and every pain avoided. But in certain circumstances and owing to the claims of duty or the obligations of business it will frequently occur that pleasures have to be repudiated and annoyances accepted. The wise man therefore always holds in these matters to this principle of selection: he rejects pleasures to secure other greater pleasures, or else he endures pains to avoid worse pains.", "text for document 6"));

        adapter.setDocs(docs);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent signOutIntent = new Intent(this, MainActivity.class);
        startActivity(signOutIntent);
    }
}