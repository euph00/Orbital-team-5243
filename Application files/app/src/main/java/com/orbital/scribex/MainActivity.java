package com.orbital.scribex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.btnLogin = findViewById(R.id.btnLogin);
    }

    public void btnLoginOnclick(View view) {
        //TODO: implement google login
        Intent loginIntent = new Intent(this, PersonalMenuActivity.class);
        startActivity(loginIntent);
    }
}