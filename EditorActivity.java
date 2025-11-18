package com.example.edit_ease_test;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;

public class EditorActivity extends AppCompatActivity {

    ImageButton btnBack, btnRevert, btnSave;

    ImageButton btnCrop, btnRotate, btnAddText, btnEffects, btnAdjust;

    ImageButton btnBwFilter, btnFrostFilter, btnSoftFilter, btnFadedFilter;

    ImageButton btnSaturation, btnContrast, btnTint, btnTemperature;

    CustomImageView imageView;
    SeekBar adjustmentSlider;
    TextView adjustmentValue;

    LinearLayout mainToolsLayout, effectsLayout, adjustLayout, adjustmentSliderLayout;

    Bitmap originalBitmap, currentBitmap;
    Uri imageUri;
    String currentAdjustment = "";

    private ColorMatrix cumulativeColorMatrix = new ColorMatrix();
    private boolean hasActiveFilter = false;

    private RectF cropRectF = new RectF();
    private boolean isCropping = false;
    private float startX, startY;
    private Paint gridPaint;

    private boolean isAddingText = false;
    private String currentText = "";
    private float textX = 0;
    private float textY = 0;
    private float textWidth = 0;
    private float textHeight = 0;
    private float textSize = 60f;
    private int textColor = Color.BLACK;
    private Paint textPaint;
    private Paint textBackgroundPaint;
    private Paint handlePaint;
    private boolean isDragging = false;
    private boolean isResizing = false;
    private int resizeHandle = -1;
    private float touchStartX, touchStartY;
    private float textStartX, textStartY;
    private float textStartWidth, textStartHeight;
    private RectF textRect = new RectF();
    private boolean textAdded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        findViews();
        setupGridPaint();
        setupTextPaint();
        loadImage();
        setupClickListeners();
    }

    void findViews() {
        imageView = findViewById(R.id.imageView);
        btnBack = findViewById(R.id.btnBack);
        btnRevert = findViewById(R.id.btnRevert);
        btnSave = findViewById(R.id.btnSave);

        btnCrop = findViewById(R.id.btnCrop);
        btnRotate = findViewById(R.id.btnRotate);
        btnAddText = findViewById(R.id.btnAddText);
        btnEffects = findViewById(R.id.btnEffects);
        btnAdjust = findViewById(R.id.btnAdjust);

        btnBwFilter = findViewById(R.id.btnBwFilter);
        btnFrostFilter = findViewById(R.id.btnFrostFilter);
        btnSoftFilter = findViewById(R.id.btnSoftFilter);
        btnFadedFilter = findViewById(R.id.btnFadedFilter);

        btnSaturation = findViewById(R.id.btnSaturation);
        btnContrast = findViewById(R.id.btnContrast);
        btnTint = findViewById(R.id.btnTint);
        btnTemperature = findViewById(R.id.btnTemperature);

        mainToolsLayout = findViewById(R.id.mainToolsLayout);
        effectsLayout = findViewById(R.id.effectsLayout);
        adjustLayout = findViewById(R.id.adjustLayout);
        adjustmentSliderLayout = findViewById(R.id.adjustmentSliderLayout);

        adjustmentSlider = findViewById(R.id.adjustmentSlider);
        adjustmentValue = findViewById(R.id.adjustmentValue);
    }

    void setupGridPaint() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.WHITE);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2f);
        gridPaint.setAntiAlias(true);
    }

    void setupTextPaint() {
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(48f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.LEFT);

        textBackgroundPaint = new Paint();
        textBackgroundPaint.setColor(Color.TRANSPARENT);
        textBackgroundPaint.setStyle(Paint.Style.STROKE);
        textBackgroundPaint.setStrokeWidth(3f);
        textBackgroundPaint.setAntiAlias(true);

        handlePaint = new Paint();
        handlePaint.setColor(Color.WHITE);
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setAntiAlias(true);
    }

    private int getOrientationFromExif(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return 0;
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            inputStream.close();

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    void loadImage() {
        imageUri = getIntent().getData();

        if (imageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            Bitmap original = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            int rotation = getOrientationFromExif(imageUri);
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                original = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
            }

            int maxSize = 1024;
            int width = original.getWidth();
            int height = original.getHeight();

            if (width > maxSize || height > maxSize) {
                float ratio = (float) width / height;
                if (ratio > 1) {
                    width = maxSize;
                    height = (int) (maxSize / ratio);
                } else {
                    height = maxSize;
                    width = (int) (maxSize * ratio);
                }
                originalBitmap = Bitmap.createScaledBitmap(original, width, height, true);
                original.recycle();
            } else {
                originalBitmap = original;
            }

            currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            imageView.setImageBitmap(currentBitmap);
            imageView.setCropRect(cropRectF);
            imageView.setGridPaint(gridPaint);

            imageView.post(() -> {
                if (imageView.getWidth() > 0 && imageView.getHeight() > 0) {
                    cropRectF.set(0, 0, imageView.getWidth(), imageView.getHeight());
                    imageView.setCropRect(cropRectF);
                }
            });

        } catch (IOException | SecurityException e) {
            Toast.makeText(this, "Failed to load image. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (effectsLayout.getVisibility() == View.VISIBLE || adjustLayout.getVisibility() == View.VISIBLE) {
                showMainTools();
            } else {
                finish();
            }
        });

        btnRevert.setOnClickListener(v -> revertImage());
        btnSave.setOnClickListener(v -> saveImage());

        btnEffects.setOnClickListener(v -> showEffects());
        btnAdjust.setOnClickListener(v -> showAdjust());

        btnCrop.setOnClickListener(v -> toggleCropMode());
        btnRotate.setOnClickListener(v -> rotateImage90());
        btnAddText.setOnClickListener(v -> startAddingText());

        btnBwFilter.setOnClickListener(v -> applyBlackWhite());
        btnFrostFilter.setOnClickListener(v -> showFrostSlider());
        btnSoftFilter.setOnClickListener(v -> applySoftFilter());
        btnFadedFilter.setOnClickListener(v -> applyFadedFilter());

        btnSaturation.setOnClickListener(v -> showSaturationSlider());
        btnContrast.setOnClickListener(v -> showBrightnessSlider());
        btnTint.setOnClickListener(v -> showTintSlider());
        btnTemperature.setOnClickListener(v -> showTemperatureSlider());

        adjustmentSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                adjustmentValue.setText(String.valueOf(progress));
                applyAdjustment(progress);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {
                finalizeAdjustment(seekBar.getProgress());
                adjustmentSliderLayout.setVisibility(View.GONE);
                showMainTools();
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(EditorActivity.this,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDown(MotionEvent e) {
                            if (isCropping) {
                                startX = e.getX();
                                startY = e.getY();
                            }
                            return true;
                        }
                    });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isAddingText) {
                    return handleTextTouch(event);
                }

                if (!isCropping) return false;

                gestureDetector.onTouchEvent(event);

                float x = event.getX();
                float y = event.getY();

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    float width = x - startX;
                    float height = y - startY;

                    cropRectF.set(
                            Math.min(startX, startX + width),
                            Math.min(startY, startY + height),
                            Math.max(startX, startX + width),
                            Math.max(startY, startY + height)
                    );
                    imageView.setCropRect(cropRectF);
                    imageView.invalidate();
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (cropRectF.width() > 10 && cropRectF.height() > 10) {
                        performCrop();
                    }
                    performCrop();
                    isCropping = false;
                    imageView.invalidate();
                }
                return true;
            }
        });
    }

    void startAddingText() {
        isAddingText = true;
        currentText = "";
        textAdded = false;

        textX = imageView.getWidth() / 2f;
        textY = imageView.getHeight() / 2f;

        textWidth = 400f;
        textHeight = 150f;

        textRect.set(textX - textWidth/2, textY - textHeight/2,
                textX + textWidth/2, textY + textHeight/2);

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }

        showTextInputDialog();
    }

    private RectF convertViewRectToBitmapRect(RectF viewRect) {
        if (imageView.getWidth() == 0 || imageView.getHeight() == 0) return viewRect;

        float scaleX = (float) currentBitmap.getWidth() / imageView.getWidth();
        float scaleY = (float) currentBitmap.getHeight() / imageView.getHeight();

        return new RectF(
                viewRect.left * scaleX,
                viewRect.top * scaleY,
                viewRect.right * scaleX,
                viewRect.bottom * scaleY
        );
    }

    private boolean handleTextTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (textAdded) {
                    if (textRect.contains(x, y)) {
                        float handleRadius = 30f;
                        float[] handleX = {textRect.left, textRect.right, textRect.right, textRect.left};
                        float[] handleY = {textRect.top, textRect.top, textRect.bottom, textRect.bottom};

                        for (int i = 0; i < 4; i++) {
                            if (Math.abs(x - handleX[i]) < handleRadius && Math.abs(y - handleY[i]) < handleRadius) {
                                isResizing = true;
                                resizeHandle = i;
                                touchStartX = x;
                                touchStartY = y;
                                textStartX = textRect.left;
                                textStartY = textRect.top;
                                textStartWidth = textRect.width();
                                textStartHeight = textRect.height();
                                return true;
                            }
                        }

                        isDragging = true;
                        touchStartX = x;
                        touchStartY = y;
                        textStartX = textRect.left;
                        textStartY = textRect.top;
                        return true;
                    }
                    return false;
                }
                float handleRadius = 30f;
                float[] handleX = {textRect.left, textRect.right, textRect.right, textRect.left};
                float[] handleY = {textRect.top, textRect.top, textRect.bottom, textRect.bottom};

                for (int i = 0; i < 4; i++) {
                    if (Math.abs(x - handleX[i]) < handleRadius && Math.abs(y - handleY[i]) < handleRadius) {
                        isResizing = true;
                        resizeHandle = i;
                        touchStartX = x;
                        touchStartY = y;
                        textStartX = textRect.left;
                        textStartY = textRect.top;
                        textStartWidth = textRect.width();
                        textStartHeight = textRect.height();
                        return true;
                    }
                }
                if (textRect.contains(x, y)) {
                    isDragging = true;
                    touchStartX = x;
                    touchStartY = y;
                    textStartX = textRect.left;
                    textStartY = textRect.top;
                    return true;
                }
                if (!textRect.contains(x, y) && !currentText.isEmpty()) {
                    new android.os.Handler().postDelayed(() -> {
                        textAdded = true;
                        isAddingText = false;
                        updateTextInImageView();
                        showMainTools();
                    }, 50);
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isResizing) {
                    float dx = (x - touchStartX) * 0.35f;
                    float dy = (y - touchStartY) * 0.35f;
                    float newLeft = textStartX;
                    float newTop = textStartY;
                    float newRight = textStartX + textStartWidth;
                    float newBottom = textStartY + textStartHeight;

                    switch (resizeHandle) {
                        case 0:
                            newLeft += dx;
                            newTop += dy;
                            break;
                        case 1:
                            newTop += dy;
                            newRight += dx;
                            break;
                        case 2:
                            newRight += dx;
                            newBottom += dy;
                            break;
                        case 3:
                            newBottom += dy;
                            newLeft += dx;
                            break;
                    }
                    if (newRight - newLeft > 100 && newBottom - newTop > 50) {
                        textRect.set(newLeft, newTop, newRight, newBottom);
                        updateTextInImageView();
                    }
                    return true;
                }

                if (isDragging) {
                    float dx = (x - touchStartX) * 0.2f;
                    float dy = (y - touchStartY) * 0.2f;
                    textRect.offset(dx, dy);
                    updateTextInImageView();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                isDragging = false;
                isResizing = false;
                resizeHandle = -1;
                break;
        }
        return true;
    }

    void showTextInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Text");

        final EditText input = new EditText(this);
        input.setText(currentText);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            currentText = input.getText().toString();
            if (!currentText.isEmpty()) {
                updateTextInImageView();
            } else {
                isAddingText = false;
                showMainTools();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            isAddingText = false;
            showMainTools();
        });

        builder.show();
    }

    void updateTextInImageView() {
        imageView.setTextMode(isAddingText, currentText, textRect, textAdded);
        imageView.setTextPaints(textPaint, textBackgroundPaint, handlePaint);
        imageView.invalidate();
    }

    void showMainTools() {
        mainToolsLayout.setVisibility(View.VISIBLE);
        effectsLayout.setVisibility(View.GONE);
        adjustLayout.setVisibility(View.GONE);
        adjustmentSliderLayout.setVisibility(View.GONE);
        imageView.setShowGrid(false);
        updateTextInImageView();

        if (isAddingText) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
            isAddingText = false;
        }
    }

    void showEffects() {
        mainToolsLayout.setVisibility(View.GONE);
        effectsLayout.setVisibility(View.VISIBLE);
        adjustLayout.setVisibility(View.GONE);
        adjustmentSliderLayout.setVisibility(View.GONE);
        imageView.setShowGrid(false);
        imageView.invalidate();
    }

    void showAdjust() {
        mainToolsLayout.setVisibility(View.GONE);
        effectsLayout.setVisibility(View.GONE);
        adjustLayout.setVisibility(View.VISIBLE);
        adjustmentSliderLayout.setVisibility(View.GONE);
        imageView.setShowGrid(false);
        imageView.invalidate();
    }

    void applyBlackWhite() {
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        cumulativeColorMatrix.preConcat(cm);
        hasActiveFilter = true;
        updateImageViewWithCumulativeFilter();
        updateTextInImageView();
        showMainTools();
    }

    void applySoftFilter() {
        applyCurrentFilterToBitmap();

        int width = currentBitmap.getWidth();
        int height = currentBitmap.getHeight();
        Bitmap finalBitmap = Bitmap.createBitmap(width, height, currentBitmap.getConfig());
        Canvas canvas = new Canvas(finalBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        Bitmap tempDownscaledBitmap = Bitmap.createScaledBitmap(currentBitmap, (int) (width * 0.7f), (int) (height * 0.7f), true);
        canvas.drawBitmap(tempDownscaledBitmap, null, new android.graphics.Rect(0, 0, width, height), paint);
        tempDownscaledBitmap.recycle();
        ColorMatrix warmCM = new ColorMatrix();
        warmCM.set(new float[]{
                1.1f, 0, 0, 0, 0,
                0, 1.0f, 0, 0, 0,
                0, 0, 0.9f, 0, 0,
                0, 0, 0, 1, 0
        });
        paint.setColorFilter(new ColorMatrixColorFilter(warmCM));
        canvas.drawBitmap(finalBitmap, 0, 0, paint);
        Bitmap oldBitmap = currentBitmap;
        currentBitmap = finalBitmap;
        imageView.setImageBitmap(currentBitmap);

        if (oldBitmap != null && oldBitmap != originalBitmap && !oldBitmap.isRecycled()) {
            oldBitmap.recycle();
        }
        cumulativeColorMatrix.reset();
        hasActiveFilter = false;
        imageView.setColorFilter(null);
        updateTextInImageView();
        showMainTools();
    }

    void applyFadedFilter() {
        applyCurrentFilterToBitmap();
        Bitmap fadedBitmap = Bitmap.createBitmap(currentBitmap.getWidth(), currentBitmap.getHeight(), currentBitmap.getConfig());
        Canvas canvas = new Canvas(fadedBitmap);
        Paint paint = new Paint();

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.6f);
        ColorMatrix contrast = new ColorMatrix();
        contrast.set(new float[]{
                0.9f, 0, 0, 0, 10f,
                0, 0.9f, 0, 0, 10f,
                0, 0, 0.9f, 0, 10f,
                0, 0, 0, 1, 0
        });
        cm.postConcat(contrast);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));

        canvas.drawBitmap(currentBitmap, 0, 0, paint);
        Bitmap oldBitmap = currentBitmap;
        currentBitmap = fadedBitmap;
        imageView.setImageBitmap(currentBitmap);

        if (oldBitmap != null && oldBitmap != originalBitmap && !oldBitmap.isRecycled()) {
            oldBitmap.recycle();
        }
        cumulativeColorMatrix.reset();
        hasActiveFilter = false;
        imageView.setColorFilter(null);

        updateTextInImageView();
        showMainTools();
    }

    void updateImageViewWithCumulativeFilter() {
        imageView.setColorFilter(new ColorMatrixColorFilter(cumulativeColorMatrix));
    }

    void showSaturationSlider() {
        currentAdjustment = "saturation";
        adjustmentSlider.setProgress(100);
        adjustmentSliderLayout.setVisibility(View.VISIBLE);
    }

    void showBrightnessSlider() {
        currentAdjustment = "brightness";
        adjustmentSlider.setProgress(100);
        adjustmentSliderLayout.setVisibility(View.VISIBLE);
    }

    void showTintSlider() {
        currentAdjustment = "tint";
        adjustmentSlider.setProgress(100);
        adjustmentSliderLayout.setVisibility(View.VISIBLE);
    }

    void showTemperatureSlider() {
        currentAdjustment = "temperature";
        adjustmentSlider.setProgress(100);
        adjustmentSliderLayout.setVisibility(View.VISIBLE);
    }

    void showFrostSlider() {
        currentAdjustment = "frost";
        adjustmentSlider.setProgress(100);
        adjustmentSliderLayout.setVisibility(View.VISIBLE);
    }

    void toggleCropMode() {
        isCropping = !isCropping;
        imageView.setShowGrid(isCropping);
        if (isCropping) {
            Toast.makeText(this, "Crop mode on - drag to adjust", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Crop mode off", Toast.LENGTH_SHORT).show();
        }
    }


    void rotateImage90() {
        applyCurrentFilterToBitmap();
        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        Bitmap rotatedBitmap = Bitmap.createBitmap(currentBitmap, 0, 0,
                currentBitmap.getWidth(), currentBitmap.getHeight(), matrix, true);

        Bitmap oldBitmap = currentBitmap;
        currentBitmap = rotatedBitmap;
        imageView.setImageBitmap(currentBitmap);

        if (oldBitmap != null && oldBitmap != originalBitmap && !oldBitmap.isRecycled()) {
            oldBitmap.recycle();
        }

        cumulativeColorMatrix.reset();
        hasActiveFilter = false;
        imageView.setColorFilter(null);
        if (textAdded) {
            float oldBitmapCenterX = oldBitmap.getWidth() / 2f;
            float oldBitmapCenterY = oldBitmap.getHeight() / 2f;
            float oldTextCenterX = (textRect.left + textRect.right) / 2f;
            float oldTextCenterY = (textRect.top + textRect.bottom) / 2f;
            float offsetX = oldTextCenterX - oldBitmapCenterX;
            float offsetY = oldTextCenterY - oldBitmapCenterY;
            float newOffsetX = -offsetY;
            float newOffsetY = offsetX;

            float newBitmapCenterX = currentBitmap.getWidth() / 2f;
            float newBitmapCenterY = currentBitmap.getHeight() / 2f;

            float newTextCenterX = newBitmapCenterX + newOffsetX;
            float newTextCenterY = newBitmapCenterY + newOffsetY;

            float newTextWidth = textRect.height();
            float newTextHeight = textRect.width();

            textRect.set(
                    newTextCenterX - newTextWidth / 2f,
                    newTextCenterY - newTextHeight / 2f,
                    newTextCenterX + newTextWidth / 2f,
                    newTextCenterY + newTextHeight / 2f
            );
        }
        updateTextInImageView();
    }


    void performCrop() {
        if (cropRectF.width() < 50 || cropRectF.height() < 50) {
            Toast.makeText(this, "Crop area too small", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            applyCurrentFilterToBitmap();

            float scaleX = (float) currentBitmap.getWidth() / imageView.getWidth();
            float scaleY = (float) currentBitmap.getHeight() / imageView.getHeight();

            int bitmapX = (int) (cropRectF.left * scaleX);
            int bitmapY = (int) (cropRectF.top * scaleY);
            int bitmapWidth = Math.min((int) (cropRectF.width() * scaleX), currentBitmap.getWidth() - bitmapX);
            int bitmapHeight = Math.min((int) (cropRectF.height() * scaleY), currentBitmap.getHeight() - bitmapY);

            if (bitmapWidth <= 0 || bitmapHeight <= 0) return;
            Bitmap croppedBitmap = Bitmap.createBitmap(currentBitmap, bitmapX, bitmapY, bitmapWidth, bitmapHeight);

            Bitmap oldBitmap = currentBitmap;
            currentBitmap = croppedBitmap;
            imageView.setImageBitmap(currentBitmap);

            if (oldBitmap != null && oldBitmap != originalBitmap && !oldBitmap.isRecycled()) {
                oldBitmap.recycle();
            }

            if (textAdded) {
                float newLeft = textRect.left - bitmapX;
                float newTop = textRect.top - bitmapY;
                float newRight = textRect.right - bitmapX;
                float newBottom = textRect.bottom - bitmapY;
                textRect.set(newLeft, newTop, newRight, newBottom);
            }
            isCropping = false;
            imageView.setShowGrid(false);
            updateTextInImageView();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to crop image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }



    void applyAdjustment(int value) {
        ColorMatrix cm = new ColorMatrix();
        float normalized = value / 100.0f;

        if ("saturation".equals(currentAdjustment)) {
            cm.setSaturation(normalized);
        } else if ("brightness".equals(currentAdjustment)) {
            cm.set(new float[] {
                    normalized, 0, 0, 0, 0,
                    0, normalized, 0, 0, 0,
                    0, 0, normalized, 0, 0,
                    0, 0, 0, 1, 0
            });
        } else if ("tint".equals(currentAdjustment)) {
            float redAdjust = 1.0f + (normalized - 1.0f) * 0.5f;
            float greenAdjust = 1.0f - (normalized - 1.0f) * 0.5f;
            float blueAdjust = 1.0f;
            cm.set(new float[] {
                    redAdjust, 0, 0, 0, 0,
                    0, greenAdjust, 0, 0, 0,
                    0, 0, blueAdjust, 0, 0,
                    0, 0, 0, 1, 0
            });
        } else if ("temperature".equals(currentAdjustment)) {
            float redAdjust = 1.0f + (normalized - 1.0f) * 0.3f;
            float greenAdjust = 1.0f + (normalized - 1.0f) * 0.1f;
            float blueAdjust = 1.0f - (normalized - 1.0f) * 0.3f;
            cm.set(new float[] {
                    redAdjust, 0, 0, 0, 0,
                    0, greenAdjust, 0, 0, 0,
                    0, 0, blueAdjust, 0, 0,
                    0, 0, 0, 1, 0
            });
        } else if ("frost".equals(currentAdjustment)) {
            float blueBoost = 1.0f + (normalized - 1.0f) * 0.5f;
            float redTweak = 0.8f + (normalized - 1.0f) * 0.2f;
            float greenTweak = 0.9f + (normalized - 1.0f) * 0.1f;
            cm.set(new float[] {
                    redTweak, 0, 0, 0, 0,
                    0, greenTweak, 0, 0, 0,
                    0, 0, blueBoost, 0, normalized * 0.1f,
                    0, 0, 0, 1, 0
            });
        }

        ColorMatrix combined = new ColorMatrix(cumulativeColorMatrix);
        combined.postConcat(cm);
        imageView.setColorFilter(new ColorMatrixColorFilter(combined));
    }

    void finalizeAdjustment(int value) {
        ColorMatrix temp = new ColorMatrix();
        float normalized = value / 100.0f;

        if ("saturation".equals(currentAdjustment)) {
            temp.setSaturation(normalized);
        } else if ("brightness".equals(currentAdjustment)) {
            temp.set(new float[] {
                    normalized, 0, 0, 0, 0,
                    0, normalized, 0, 0, 0,
                    0, 0, normalized, 0, 0,
                    0, 0, 0, 1, 0
            });
        } else if ("tint".equals(currentAdjustment)) {
            float redAdjust = 1.0f + (normalized - 1.0f) * 0.5f;
            float greenAdjust = 1.0f - (normalized - 1.0f) * 0.5f;
            float blueAdjust = 1.0f;
            temp.set(new float[] {
                    redAdjust, 0, 0, 0, 0,
                    0, greenAdjust, 0, 0, 0,
                    0, 0, blueAdjust, 0, 0,
                    0, 0, 0, 1, 0
            });
        } else if ("temperature".equals(currentAdjustment)) {
            float redAdjust = 1.0f + (normalized - 1.0f) * 0.3f;
            float greenAdjust = 1.0f + (normalized - 1.0f) * 0.1f;
            float blueAdjust = 1.0f - (normalized - 1.0f) * 0.3f;
            temp.set(new float[] {
                    redAdjust, 0, 0, 0, 0,
                    0, greenAdjust, 0, 0, 0,
                    0, 0, blueAdjust, 0, 0,
                    0, 0, 0, 1, 0
            });
        } else if ("frost".equals(currentAdjustment)) {
            float blueBoost = 1.0f + (normalized - 1.0f) * 0.5f;
            float redTweak = 0.8f + (normalized - 1.0f) * 0.2f;
            float greenTweak = 0.9f + (normalized - 1.0f) * 0.1f;
            temp.set(new float[] {
                    redTweak, 0, 0, 0, 0,
                    0, greenTweak, 0, 0, 0,
                    0, 0, blueBoost, 0, normalized * 0.1f,
                    0, 0, 0, 1, 0
            });
        }

        cumulativeColorMatrix.preConcat(temp);
        hasActiveFilter = true;
        updateImageViewWithCumulativeFilter();
    }

    void revertImage() {
        currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        imageView.setImageBitmap(currentBitmap);
        cumulativeColorMatrix.reset();
        hasActiveFilter = false;
        imageView.setColorFilter(null);
        adjustmentSliderLayout.setVisibility(View.GONE);
        textAdded = false;
        isAddingText = false;
        currentText = "";
        updateTextInImageView();
        showMainTools();
    }

    void saveImage() {
        try {
            applyCurrentFilterToBitmap();
            Bitmap bitmapToSave = currentBitmap;
            if (textAdded && !currentText.isEmpty()) {
                Bitmap bitmapWithText = applyTextToBitmap();
                if (bitmapWithText != null) {
                    bitmapToSave = bitmapWithText;
                }
            }

            String result = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    bitmapToSave,
                    "EditEase_" + System.currentTimeMillis(),
                    "Image edited with EditEase");

            if (bitmapToSave != currentBitmap && bitmapToSave != null && !bitmapToSave.isRecycled()) {
                bitmapToSave.recycle();
            }

            if (result != null) {
                Toast.makeText(this, "Image saved successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Save failed - check app permissions", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }



    void applyCurrentFilterToBitmap() {
        if (hasActiveFilter) {
            Bitmap filteredBitmap = Bitmap.createBitmap(currentBitmap.getWidth(), currentBitmap.getHeight(), currentBitmap.getConfig());
            Canvas canvas = new Canvas(filteredBitmap);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(cumulativeColorMatrix));
            canvas.drawBitmap(currentBitmap, 0, 0, paint);

            Bitmap oldBitmap = currentBitmap;
            currentBitmap = filteredBitmap;
            imageView.setImageBitmap(currentBitmap);

            if (oldBitmap != null && oldBitmap != originalBitmap && !oldBitmap.isRecycled()) {
                oldBitmap.recycle();
            }

            cumulativeColorMatrix.reset();
            hasActiveFilter = false;
            imageView.setColorFilter(null);
        }
    }

    private Bitmap applyTextToBitmap() {
        if (currentText.isEmpty() || currentBitmap == null || currentBitmap.isRecycled()) {
            return null;
        }

        try {
            Bitmap newBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(newBitmap);

            RectF bitmapTextRect = convertViewRectToBitmapRect(textRect);
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(textColor);
            textPaint.setTextSize(textSize);
            textPaint.setTextAlign(Paint.Align.CENTER);

            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textHeight = fm.descent - fm.ascent;
            float textDrawX = bitmapTextRect.centerX();
            float textDrawY = bitmapTextRect.centerY() - textHeight / 2 + textSize;

            canvas.drawText(currentText, textDrawX, textDrawY, textPaint);

            return newBitmap;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error applying text to image", Toast.LENGTH_LONG).show();
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
        if (currentBitmap != null && !currentBitmap.isRecycled() && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
        }
    }
}