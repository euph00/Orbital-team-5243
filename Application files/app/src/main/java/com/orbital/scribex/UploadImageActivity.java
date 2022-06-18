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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private TextView textViewWarning;

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
        textViewWarning = findViewById(R.id.textViewWarning);

        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newDocName = editTextDocName.getText().toString();
                firestore.collection("users")
                        .document(appUser.getUid())
                        .collection("uploads")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    QuerySnapshot docs = task.getResult();
                                    for (QueryDocumentSnapshot doc : docs) {
                                        if (doc.getId().equals(newDocName)) {
                                            showWarningNameCollision();
                                            return; //TODO: implement name collision check, only begin upload process after no collision confirmed
                                        }
                                    }
                                    upload();
                                }
                            }
                        });
            }
        });
    }

    private void showWarningNameCollision() {
        textViewWarning.setVisibility(View.VISIBLE);
    }

    /**
     * Tries to upload photos in the Collection photos to firebase storage. This method DOES NOT update firestore itself.
     * Firestore updates are handled by UploadImageActivity::updatePhotoDatabase called in this method.
     */
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

    /**
     * This method is to be called by UploadImageActivity::upload. This handles updating of firestore database.
     * @param photo Photo object that was just uploaded to firebase storage. remoteUri field should have been populated with uri of photo on firebase storage.
     */
    private void updatePhotoDatabase(Photo photo) {
        CollectionReference uploadCollection = firestore.collection("users").document(appUser.getUid()).collection("uploads");
        Task<DocumentReference> handle = uploadCollection.add(photo);
        handle.addOnCompleteListener(this, new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (task.isSuccessful()) {
                    photo.setId(task.getResult().getId());
                    String name = editTextDocName.getText().toString();
                    photo.setName(name);
                    firestore.collection("users")
                            .document(appUser.getUid())
                            .collection("uploads")
                            .document(photo.getName())//TODO: replace document name with photo's name after implementing collision check
                            .set(photo);
                    firestore.collection("users")
                            .document(appUser.getUid())
                            .collection("uploads")
                            .document("QUEUE")
                            .update(photo.getName(), photo.getRemoteUri());
                    String url = appUser.getUid();
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            sendRequest(url);
                        }
                    });
                    Toast.makeText(UploadImageActivity.this, "Upload image success.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "error updating photo data");
                }
            }
        });
    }

    /**
     * Sets up the preliminary permissions for opening the camera and accessing storage. Requests permissions if not granted.
     */
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

    /**
     * On permission granted by UploadImageActivity::takePhoto, file destination created and camera opened.
     */
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
                Photo photo = new Photo(uri, null, new Date(), null, null);
                photos.clear();//TODO: photos is cleared every time because we are only doing 1 photo each time for MS1. Reimplement for later.
                photos.add(photo);
            } else {
                Log.e(TAG, "Image not saved. " + uri);
            }
        }
    });

    /**
     * Writes the taken picture to storage.
     * @return  File created by taking the photo and writing to storage. Get local URI from this object.
     */
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

    void sendRequest(String req) {
        try {
            URL url = new URL("http://34.142.160.9/app/" + req);
            URLConnection conn = url.openConnection();
            conn.connect();
            conn.getContent();
            Log.d(TAG, "success");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}