package com.example.myapplication;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    ImageView imageView;
    Button uploadbutton;

    FirebaseStorage storage;
    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        uploadbutton = findViewById(R.id.uploadimage);
        imageView = findViewById((R.id.firebaseimage));

        storage = FirebaseStorage.getInstance();


//        run the below method on imageview click
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mGetContent.launch("image/*");
//                the image is selected successfully from the gallery
//                now we need to upload the image into firebase storage
            }
        });

        uploadbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                upload image on button click

                uploadImage();

            }
        });

    }

    private void uploadImage() {

        if (imageUri != null) {
            StorageReference reference = storage.getReference().child("images/" + UUID.randomUUID().toString());
//            images will be stored in teh images folder in firebase storage

            reference.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {

//                        successful image upload
                        Toast.makeText(MainActivity.this,"Image Uploaded Successfully", Toast.LENGTH_SHORT).show();

                    } else {
//                        failed upload
                        Toast.makeText(MainActivity.this, task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }
    }

    //    start an activity for result (new method)
    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {

//                    this result is the result of uri
                    if (result != null){
                        imageView.setImageURI(result);
//                        result will be set to the global variable imageUri
                        imageUri = result;
                    }
                }
            });

}
