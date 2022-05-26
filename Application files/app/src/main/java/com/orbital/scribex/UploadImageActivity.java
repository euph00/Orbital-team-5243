package com.orbital.scribex;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UploadImageActivity extends AppCompatActivity {

    private static final String TAG = "UploadImageActivity";

    private FirebaseFirestore firestore;
    private StorageReference storageReference;

    private ScribexUser appUser;

    private List<Photo> photos;

    private String currImgPath;
    private Uri uri = null;

    private EditText editTextDocName;
    private Button btnTakePic;
    private Button btnUpload;
    private ImageView imageViewDoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);

        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        Intent intent = this.getIntent();
        appUser = (ScribexUser) intent.getSerializableExtra("user");

        photos = new ArrayList<>();

        currImgPath = "";

        editTextDocName = findViewById(R.id.editTextDocName);
        btnTakePic = findViewById(R.id.btnTakePic);
        btnUpload = findViewById(R.id.btnUpload);
        imageViewDoc = findViewById(R.id.imageViewDoc);

        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upload();
            }
        });


    }

    private void upload() {

        //Guard clause: case where there is nothing to upload
        if (photos.isEmpty()) {
            Toast.makeText(this, "Please take a picture first", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Photo photo : photos) {
            Uri locUri = photo.getLocalUri();
            StorageReference imageRef = storageReference.child(String.format("images/%s/%s", appUser.getUid(), locUri.getLastPathSegment()));
            UploadTask task = imageRef.putFile(locUri);
            task.addOnCompleteListener(this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        Task<Uri> downloadUrl = imageRef.getDownloadUrl();
                        downloadUrl.addOnSuccessListener(UploadImageActivity.this, new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri remoteUri) {
                                photo.setRemoteUri(remoteUri);
                                updatePhotoDatabase(photo);
                            }
                        });
                    } else {
                        Log.e(TAG, "upload failed");
                    }
                }
            });
        }
    }

    private void updatePhotoDatabase(Photo photo) {
        CollectionReference uploadCollection = firestore.collection("users").document(appUser.getUid()).collection("uploads");
        Task<DocumentReference> handle = uploadCollection.add(photo);
        handle.addOnCompleteListener(this, new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (task.isSuccessful()) {
                    photo.setId(task.getResult().getId());
                    firestore.collection("users").document(appUser.getUid()).collection("uploads").document(photo.getId()).set(photo);
                    Toast.makeText(UploadImageActivity.this, "Upload image success.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "error updating photo data");
                }
            }
        });
    }

    private void takePhoto() {
        boolean camPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED;
        boolean storePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;

        if (camPermission && storePermission) {
            //permissions already granted
            invokeCamera();
        } else {
            //request permissions
            requestPermissionsLauncher.launch(new String[]
                    {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA});
        }

    }

    private ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    if (result.containsValue(false)) {
                        Toast.makeText(UploadImageActivity.this, "Unable to open camera, please grant permissions", Toast.LENGTH_SHORT).show();
                    } else {
                        invokeCamera();
                    }
                }
            });

    private void invokeCamera() {
        File file = createImageFile();
        try {
            uri = FileProvider.getUriForFile(this, "com.orbital.scribex.fileprovider", file);
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getLocalizedMessage());
        }
        getCameraImage.launch(uri);
    }

    private ActivityResultLauncher<Uri> getCameraImage = registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (result) {
                imageViewDoc.setImageURI(uri); //TODO: figure out why there is whitespace above and below image in the scrollview
                Log.i(TAG, "Image saved to: " + uri);
                Photo photo = new Photo(uri, null, new Date(), null);
                photos.clear();//TODO: photos is cleared every time because we are only doing 1 photo each time for MS1. Reimplement for later.
                photos.add(photo);
            } else {
                Log.e(TAG, "Image not saved. " + uri);
            }
        }
    });

    private File createImageFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File result = File.createTempFile(String.format("Specimen_%s", timestamp), ".jpg", imageDir);
            currImgPath = result.getAbsolutePath();
            return result;
        } catch (IOException e) {
            Toast.makeText(this, "IOexception: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        return null;
    }
}