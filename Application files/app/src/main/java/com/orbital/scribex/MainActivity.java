package com.orbital.scribex;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;

    private GoogleSignInClient mGoogleSignInClient;

    //ActivityResultLauncher is replacement for deprecated startActivityForResult method.
    //Custom callback is defined in the anon inner class, no longer uses old default callback.
    private ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {

            int rc = result.getResultCode();

            if (rc == Activity.RESULT_OK) { //google sign in completed
                Toast.makeText(MainActivity.this, "Google result OK", Toast.LENGTH_SHORT).show();
                //retrieve result of external activity (google one touch activity)
                Intent intent = result.getData();
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
                try {
                    //Google sign in was successful, authenticate with firebase
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    Toast.makeText(MainActivity.this, "Authenticate with firebase", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    //Google sign in failed
                    Toast.makeText(MainActivity.this, "Google sign in failed", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Google sign in failed", e);
                }
            } else if (rc == Activity.RESULT_CANCELED) { //google sign in aborted
                Toast.makeText(MainActivity.this, "Google Sign In cancelled.", Toast.LENGTH_LONG).show();
            } else { //all other cases, refer to error code on screen
                Toast.makeText(MainActivity.this, "Other error: " + rc, Toast.LENGTH_LONG).show();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //GSO object for configuring scope of requested info
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.web_client_id))
                .build();

        //configure sign in client based on scope requested in GSO
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        //initialise firebase authorisation instance
        mAuth = FirebaseAuth.getInstance();

        //OnClickListener for sign in button
        Button loginButton = findViewById(R.id.sign_in_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                resultLauncher.launch(new Intent(mGoogleSignInClient.getSignInIntent()));

            }
        });
    }

    //Custom onStart routine to check for existing credentials
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            //if signed out of firebase, sign out of google as well
            mGoogleSignInClient.signOut();
            Toast.makeText(this, "logged out", Toast.LENGTH_LONG).show();
        }

        updateUI(currentUser);
    }

    //verify Google idToken for the user obtained from resultLauncher's callback with firebase
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) { //firebase authorised
                            Toast.makeText(MainActivity.this, "Firebase authentication OK", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Toast.makeText(MainActivity.this, "Firebase sign in failed", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            updateUI(null);
                        }
                    }
                });
    }

    //if user not null, login
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(this, "success", Toast.LENGTH_LONG).show();
            Intent menuIntent = new Intent(this, PersonalMenuActivity.class);
            menuIntent.putExtra("idToken", user.getUid());
            startActivity(menuIntent);
        }
    }


}