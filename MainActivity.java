package com.example.edit_ease_test;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.Toast;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Button btnAddImage;
    private ImageView backgroundImage1, backgroundImage2;
    private LinearLayout recentImagesLayout;
    private TextView noRecentText;
    private ActivityResultLauncher<String> pickImageLauncher;

    private ArrayList<Integer> backgroundImages = new ArrayList<>();
    private ArrayList<Uri> recentImageUris = new ArrayList<>();
    private int currentBgIndex = 0;
    private Handler bgHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();
        setupBackgrounds();
        setupImagePicker();
        loadRecentImages();
        startBackgroundCycle();
    }

    private void setupViews() {
        btnAddImage = findViewById(R.id.btnAddImage);
        backgroundImage1 = findViewById(R.id.backgroundImage1);
        backgroundImage2 = findViewById(R.id.backgroundImage2);
        recentImagesLayout = findViewById(R.id.recentImagesLayout);
        noRecentText = findViewById(R.id.noRecentText);
    }

    private void setupBackgrounds() {
        backgroundImages.add(R.drawable.screenshot_2025_10_25_020044);
        backgroundImages.add(R.drawable.screenshot_2025_10_25_020128);
        backgroundImages.add(R.drawable.screenshot_2025_10_25_020142);
        backgroundImages.add(R.drawable.screenshot_2025_10_25_020150);
        backgroundImages.add(R.drawable.screenshot_2025_10_25_020159);
        backgroundImages.add(R.drawable.screenshot_2025_10_25_020248);
        backgroundImages.add(R.drawable.screenshot_2025_10_25_020306);
        backgroundImages.add(R.drawable.screenshot_2025_10_25_020332);
        backgroundImages.add(R.drawable.screenshot_2025_10_25_020422);

        if (!backgroundImages.isEmpty()) {
            backgroundImage1.setImageResource(backgroundImages.get(0));
            if (backgroundImages.size() > 1) {
                backgroundImage2.setImageResource(backgroundImages.get(1));
            }
        }
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        try {
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        } catch (SecurityException e) {

                        }

                        addToRecentImages(uri);
                        Toast.makeText(this, "Added to recent!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }
        );

        btnAddImage.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });
    }

    private void startBackgroundCycle() {
        bgHandler.postDelayed(backgroundCycleRunnable, 5000);
    }

    private Runnable backgroundCycleRunnable = new Runnable() {
        @Override
        public void run() {
            if (backgroundImages.size() > 1) {
                crossfadeBackgrounds();
            }
            bgHandler.postDelayed(this, 5000);
        }
    };

    private void crossfadeBackgrounds() {
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(2000);

        backgroundImage2.startAnimation(fadeIn);
        backgroundImage2.setAlpha(1.0f);

        bgHandler.postDelayed(() -> {
            currentBgIndex = (currentBgIndex + 1) % backgroundImages.size();
            int nextBgIndex = (currentBgIndex + 1) % backgroundImages.size();

            backgroundImage1.setImageResource(backgroundImages.get(currentBgIndex));
            backgroundImage2.setImageResource(backgroundImages.get(nextBgIndex));
            backgroundImage2.setAlpha(0.0f);
        }, 2000);
    }

    private void addToRecentImages(Uri imageUri) {
        recentImageUris.add(0, imageUri);

        if (recentImageUris.size() > 6) {
            recentImageUris.remove(recentImageUris.size() - 1);
        }

        updateRecentImagesDisplay();
    }

    private void loadRecentImages() {
        updateRecentImagesDisplay();
    }

    private void updateRecentImagesDisplay() {
        recentImagesLayout.removeAllViews();

        if (recentImageUris.isEmpty()) {
            noRecentText.setVisibility(View.VISIBLE);
        } else {
            noRecentText.setVisibility(View.GONE);

            for (Uri uri : recentImageUris) {
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
                params.setMargins(8, 0, 8, 0);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setBackgroundResource(android.R.color.darker_gray);

                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true);
                    imageView.setImageBitmap(smallBitmap);
                    if (bitmap != smallBitmap) {
                        bitmap.recycle();
                    }
                } catch (Exception e) {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                }

                imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                    intent.setData(uri);
                    startActivity(intent);
                });

                recentImagesLayout.addView(imageView);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        bgHandler.removeCallbacks(backgroundCycleRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bgHandler.removeCallbacks(backgroundCycleRunnable);
        bgHandler.postDelayed(backgroundCycleRunnable, 5000);
    }
}