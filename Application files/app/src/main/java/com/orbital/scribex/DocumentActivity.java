package com.orbital.scribex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class DocumentActivity extends AppCompatActivity {

    private Button btnDelete;

    private ImageView imgViewDocPreview;

    private TextView txtName;
    private TextView txtDateCreated;
    private TextView txtDoc;

    private Document doc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document);

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
//        txtDateCreated.setText("Created on: " + doc.getDateTime());
        txtDoc.setText(doc.getText());

    }
}