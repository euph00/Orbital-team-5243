package com.orbital.scribex;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class DocumentViewActivity extends AppCompatActivity {

    private static final String TAG = "DocumentViewActivity";

    //misc
    private Document doc;

    //user
    private FirebaseUser user;
    private ScribexUser appUser;
    private FirebaseFirestore firestore;
    private FirebaseStorage firebaseStorage;

    //view
    private Button btnDelete;
    private TextView txtName;
    private TextView txtDoc;
    private Button btnCopy;
    private CircleImageView btnCopybg;
    private CircleImageView btnDeletebg;

    // animations
    private Animation scaleDown;
    private Animation scaleUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_document_view);

        //get Document from DocumentMenuActivity that called this activity
        Intent intent = this.getIntent();
        doc = (Document) intent.getSerializableExtra("Document");

        //user specific elements
        firestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        appUser = doc.getOwner();

        //init view elements
        btnDelete = findViewById(R.id.btnDelete);
        txtName = findViewById(R.id.txtName);
        txtDoc = findViewById(R.id.txtDoc);
        btnCopy = findViewById(R.id.btnCopy);
        txtName.setText(doc.getName());
        txtDoc.setText(doc.getText());
        btnCopybg = findViewById(R.id.btnCopybg);
        btnDeletebg = findViewById(R.id.btnDeletebg);


        // init anim elements
        scaleUp = AnimationUtils.loadAnimation(this,R.anim.scaleup);
        scaleDown = AnimationUtils.loadAnimation(this,R.anim.scaledown);


        //onClickListeners for buttons
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAnimation(btnDeletebg);
                confirmDeleteDocument();
            }
        });

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAnimation(btnCopybg);
                copyTextToClip();
            }
        });
    }

    /**
     * Copies document text to clipboard. Called by the onclick of the copy button.
     */
    private void copyTextToClip() {
        ClipboardManager cpm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("document text", doc.getText());
        cpm.setPrimaryClip(clip);
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    /**
     * Deletes references of this document from firestore and the actual image and
     * txt files from firebase cloud storage.
     */
    private void scrubFromFirebase() {
        deleteFromUploads();
        deleteFromTranscribed();
        deleteFromStorage();
    }

    /**
     * Deletes the txt file and the image file from cloud storage
     */
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

    /**
     * Deletes the reference of the image from firestore database
     */
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

    /**
     * Deletes the reference of the txt from firestore database
     */
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

    // button animation
    private void btnAnimation(View v) {
        v.startAnimation(scaleDown);
        v.startAnimation(scaleUp);
    }

    private void confirmDeleteDocument() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Confirm document deletion");
        alertDialogBuilder.setMessage("This document will be deleted. This action is irreversible.");
        alertDialogBuilder.setPositiveButton("Confirm Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                scrubFromFirebase();
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        alertDialogBuilder.create().show();
    }

    private void openDocumentMenuActivity() {
        Intent documentMenuIntent = new Intent(this, DocumentMenuActivity.class);
        documentMenuIntent.putExtra("user", appUser);
        startActivity(documentMenuIntent);
    }
}