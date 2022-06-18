package com.orbital.scribex;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private ScribexUser appUser;

    private FirebaseFirestore firestore;
    private StorageReference storageReference;

    private ImageView imgViewProfilePic;
    private EditText editTextUserName;
    private Button buttonApplyChanges;
    private Button buttonDeleteAccount;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        //init view elements
        this.imgViewProfilePic = findViewById(R.id.imgViewProfilePic);
        this.editTextUserName = findViewById(R.id.editTextUserName);
        this.buttonApplyChanges = findViewById(R.id.buttonApplyChanges);
        this.buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount);

        Intent intent = this.getIntent();
        appUser = (ScribexUser) intent.getSerializableExtra("user");

        //check current username from firebase
        firestore.collection("users")
                .document(appUser.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot doc = task.getResult();
                            if (doc.exists()) {
                                Map<String, Object> map = doc.getData();
                                if (map != null && map.containsKey("name")) {
                                    editTextUserName.setText(doc.getString("name"));
                                } else {
                                    editTextUserName.setText("");
                                    editTextUserName.setHint("input username");
                                }
                            } else {
                                Log.d(TAG, "no such user?? " + appUser.getUid());
                            }
                        } else {
                            Log.d(TAG, "get failed, " + task.getException());
                        }
                    }
                });

        //OnClickListeners
        buttonApplyChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newUsername = editTextUserName.getText().toString();
                updateProfile("name", newUsername);
                openPersonalMenuActivity();
            }
        });
    }

    private void updateProfile(String field, String input) {
        DocumentReference ref = firestore.collection("users").document(appUser.getUid());
        Map<String,String> data = new HashMap<String,String>();
        data.put(field, input);
        ref.set(data).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d(TAG, "updated " + field + ": " + input);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "error writing to profile", e);
            }
        });
    }

    private void openPersonalMenuActivity() {
        Intent personalMenuActivityIntent = new Intent(EditProfileActivity.this, PersonalMenuActivity.class);
        personalMenuActivityIntent.putExtra("user", appUser);
        startActivity(personalMenuActivityIntent);
    }
}