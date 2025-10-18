package com.example.edit_ease_test;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button btnGallery;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the gallery button
        btnGallery = findViewById(R.id.btnGallery);

        // This handles picking image from gallery
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        // Open editor with the selected image
                        Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }
        );

        // When button clicked, open gallery
        btnGallery.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });
    }
}