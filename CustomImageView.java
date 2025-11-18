package com.example.edit_ease_test;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public class CustomImageView extends AppCompatImageView {
    private RectF cropRect = new RectF();
    private Paint gridPaint;
    private Paint borderPaint;
    private boolean showGrid = false;

    private boolean isAddingText = false;
    private String currentText = "";
    private RectF textRect = new RectF();
    private Paint textPaint;
    private Paint textBackgroundPaint;
    private Paint handlePaint;
    private boolean textAdded = false;

    public CustomImageView(Context context) {
        super(context);
        init();
    }

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        borderPaint.setAntiAlias(true);

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

    public void setCropRect(RectF rect) {
        this.cropRect = rect;
    }

    public void setGridPaint(Paint paint) {
        this.gridPaint = paint;
    }

    public void setShowGrid(boolean show) {
        this.showGrid = show;
    }

    public void setTextMode(boolean adding, String text, RectF rect, boolean added) {
        this.isAddingText = adding;
        this.currentText = text;
        this.textRect.set(rect);
        this.textAdded = added;
    }

    public void setTextPaints(Paint textPaint, Paint textBackgroundPaint, Paint handlePaint) {
        this.textPaint = textPaint;
        this.textBackgroundPaint = textBackgroundPaint;
        this.handlePaint = handlePaint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (showGrid && cropRect.width() > 0 && cropRect.height() > 0) {
            canvas.drawRect(cropRect, borderPaint);

            if (gridPaint != null) {
                float cellWidth = cropRect.width() / 3;
                float cellHeight = cropRect.height() / 3;

                for (int i = 1; i < 3; i++) {
                    float x = cropRect.left + cellWidth * i;
                    canvas.drawLine(x, cropRect.top, x, cropRect.bottom, gridPaint);
                }

                for (int i = 1; i < 3; i++) {
                    float y = cropRect.top + cellHeight * i;
                    canvas.drawLine(cropRect.left, y, cropRect.right, y, gridPaint);
                }
            }
        }

        if (isAddingText && !textAdded && !currentText.isEmpty()) {
            canvas.drawRect(textRect, textBackgroundPaint);

            float handleRadius = 15f;
            float[] handleX = {textRect.left, textRect.right, textRect.right, textRect.left};
            float[] handleY = {textRect.top, textRect.top, textRect.bottom, textRect.bottom};

            for (int i = 0; i < 4; i++) {
                canvas.drawCircle(handleX[i], handleY[i], handleRadius, handlePaint);
            }

            if (textPaint != null) {
                float textWidth = textRect.width() - 20;
                float textHeight = textRect.height() - 20;
                float textSize = Math.min(textWidth / 2f, textHeight);
                if (textSize > 20) textPaint.setTextSize(textSize);
                else textPaint.setTextSize(20);

                canvas.drawText(currentText, textRect.left + 10, textRect.top + textPaint.getTextSize() + 10, textPaint);
            }
        } else if (textAdded && !currentText.isEmpty()) {
            if (textPaint != null) {
                float textWidth = textRect.width() - 20;
                float textHeight = textRect.height() - 20;
                float textSize = Math.min(textWidth / 2f, textHeight);
                if (textSize > 20) textPaint.setTextSize(textSize);
                else textPaint.setTextSize(20);

                canvas.drawText(currentText, textRect.left + 10, textRect.top + textPaint.getTextSize() + 10, textPaint);
            }
        }
    }
}