package com.orbital.scribex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class DocumentViewActivity extends AppCompatActivity {

    private Button btnDelete;

    private ImageView imgViewDocPreview;

    private TextView txtName;
    private TextView txtDateCreated;
    private TextView txtDoc;

    private Document doc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_document_view);

        Intent intent = this.getIntent();
        doc = (Document) intent.getSerializableExtra("Document");

        btnDelete = findViewById(R.id.btnDelete);
        imgViewDocPreview = findViewById(R.id.imgViewDocPreview);
        txtName = findViewById(R.id.txtName);
        txtDateCreated = findViewById(R.id.txtDateCreated);
        txtDoc = findViewById(R.id.txtDoc);

        //TODO: set imgViewDocPreview to be first page of pics
        //TODO: set date time when available
        //TODO: implement delete button

        txtName.setText(doc.getName());
        txtDoc.setText(doc.getText());

    }
}