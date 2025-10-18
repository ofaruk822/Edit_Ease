package com.example.edit_ease_test;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import java.io.IOException;
import android.graphics.Canvas;
import android.graphics.Paint;

public class EditorActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button btnBack, btnRevert, btnSave, btnRotate, btnCrop;
    private Button btnEffects, btnSoftFilter, btnFrostFilter, btnContrast, btnExposure;
    private SeekBar adjustmentSlider;
    private TextView sliderValue;
    private LinearLayout effectsLayout, sliderLayout, mainToolsLayout;

    private Bitmap originalBitmap, currentBitmap;
    private Uri imageUri;
    private String currentAdjustment = "";
    private boolean isEffectsMenuVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        setupViews();
        loadImage();
        setupButtonClicks();
        setupSlider();
    }

    private void setupViews() {
        imageView = findViewById(R.id.imageView);

        // Navigation buttons
        btnBack = findViewById(R.id.btnBack);
        btnRevert = findViewById(R.id.btnRevert);
        btnSave = findViewById(R.id.btnSave);
        btnRotate = findViewById(R.id.btnRotate);

        // Tool buttons
        btnCrop = findViewById(R.id.btnCrop);
        btnEffects = findViewById(R.id.btnEffects);

        // Effect buttons
        btnSoftFilter = findViewById(R.id.btnSoftFilter);
        btnFrostFilter = findViewById(R.id.btnFrostFilter);
        btnContrast = findViewById(R.id.btnContrast);
        btnExposure = findViewById(R.id.btnExposure);

        // Layouts
        effectsLayout = findViewById(R.id.effectsLayout);
        sliderLayout = findViewById(R.id.sliderLayout);
        mainToolsLayout = findViewById(R.id.mainToolsLayout);

        // Slider
        adjustmentSlider = findViewById(R.id.adjustmentSlider);
        sliderValue = findViewById(R.id.sliderValue);
    }

    private void loadImage() {
        imageUri = getIntent().getData();
        if (imageUri != null) {
            try {
                originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                imageView.setImageBitmap(currentBitmap);
            } catch (IOException e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupButtonClicks() {
        // Back button - go to home
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(EditorActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Revert button - reset to original
        btnRevert.setOnClickListener(v -> {
            if (originalBitmap != null) {
                currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                imageView.setImageBitmap(currentBitmap);
                imageView.setColorFilter(null);
                hideSlider();
                Toast.makeText(this, "All edits reverted", Toast.LENGTH_SHORT).show();
            }
        });

        // Save button
        btnSave.setOnClickListener(v -> saveImage());

        // Rotate button
        btnRotate.setOnClickListener(v -> rotateImage());

        // Effects button - toggle effects menu
        btnEffects.setOnClickListener(v -> toggleEffectsMenu());

        // Crop button
        btnCrop.setOnClickListener(v -> cropImage());

        // Effect buttons
        btnSoftFilter.setOnClickListener(v -> applySoftFilter());
        btnFrostFilter.setOnClickListener(v -> applyFrostFilter());
        btnContrast.setOnClickListener(v -> showSliderForAdjustment("contrast"));
        btnExposure.setOnClickListener(v -> showSliderForAdjustment("exposure"));
    }

    private void toggleEffectsMenu() {
        if (isEffectsMenuVisible) {
            effectsLayout.setVisibility(View.GONE);
            mainToolsLayout.setVisibility(View.VISIBLE);
            hideSlider();
        } else {
            effectsLayout.setVisibility(View.VISIBLE);
            mainToolsLayout.setVisibility(View.GONE);
        }
        isEffectsMenuVisible = !isEffectsMenuVisible;
    }

    private void showSliderForAdjustment(String adjustment) {
        currentAdjustment = adjustment;
        sliderLayout.setVisibility(View.VISIBLE);
        adjustmentSlider.setProgress(100);
        sliderValue.setText(adjustment + ": 100");
    }

    private void hideSlider() {
        sliderLayout.setVisibility(View.GONE);
        currentAdjustment = "";
    }

    private void setupSlider() {
        adjustmentSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sliderValue.setText(currentAdjustment + ": " + progress);
                applyAdjustment(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void applyAdjustment(int value) {
        if (currentBitmap == null) return;

        ColorMatrix colorMatrix = new ColorMatrix();

        switch (currentAdjustment) {
            case "contrast":
                float contrast = value / 100.0f;
                float translate = (1 - contrast) / 2;
                colorMatrix.set(new float[] {
                        contrast, 0, 0, 0, translate,
                        0, contrast, 0, 0, translate,
                        0, 0, contrast, 0, translate,
                        0, 0, 0, 1, 0
                });
                break;

            case "exposure":
                float exposure = (value - 100) / 100.0f;
                colorMatrix.set(new float[] {
                        1, 0, 0, 0, exposure,
                        0, 1, 0, 0, exposure,
                        0, 0, 1, 0, exposure,
                        0, 0, 0, 1, 0
                });
                break;
        }

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        imageView.setColorFilter(filter);
    }

    private void rotateImage() {
        if (currentBitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            currentBitmap = Bitmap.createBitmap(currentBitmap, 0, 0,
                    currentBitmap.getWidth(), currentBitmap.getHeight(), matrix, true);
            originalBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
            imageView.setImageBitmap(currentBitmap);
            imageView.setColorFilter(null);
            hideSlider();
        }
    }

    private void cropImage() {
        if (currentBitmap != null) {
            // Simple center crop (replace with manual crop UI later)
            int newWidth = (int)(currentBitmap.getWidth() * 0.8);
            int newHeight = (int)(currentBitmap.getHeight() * 0.8);
            int startX = (currentBitmap.getWidth() - newWidth) / 2;
            int startY = (currentBitmap.getHeight() - newHeight) / 2;

            currentBitmap = Bitmap.createBitmap(currentBitmap, startX, startY, newWidth, newHeight);
            originalBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
            imageView.setImageBitmap(currentBitmap);
            imageView.setColorFilter(null);
            hideSlider();
            Toast.makeText(this, "Image cropped", Toast.LENGTH_SHORT).show();
        }
    }

    private void applySoftFilter() {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(new float[] {
                0.8f, 0, 0, 0, 0.1f,
                0, 0.8f, 0, 0, 0.1f,
                0, 0, 0.8f, 0, 0.1f,
                0, 0, 0, 1, 0
        });
        imageView.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        hideSlider();
    }

    private void applyFrostFilter() {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.set(new float[] {
                0.8f, 0, 0, 0, 0,
                0, 0.9f, 0, 0, 0,
                0, 0, 1.2f, 0, 0.1f,
                0, 0, 0, 1, 0
        });
        imageView.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        hideSlider();
    }

    private Bitmap applyCurrentFiltersToBitmap(Bitmap original) {
        // Create a mutable bitmap to draw on
        Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);

        // Get the current color filter from ImageView
        ColorMatrixColorFilter currentFilter = (ColorMatrixColorFilter) imageView.getColorFilter();

        if (currentFilter != null) {
            // Create paint with the current filter
            Paint paint = new Paint();
            paint.setColorFilter(currentFilter);

            // Draw the bitmap with filter applied
            canvas.drawBitmap(original, 0, 0, paint);
        } else {
            // No filter, just draw original
            canvas.drawBitmap(original, 0, 0, null);
        }

        return result;
    }
    private void saveImage() {
        if (currentBitmap != null) {
            try {
                // Apply current filters to the bitmap before saving
                Bitmap bitmapToSave = applyCurrentFiltersToBitmap(currentBitmap);

                String savedImageURL = MediaStore.Images.Media.insertImage(
                        getContentResolver(), bitmapToSave, "Edited_Photo", "Edited with Photo Editor");

                if (savedImageURL != null) {
                    Toast.makeText(this, "Image Saved to Gallery!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
            }
        }
    }
}