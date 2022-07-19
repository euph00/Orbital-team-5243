package com.orbital.scribex;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import de.hdodenhof.circleimageview.CircleImageView;

public class DocumentMenuActivity extends AppCompatActivity {

    private static final String TAG = "DocumentMenuActivity";

    //misc
    private RecViewDocsAdapter adapter;

    //user
    private ScribexUser appUser;
    private FirebaseUser user;
    private FirebaseFirestore firestore;
    private FirebaseStorage firebaseStorage;

    //view
    private RecyclerView recViewDocs;
    private Button btnNewDoc;
    private Button btnEditProfile;
    private CircleImageView profileImage;

    private Animation scaleDown;
    private Animation scaleUp;

    private Boolean notNow = false;
    private Rect rect;    // Variable rect to hold the bounds of the view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_document_menu);
        GestureDetector gestureDetector = new GestureDetector(this, new SingleTapConfirm());

        //user specific elements
        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        //init anim elements
        scaleUp = AnimationUtils.loadAnimation(this,R.anim.scaleup);
        scaleDown = AnimationUtils.loadAnimation(this,R.anim.scaledown);

        //init view elements
        recViewDocs = findViewById(R.id.recViewDocs);
        btnNewDoc = findViewById(R.id.btnNewDoc);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        profileImage = findViewById(R.id.profileimage);
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            Picasso.with(this).load(acct.getPhotoUrl()).into(profileImage); //set profile pic icon
            Log.d(TAG, "done setting profile pic");
        } else {
            Log.d(TAG, "Account was null");
        }


//        btnEditProfile.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction()==MotionEvent.ACTION_DOWN){
//                    profileImage.startAnimation(scaleDown);
//                }else if (event.getAction()==MotionEvent.Ac){
//                    profileImage.startAnimation(scaleUp);
//                    openProfilePageActivity();
//                }
//                return true;
//            }
//        });


        btnEditProfile.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                    profileImage.startAnimation(scaleDown);
                    btnEditProfile.setPressed(true);
                    rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                }
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    if (!notNow) {
                        profileImage.startAnimation(scaleUp);
                        openProfilePageActivity();
                        btnEditProfile.setPressed(false);
                    } else //button press canceled
                        notNow = false;
                }
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
                    if (!notNow)
                        if (!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                            // finger moved out of bounds, return button image to original
                            profileImage.startAnimation(scaleUp);
                            btnEditProfile.setPressed(false);
                            notNow = true; //cancel button press the next time
                        }
                }

                return true;
            }
            });

//        onClickListeners for buttons
        btnNewDoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAnimation(btnNewDoc);
                openTranscribeActivity();
            }
        });

//        btnEditProfile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                btnAnimation(profileImage);
//                openProfilePageActivity();
//            }
//        });

        //retrieve ScribexUser
        Intent intent = this.getIntent();
        appUser = (ScribexUser) intent.getSerializableExtra("user");

        //recyclerview code, updates the List docs in realtime from firestore
        adapter = new RecViewDocsAdapter(this);
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

    /**
     * Fetches content of relevant txt file from firebase and populates metadata
     * @param doc   as provided by the QuerySnapshot value triggered on event
     * @param docs  List of documents to be displayed in the recyclerview
     */
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
                    Document document = new Document(name, name, null, readFile(localFile));
                    document.setOwner(appUser);
                    docs.add(document);
                    Log.d(TAG, String.valueOf(docs.size()));
                    localFile.delete();
                    adapter.setDocs(docs);
                    setRemUri(document, docs);
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

    /**
     * Populates the remote URI metafield of document object
     * @param document  relevant Document object to be updated
     * @param docs  List of documents to be displayed in recyclerview
     */
    private void setRemUri(Document document, List<Document> docs) {
        firestore.collection("users")
                .document(appUser.getUid())
                .collection("uploads")
                .document(document.getName())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot dcs = task.getResult();
                            String urlString = dcs.getString("remoteUri");
                            if (urlString != null) {
                                Uri url = Uri.parse(urlString);
                                document.setUrl(url.toString());
                                adapter.setDocs(docs);
                            }

                        } else {
                            Log.e(TAG, "get url failed", task.getException());
                        }
                    }
                });
    }

    /**
     * Reads txt file into a String
     * @param file  File object, the txt file to be read
     * @return  String of the file contents
     */
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

    // button animation
    private void btnAnimation(View v) {
        v.startAnimation(scaleDown);
        v.startAnimation(scaleUp);
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }


    private void openProfilePageActivity() {
        Intent editProfileIntent = new Intent(DocumentMenuActivity.this, ProfilePageActivity.class);
        editProfileIntent.putExtra("user", appUser);
        startActivity(editProfileIntent);
    }
}