package com.orbital.scribex;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProfilePageActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    //user
    private ScribexUser appUser;
    private FirebaseUser user;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;

    //view
    private ImageView imgViewProfilePic;
    private EditText editTextUserName;
    private CustomAnimatedButton buttonApplyChanges;
    private CustomAnimatedButton buttonDeleteAccount;
    private CustomAnimatedButton btnSignOut;
    private TextView textViewDelAccWarn;

    // animations
    private Animation scaleDown;
    private Animation scaleUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_profile_page);

        //user specific elements
        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        user = FirebaseAuth.getInstance().getCurrentUser();
        Supplier<String> userNameSupplier = () -> editTextUserName.getText().toString();

        //init anim elements
        scaleUp = AnimationUtils.loadAnimation(this,R.anim.scaleup);
        scaleDown = AnimationUtils.loadAnimation(this,R.anim.scaledown);


        //init view elements
        this.imgViewProfilePic = findViewById(R.id.imgViewProfilePic);
        this.editTextUserName = findViewById(R.id.editTextUserName);
        this.buttonApplyChanges = findViewById(R.id.buttonApplyChanges);
        this.buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount);
        this.btnSignOut = findViewById(R.id.btnSignOut);
        this.textViewDelAccWarn = findViewById(R.id.textViewDelAcctWarn);
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            Uri url = Uri.parse(acct.getPhotoUrl().toString().replace("s96-c", "s400-c"));
            Picasso.with(this).load(url).into(imgViewProfilePic);
        } else {
            Log.d(TAG, "Google account was null");
        }

        //retrieve ScribexUser
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
        btnSignOut.setAction(this::signOut);

        buttonApplyChanges.setAction(() -> updateProfileName(userNameSupplier));

        buttonDeleteAccount.setAction(this::deleteAccount);
    }

    private void confirmDeleteAccount() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Confirm account deletion");
        alertDialogBuilder.setMessage("You will lose all your documents stored in the app. This action is irreversible.");
        alertDialogBuilder.setPositiveButton("Confirm Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteAccount();
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

    /**
     * Updates the profile username with user's input, syncs to firebase
     * @param supp username supplier that evaluates the value of edittextusername lazily
     */
    private void updateProfileName(Supplier<String> supp) {
        String input = supp.get();
        UserProfileChangeRequest nameUpdate = new UserProfileChangeRequest.Builder()
                .setDisplayName(input)
                .build();
        user.updateProfile(nameUpdate)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "user name updated");
                    openDocumentMenuActivity();
                }
            }
        });
    }

    private void openDocumentMenuActivity() {
        Intent documentMenuActivityIntent = new Intent(ProfilePageActivity.this, DocumentMenuActivity.class);
        documentMenuActivityIntent.putExtra("user", appUser);
        startActivity(documentMenuActivityIntent);
    }

    /**
     * Attempts to delete firebase account authorised by google login.
     * If it fails by timeout of the sign in, user will be prompted to
     * log out and log back in to resolve the error.
     */
    private void deleteAccount() {
        if (user != null) {
            user.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Log.d(TAG, "User account deleted");
                    Toast.makeText(ProfilePageActivity.this, "Account deleted", Toast.LENGTH_LONG).show();
                    openMainActivity();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    showWarning();
                    Log.e(TAG, "failed delete account", e);
                }
            });
        }
    }

    /**
     * Prompts user to log out and log back in.
     */
    private void showWarning() {
        textViewDelAccWarn.setVisibility(View.VISIBLE);
    }

    private void openMainActivity() {
        Intent mainActivityIntent = new Intent(ProfilePageActivity.this, MainActivity.class);
        startActivity(mainActivityIntent);
    }
    // button animation
    private void btnAnimation(View v) {
        v.startAnimation(scaleDown);
        v.startAnimation(scaleUp);
    }

    /**
     * Signs the user out of Firebase.
     * Note: this does NOT sign the user out of Google Authentication.
     * Sign out from Google is only done after returning to MainActivity.
     */
    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent signOutIntent = new Intent(this, MainActivity.class);
        startActivity(signOutIntent);
    }
}