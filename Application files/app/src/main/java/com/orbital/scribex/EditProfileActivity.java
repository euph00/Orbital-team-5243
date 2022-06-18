package com.orbital.scribex;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private ScribexUser appUser;
    private FirebaseUser user;

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
        user = FirebaseAuth.getInstance().getCurrentUser();

        //init view elements
        this.imgViewProfilePic = findViewById(R.id.imgViewProfilePic);
        this.editTextUserName = findViewById(R.id.editTextUserName);
        this.buttonApplyChanges = findViewById(R.id.buttonApplyChanges);
        this.buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount);

        Intent intent = this.getIntent();
        appUser = (ScribexUser) intent.getSerializableExtra("user");

        //check and set current username from firebase
        if (user != null) {
            String name = user.getDisplayName();
            if (name != null) editTextUserName.setText(name);
            else {
                editTextUserName.setText("");
                editTextUserName.setHint("enter a username");
            }
        }

        //OnClickListeners
        buttonApplyChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newUsername = editTextUserName.getText().toString();
                updateProfileName(newUsername);
            }
        });

        buttonDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAccount();
            }
        });
    }

    private void updateProfileName(String input) {
        UserProfileChangeRequest nameUpdate = new UserProfileChangeRequest.Builder()
                .setDisplayName(input)
                .build();
        user.updateProfile(nameUpdate)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "user name updated");
                    openPersonalMenuActivity();
                }
            }
        });
    }

    private void openPersonalMenuActivity() {
        Intent personalMenuActivityIntent = new Intent(EditProfileActivity.this, PersonalMenuActivity.class);
        personalMenuActivityIntent.putExtra("user", appUser);
        startActivity(personalMenuActivityIntent);
    }

    private void deleteAccount() {
        if (user != null) {
            user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User account deleted");
                        Toast.makeText(EditProfileActivity.this, "Account deleted", Toast.LENGTH_LONG).show();
                        openMainActivity();
                    } else {
                        Log.e(TAG, "Failed to delete account", task.getException());
                    }
                }
            });
        }
    }

    private void openMainActivity() {
        Intent mainActivityIntent = new Intent(EditProfileActivity.this, MainActivity.class);
        startActivity(mainActivityIntent);
    }
}