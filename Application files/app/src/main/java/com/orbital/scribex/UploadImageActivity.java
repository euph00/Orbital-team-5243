package com.orbital.scribex;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class UploadImageActivity extends AppCompatActivity {

    private static final String TAG = "UploadImageActivity";

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