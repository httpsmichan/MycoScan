package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CameraOverlay extends View {

    private Paint borderPaint;
    private Paint dimPaint;
    private Paint scanPaint;
    private RectF cropRectF;
    private final float cornerRadius = 30f;
    private float scanY;
    private final float scanSpeed = 5f;

    public CameraOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        dimPaint = new Paint();
        dimPaint.setColor(Color.parseColor("#66000000"));
        dimPaint.setStyle(Paint.Style.FILL);
        dimPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        scanPaint = new Paint();
        scanPaint.setColor(Color.GREEN);
        scanPaint.setStrokeWidth(2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        int cropWidth = (int) (width * 0.9);
        int cropHeight = (int) (cropWidth * 1.37);
        float left = (width - cropWidth) / 2f;
        float top = (height - cropHeight) / 2f;
        float right = left + cropWidth;
        float bottom = top + cropHeight;

        cropRectF = new RectF(left, top, right, bottom);

        canvas.drawRect(0, 0, width, height, dimPaint);

        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawRoundRect(cropRectF, cornerRadius, cornerRadius, clearPaint);

        Paint outerBorder = new Paint();
        outerBorder.setColor(Color.parseColor("#80FFFFFF"));
        outerBorder.setStyle(Paint.Style.STROKE);
        outerBorder.setStrokeWidth(1f);

        float outerWidthOffset = 30f;
        float outerHeightOffset = 40f;
        RectF outerRect = new RectF(
                cropRectF.left - outerWidthOffset,
                cropRectF.top - outerHeightOffset,
                cropRectF.right + outerWidthOffset,
                cropRectF.bottom + outerHeightOffset
        );
        canvas.drawRoundRect(outerRect, cornerRadius + 5f, cornerRadius + 5f, outerBorder);

        canvas.drawRoundRect(cropRectF, cornerRadius, cornerRadius, borderPaint);

        Paint cornerPaint = new Paint();
        cornerPaint.setColor(Color.GREEN);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(2f);

        RectF topLeftArc = new RectF(left, top, left + 2 * cornerRadius, top + 2 * cornerRadius);
        RectF topRightArc = new RectF(right - 2 * cornerRadius, top, right, top + 2 * cornerRadius);
        RectF bottomLeftArc = new RectF(left, bottom - 2 * cornerRadius, left + 2 * cornerRadius, bottom);
        RectF bottomRightArc = new RectF(right - 2 * cornerRadius, bottom - 2 * cornerRadius, right, bottom);

        canvas.drawArc(topLeftArc, 180, 90, false, cornerPaint);
        canvas.drawArc(topRightArc, 270, 90, false, cornerPaint);
        canvas.drawArc(bottomLeftArc, 90, 90, false, cornerPaint);
        canvas.drawArc(bottomRightArc, 0, 90, false, cornerPaint);

        if (scanY == 0) scanY = cropRectF.top;
        canvas.drawLine(cropRectF.left, scanY, cropRectF.right, scanY, scanPaint);

        scanY += scanSpeed;
        if (scanY > cropRectF.bottom) scanY = cropRectF.top;

        postInvalidateOnAnimation();
    }

    public RectF getCropRectF() {
        return cropRectF;
    }
}
