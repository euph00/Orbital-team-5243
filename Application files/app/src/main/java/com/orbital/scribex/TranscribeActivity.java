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
import android.view.Window;
import android.view.WindowManager;
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

public class TranscribeActivity extends AppCompatActivity {

    private static final String TAG = "TranscribeActivity";

    //misc
    private List<Photo> photos;
    private String currImgPath;
    private Uri uri = null;

    //user
    private FirebaseFirestore firestore;
    private StorageReference storageReference;
    private ScribexUser appUser;

    //view
    private EditText editTextDocName;
    private Button btnTakePic;
    private Button btnUpload;
    private ImageView imageViewDoc;
    private TextView textViewWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_transcribe);

        //user specific elements
        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        //retrieve ScribexUser
        Intent intent = this.getIntent();
        appUser = (ScribexUser) intent.getSerializableExtra("user");

        //misc inits
        photos = new ArrayList<>();
        currImgPath = "";

        //init view elements
        editTextDocName = findViewById(R.id.editTextDocName);
        btnTakePic = findViewById(R.id.btnTakePic);
        btnUpload = findViewById(R.id.btnUpload);
        imageViewDoc = findViewById(R.id.imageViewDoc);
        textViewWarning = findViewById(R.id.textViewWarning);

        //onClickListeners for buttons
        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
                btnTakePic.setVisibility(View.INVISIBLE);
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newDocName = editTextDocName.getText().toString();
                collisionCheck(newDocName);
            }
        });
    }

    /**
     * Checks for naming collisions with previously uploaded docs, shows warning
     * and rejects if there is a name collision
     * @param newDocName    the desired new document name
     */
    private void collisionCheck(String newDocName) {
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
                                    return;
                                }
                            }
                            upload();
                        }
                    }
                });
    }

    /**
     * warns user about naming collision of new doc with and existing one
     */
    private void showWarningNameCollision() {
        textViewWarning.setVisibility(View.VISIBLE);
    }

    /**
     * Tries to upload photos in the Collection photos to firebase storage. This method DOES NOT update firestore itself.
     * Firestore updates are handled by TranscribeActivity::updatePhotoDatabase called in this method.
     */
    private void upload() {
        //Guard clause: case where there is nothing to upload
        if (photos.isEmpty()) {
            Toast.makeText(this, "Please take a picture first", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Photo photo : photos) {
            Uri locUri = photo.getLocalUri();
            StorageReference imageRef = storageReference.child(String.format("images/%s/%s", appUser.getUid(), editTextDocName.getText().toString()));
            UploadTask task = imageRef.putFile(locUri);
            task.addOnCompleteListener(this, new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        Task<Uri> downloadUrl = imageRef.getDownloadUrl();
                        updatePhotoRemoteUri(downloadUrl, photo);
                    } else {
                        Log.e(TAG, "upload failed");
                    }
                }
            });
        }
    }

    /**
     * Updates the metadata of the photo object with its URL on firebase cloud storage
     * @param downloadUrl   the task to retrieve remote url
     * @param photo Photo object to be updated
     */
    private void updatePhotoRemoteUri(Task<Uri> downloadUrl, Photo photo) {
        downloadUrl.addOnSuccessListener(TranscribeActivity.this, new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri remoteUri) {
                photo.setRemoteUri(remoteUri);
                updatePhotoDatabase(photo);
            }
        });
    }

    /**
     * This method is to be called by TranscribeActivity::upload. This handles updating of firestore database.
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
                    if (name.equals("")) name = photo.getId();
                    photo.setName(name);

                    firestore.collection("users")
                            .document(appUser.getUid())
                            .collection("uploads")
                            .document(photo.getName())
                            .set(photo);

                    updateQueuePingServer(photo);

                    firestore.collection("users")
                            .document(appUser.getUid())
                            .collection("uploads")
                            .document(photo.getId())
                            .delete();
                    Toast.makeText(TranscribeActivity.this, "Upload image success.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "error updating photo data");
                }
            }
        });
    }

    /**
     * Updates the QUEUE on firestore, sends request to backend to begin transcription
     * @param photo photo object to be transcribed
     */
    private void updateQueuePingServer(Photo photo) {
        firestore.collection("users")
                .document(appUser.getUid())
                .collection("uploads")
                .document("QUEUE")
                .update(photo.getName(), photo.getRemoteUri()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            String url = appUser.getUid();
                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            executor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    sendRequest(url);
                                }
                            });
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
                        Toast.makeText(TranscribeActivity.this, "Unable to open camera, please grant permissions", Toast.LENGTH_SHORT).show();
                    } else {
                        invokeCamera();
                    }
                }
            });

    /**
     * On permission granted by TranscribeActivity::takePhoto, file destination created and camera opened.
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
                imageViewDoc.setImageURI(uri);
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
            File result = File.createTempFile(String.format("photograph_%s", editTextDocName.getText().toString()), ".jpg", imageDir);
            currImgPath = result.getAbsolutePath();
            return result;
        } catch (IOException e) {
            Toast.makeText(this, "IOexception: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        return null;
    }

    /**
     * sends request to backend to begin transcription
     * @param req   request id, currently implemented as user id
     */
    void sendRequest(String req) {
        try {
            URL url = new URL("http://34.142.160.9/app/" + req);
            URLConnection conn = url.openConnection();
            conn.connect();
            Object content = conn.getContent();

            Log.d(TAG, "success on pinging server" + content);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}