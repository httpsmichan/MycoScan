package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.List;

public class ObjectDetectionHelper {

    private final ObjectDetector objectDetector;
    private static final String TAG = "ObjectDetectionHelper";

    public ObjectDetectionHelper() {
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .build();

        objectDetector = ObjectDetection.getClient(options);
    }

    public interface DetectionCallback {
        void onObjectDetected(Bitmap cropped);
        void onNoObjectDetected();
    }

    public void detectAndCrop(Bitmap bitmap, DetectionCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        objectDetector.process(image)
                .addOnSuccessListener(detectedObjects -> {
                    if (detectedObjects.isEmpty()) {
                        Log.w(TAG, "⚠️ No objects detected, using FULL original image.");
                        callback.onNoObjectDetected();
                        return;
                    }

                    // Pick the largest detected object
                    DetectedObject bestObject = detectedObjects.get(0);
                    for (DetectedObject obj : detectedObjects) {
                        int area = obj.getBoundingBox().width() * obj.getBoundingBox().height();
                        int bestArea = bestObject.getBoundingBox().width() * bestObject.getBoundingBox().height();
                        if (area > bestArea) {
                            bestObject = obj;
                        }
                    }

                    Rect box = bestObject.getBoundingBox();

                    // Calculate crop area with padding
                    int padding = 20; // Add 20px padding around detected object
                    int left = Math.max(box.left - padding, 0);
                    int top = Math.max(box.top - padding, 0);
                    int right = Math.min(box.right + padding, bitmap.getWidth());
                    int bottom = Math.min(box.bottom + padding, bitmap.getHeight());
                    int width = right - left;
                    int height = bottom - top;

                    if (width <= 0 || height <= 0) {
                        Log.w(TAG, "⚠️ Invalid crop dimensions, using full image");
                        callback.onNoObjectDetected();
                        return;
                    }

                    Bitmap cropped = Bitmap.createBitmap(bitmap, left, top, width, height);
                    Log.d(TAG, "✅ Object detected & cropped: " + width + "x" + height);
                    callback.onObjectDetected(cropped);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Object detection failed, using full image", e);
                    callback.onNoObjectDetected();
                });
    }
}
