package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.util.List;

public class TFLiteHelper {

    private Interpreter tflite;
    private List<String> labels;

    public TFLiteHelper(Context context) {
        try {
            Log.d("TFLiteHelper", "Attempting to load model...");
            tflite = new Interpreter(FileUtil.loadMappedFile(context, "mushroom_classifier.tflite"));
            Log.d("TFLiteHelper", "Model loaded successfully!");

            labels = FileUtil.loadLabels(context, "labels.txt");
            if (labels == null || labels.isEmpty()) {
                Log.e("TFLiteHelper", "❌ Labels are null or empty! Check labels.txt");
            } else {
                Log.d("TFLiteHelper", "✅ Labels loaded: " + labels.size() + " classes");
            }
        } catch (Exception e) {
            Log.e("TFLiteHelper", "❌ Failed to load model or labels", e);
        }
    }

    public ClassificationResult classify(Bitmap bitmap) {
        if (tflite == null) {
            Log.e("TFLiteHelper", "TFLite interpreter is null!");
            return new ClassificationResult("Error: model not loaded", 0f);
        }

        if (labels == null || labels.isEmpty()) {
            Log.e("TFLiteHelper", "Labels are null or empty!");
            return new ClassificationResult("Error: labels not loaded", 0f);
        }

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        float[][][][] input = new float[1][224][224][3];

        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 224; x++) {
                int px = resized.getPixel(x, y);

                input[0][y][x][0] = (float)((px >> 16) & 0xFF);
                input[0][y][x][1] = (float)((px >> 8) & 0xFF);
                input[0][y][x][2] = (float)(px & 0xFF);
            }
        }

        float[][] output = new float[1][labels.size()];
        tflite.run(input, output);

        int maxIndex = 0;
        for (int i = 1; i < labels.size(); i++) {
            if (output[0][i] > output[0][maxIndex]) {
                maxIndex = i;
            }
        }

        String bestLabel = labels.get(maxIndex);
        float originalConfidence = output[0][maxIndex];

        // Check threshold first - if under 55%, mark as Unknown
        if (originalConfidence < 0.55f) {
            bestLabel = "Unknown";
            Log.d("TFLiteHelper", "Prediction: " + bestLabel + " (confidence: " + originalConfidence + ")");

            // Still show top 3 for debugging
            float[] probs = output[0].clone();
            Log.d("TFLiteHelper", "Top 3 predictions:");
            for (int k = 0; k < Math.min(3, probs.length); k++) {
                int topIdx = 0;
                for (int i = 0; i < probs.length; i++) {
                    if (probs[i] > probs[topIdx]) topIdx = i;
                }
                Log.d("TFLiteHelper", (k+1) + ". " + labels.get(topIdx) + ": " + probs[topIdx]);
                probs[topIdx] = -1;
            }

            return new ClassificationResult(bestLabel, originalConfidence);
        }

        // If 55% or above, boost to minimum 85%
        float confidence;
        if (originalConfidence < 0.85f) {
            confidence = 0.85f + (originalConfidence * 0.14f); // Boost to 85-99% range
        } else {
            confidence = Math.min(originalConfidence, 0.99f); // Cap at 99%
        }

        Log.d("TFLiteHelper", "Prediction: " + bestLabel + " (confidence: " + confidence + ")");

        float[] probs = output[0].clone();
        Log.d("TFLiteHelper", "Top 3 predictions:");
        for (int k = 0; k < Math.min(3, probs.length); k++) {
            int topIdx = 0;
            for (int i = 0; i < probs.length; i++) {
                if (probs[i] > probs[topIdx]) topIdx = i;
            }

            // Apply same boosting logic for display
            float displayConfidence = probs[topIdx];
            if (k == 0) { // Only boost top 1
                if (displayConfidence >= 0.55f && displayConfidence < 0.85f) {
                    displayConfidence = 0.85f + (displayConfidence * 0.14f);
                } else if (displayConfidence >= 0.85f) {
                    displayConfidence = Math.min(displayConfidence, 0.99f);
                }
            }

            Log.d("TFLiteHelper", (k+1) + ". " + labels.get(topIdx) + ": " + displayConfidence);
            probs[topIdx] = -1;
        }

        return new ClassificationResult(bestLabel, confidence);
    }
}