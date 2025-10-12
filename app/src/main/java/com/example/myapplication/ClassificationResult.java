package com.example.myapplication;

public class ClassificationResult {
    public final String label;
    public final float confidence;

    public ClassificationResult(String label, float confidence) {
        this.label = label;
        this.confidence = confidence;
    }
}
