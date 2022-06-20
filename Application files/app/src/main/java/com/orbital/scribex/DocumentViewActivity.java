package com.orbital.scribex;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class DocumentViewActivity extends AppCompatActivity {

    private Button btnDelete;

    private TextView txtName;
    private TextView txtDoc;

    private FirebaseUser user;
    private ScribexUser appUser;

    private Document doc;

    private FirebaseFirestore firestore;
    private FirebaseStorage firebaseStorage;

    private static final String TAG = "DocumentViewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_document_view);

        Intent intent = this.getIntent();
        doc = (Document) intent.getSerializableExtra("Document");

        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        appUser = doc.getOwner();

        //init view elements
        btnDelete = findViewById(R.id.btnDelete);
        txtName = findViewById(R.id.txtName);
        txtDoc = findViewById(R.id.txtDoc);

        txtName.setText(doc.getName());
        txtDoc.setText(doc.getText());

        //onClickListeners for buttons
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrubFromFirebase(doc);
            }
        });
    }

    private void scrubFromFirebase(Document doc) {
        //must get imageUrl before starting deletes, otherwise will race
        firestore.collection("users")
                .document(appUser.getUid())
                .collection("uploads")
                .document(doc.getName())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot dcs = task.getResult();
                            String imageUrl = dcs.getString("remoteUri");
                            Log.d(TAG, imageUrl);

                            //these 4 can happen in parallel
                            deleteFromUploads();
                            deleteFromTranscribed();
                            deleteFromStorage();

                        } else {
                            Log.e(TAG, "failed to get doc", task.getException());
                        }
                    }
                });

    }

    private void deleteFromStorage() {
        String name = doc.getId();
        StorageReference txtRef = firebaseStorage
                .getReferenceFromUrl("gs://scribex-1653106340524.appspot.com")
                .child(String.format("/transcribed/%s/%s", appUser.getUid(), name+".txt"));
        txtRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "deleted txt");
                } else {
                    Log.e(TAG, "failed delete txt", task.getException());
                }
            }
        });
        StorageReference imgRef = firebaseStorage
                .getReferenceFromUrl("gs://scribex-1653106340524.appspot.com")
                .child(String.format("/images/%s/%s", appUser.getUid(), name));
        imgRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "deleted img");
                } else {
                    Log.e(TAG, "failed delete img", task.getException());
                }
            }
        });
    }

    private void deleteFromUploads() {
        firestore.collection("users")
                .document(appUser.getUid())
                .collection("uploads")
                .document(doc.getName())
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "removed doc from uploads");
                        } else {
                            Log.e(TAG, "failed remove from uploads", task.getException());
                        }
                    }
                });
    }

    private void deleteFromTranscribed() {
        firestore.collection("users")
                .document(appUser.getUid())
                .collection("transcribed")
                .document(doc.getName())
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "removed doc from transcribed");
                            openDocumentMenuActivity();
                        } else {
                            Log.e(TAG, "failed remove from transcribed", task.getException());
                        }
                    }
                });
    }

    private void openDocumentMenuActivity() {
        Intent documentMenuIntent = new Intent(this, DocumentMenuActivity.class);
        documentMenuIntent.putExtra("user", appUser);
        startActivity(documentMenuIntent);
    }
}