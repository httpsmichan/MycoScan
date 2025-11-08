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
            tflite = new Interpreter(FileUtil.loadMappedFile(context, "mycoscan_model.tflite"));
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
                input[0][y][x][0] = ((px >> 16) & 0xFF) / 255.0f;
                input[0][y][x][1] = ((px >> 8) & 0xFF) / 255.0f;
                input[0][y][x][2] = (px & 0xFF) / 255.0f;
            }
        }

        float[][] output = new float[1][labels.size()];
        tflite.run(input, output);

        int maxIndex = 0;
        for (int i = 1; i < labels.size(); i++) {
            if (output[0][i] > output[0][maxIndex]) maxIndex = i;
        }

        String bestLabel = labels.get(maxIndex);
        float confidence = output[0][maxIndex];

        if (confidence < 0.8f) {
            bestLabel = "Unknown";
        }

        if (!bestLabel.equals("Unknown")) {
            bestLabel = bestLabel.replaceAll("^\\d+\\s*", "");
        }

        Log.d("TFLiteHelper", "Prediction: " + bestLabel + " (confidence: " + confidence + ")");

        return new ClassificationResult(bestLabel, confidence);
    }

}
