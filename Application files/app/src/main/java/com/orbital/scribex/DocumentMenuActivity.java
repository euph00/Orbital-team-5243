package com.orbital.scribex;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DocumentMenuActivity extends AppCompatActivity {

    private static final String TAG = "DocumentMenuActivity";

    private recViewDocsAdapter adapter;

    private ScribexUser appUser;
    private FirebaseUser user;

    private RecyclerView recViewDocs;
//    private TextView txtUsername;
//    private ImageView imgView_icon;
    private Button btnNewDoc;
    private Button btnEditProfile;

    private FirebaseFirestore firestore;
    private FirebaseStorage firebaseStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_document_menu);

        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        //init view elements
        recViewDocs = findViewById(R.id.recViewDocs);
//        txtUsername = findViewById(R.id.textViewUsername);
//        imgView_icon = findViewById(R.id.imgView_icon);
        btnNewDoc = findViewById(R.id.btnNewDoc);
        btnEditProfile = findViewById(R.id.btnEditProfile);


        //onClickListeners for buttons
        btnNewDoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTranscribeActivity();
            }
        });

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openProfilePageActivity();
            }
        });

        //retrieve ScribexUser
        Intent intent = this.getIntent();
        appUser = (ScribexUser) intent.getSerializableExtra("user");
//        Toast.makeText(this, appUser.getUid(), Toast.LENGTH_LONG).show();

        //recyclerview code, updates the List docs in realtime from firestore
        adapter = new recViewDocsAdapter(this);
        recViewDocs.setAdapter(adapter);
        recViewDocs.setLayoutManager(new LinearLayoutManager(this));

        List<Document> docs = new ArrayList<>();
        firestore.collection("users")
                .document(appUser.getUid())
                .collection("transcribed")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.w(TAG, "Listen failed", error);
                            return;
                        }
                        docs.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            processQuery(doc, docs);
                        }
                        Log.d(TAG, "Done with docs, size is " + String.valueOf(docs.size()));

                        Log.d(TAG, "update docs success refresh");
                    }
                });
    }

    private void processQuery(QueryDocumentSnapshot doc, List<Document> docs) {
        String name = doc.getId();
        StorageReference ref = firebaseStorage
                .getReferenceFromUrl("gs://scribex-1653106340524.appspot.com")
                .child(String.format("/transcribed/%s/%s", appUser.getUid(), name+".txt"));
        try {
            final File localFile = File.createTempFile(name, "txt");
            ref.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    docs.add(new Document(name, name, null, readFile(localFile)));
                    Log.d(TAG, String.valueOf(docs.size()));
                    localFile.delete();
                    adapter.setDocs(docs);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            });
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private String readFile(File file) {
        try {
            Scanner sc = new Scanner(file);
            StringBuffer sb = new StringBuffer();
            while (sc.hasNextLine()) sb.append(sc.nextLine());
            return sb.toString();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getLocalizedMessage());
            return null;
        }
    }

    private void openTranscribeActivity() {
        Intent transcribeIntent = new Intent(DocumentMenuActivity.this, TranscribeActivity.class);
        transcribeIntent.putExtra("user", appUser);
        startActivity(transcribeIntent);
    }

    private void openProfilePageActivity() {
        Intent editProfileIntent = new Intent(DocumentMenuActivity.this, ProfilePageActivity.class);
        editProfileIntent.putExtra("user", appUser);
        startActivity(editProfileIntent);
    }
}