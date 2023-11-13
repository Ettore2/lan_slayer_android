package com.example.lanslayer;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DictionaryActivity extends AppCompatActivity {

    TextView textInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictionary);

        textInfo = findViewById(R.id.text_info);
    }

    public void back(View view){
        finish();

    }
}