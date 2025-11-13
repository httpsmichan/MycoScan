package com.example.myapplication;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FavoritesFragment extends Fragment {

    private static final String TAG = "FavoritesFragment";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private PreviewView previewView;
    private Button btnTakePhoto;
    private ImageButton btnToggleCamera, btnFlash;
    private MaterialButton btnCameraMode;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    private TFLiteHelper tfliteHelper;
    private CameraOverlay cameraOverlay;
    private Camera camera;
    private boolean isFlashOn = false;
    private static final int REQUEST_CODE_PICK_IMAGE = 20;

    private FirebaseFirestore db;

    private boolean isScanMode = true;

    public FavoritesFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_favorites_fragment, container, false);

        previewView = view.findViewById(R.id.previewView);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        cameraOverlay = view.findViewById(R.id.cameraOverlay);
        btnFlash = view.findViewById(R.id.btnFlash);
        btnToggleCamera = view.findViewById(R.id.btnToggleCamera);
        btnCameraMode = view.findViewById(R.id.btnCameraMode);

        tfliteHelper = new TFLiteHelper(getContext());
        db = FirebaseFirestore.getInstance();

        ImageButton btnPickGallery = view.findViewById(R.id.btnPickGallery);

        Uri lastImageUri = getLastImageUri();
        if (lastImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), lastImageUri);
                btnPickGallery.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        btnPickGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        });

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(getActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        btnToggleCamera.setOnClickListener(v -> toggleCamera());
        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnFlash.setOnClickListener(v -> toggleFlashMode());
        btnCameraMode.setOnClickListener(v -> toggleCameraMode());

        FrameLayout scanningTipsContainer = view.findViewById(R.id.scanningTipsContainer);
        TextView scanningTipsText = view.findViewById(R.id.scanningTipsText);
        LinearLayout scanningTipsHeader = view.findViewById(R.id.scanningTipsHeader);

        scanningTipsContainer.getBackground().setAlpha(0);

        scanningTipsHeader.setOnClickListener(v -> {
            if (scanningTipsText.getVisibility() == View.GONE) {
                scanningTipsText.setVisibility(View.VISIBLE);
                scanningTipsContainer.getBackground().setAlpha(178);
            } else {
                scanningTipsText.setVisibility(View.GONE);
                scanningTipsContainer.getBackground().setAlpha(0);
            }
        });

        return view;
    }

    /**
     * Toggle between SCAN and CAPTURE modes
     */
    private void toggleCameraMode() {
        isScanMode = !isScanMode;

        if (isScanMode) {
            btnCameraMode.setText("SCAN");
            btnCameraMode.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_dark));
            showCustomToast("Scan mode");
        } else {
            btnCameraMode.setText("CAPTURE");
            btnCameraMode.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_blue_dark));
            showCustomToast("Capture mode");
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        imageCapture = new ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == getActivity().RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);

                    // Direct classification - no cropping
                    ClassificationResult result = tfliteHelper.classify(bitmap);
                    handleClassificationResult(result, imageUri);

                } catch (IOException e) {
                    e.printStackTrace();
                    showCustomToast("Failed to load image");
                }
            }
        }
    }

    /**
     * Helper method to handle classification results from gallery
     */
    private void handleClassificationResult(ClassificationResult result, Uri imageUri) {
        Log.d(TAG, "Predicted: " + result.label + ", Confidence: " + result.confidence);
        logScanToFirestore(result.label, result.confidence);

        if (result.label.toLowerCase().contains("unknown")) {
            showCustomToast("This object is not a mushroom species");
        } else {
            Intent intent = new Intent(getContext(), ResultActivity.class);
            intent.putExtra("photoUri", imageUri.toString());
            intent.putExtra("prediction", result.label);
            intent.putExtra("confidence", result.confidence);
            intent.putExtra("confidencePercentage", result.confidence * 100);
            startActivity(intent);
        }
    }

    private void toggleFlashMode() {
        if (imageCapture == null) return;

        int currentMode = imageCapture.getFlashMode();
        int newMode;

        switch (currentMode) {
            case ImageCapture.FLASH_MODE_OFF:
                newMode = ImageCapture.FLASH_MODE_ON;
                btnFlash.setImageResource(R.drawable.ic_flash_on);
                break;
            case ImageCapture.FLASH_MODE_ON:
                newMode = ImageCapture.FLASH_MODE_AUTO;
                btnFlash.setImageResource(R.drawable.ic_flash_auto);
                break;
            default:
                newMode = ImageCapture.FLASH_MODE_OFF;
                btnFlash.setImageResource(R.drawable.ic_flash_off);
                break;
        }

        imageCapture.setFlashMode(newMode);
    }

    private void toggleCamera() {
        cameraSelector = (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;
        startCamera();
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        if (isScanMode) {
            takeScanPhoto();
        } else {
            takeCapturePhoto();
        }
    }

    /**
     * SCAN MODE: Take photo, predict, but don't save to gallery
     */
    /**
     * SCAN MODE: Take photo, predict, but don't save to gallery
     */
    private void takeScanPhoto() {
        File tempFile = new File(requireContext().getCacheDir(), "temp_scan_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(tempFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(getContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Scan failed: " + exception.getMessage(), exception);
                        showCustomToast("Scan failed!");
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        try {
                            FileInputStream fis = new FileInputStream(tempFile);
                            Bitmap bitmap = BitmapFactory.decodeStream(fis);
                            fis.close();

                            // Direct classification
                            ClassificationResult result = tfliteHelper.classify(bitmap);
                            handleScanResult(result, bitmap, tempFile);

                        } catch (IOException e) {
                            e.printStackTrace();
                            showCustomToast("Failed to process scan");
                            tempFile.delete();
                        }
                    }
                });
    }

    /**
     * CAPTURE MODE: Save photo to gallery without prediction
     */
    /**
     * Helper method to handle classification results from camera scan
     */
    private void handleScanResult(ClassificationResult result, Bitmap bitmap, File tempFile) {
        Log.d(TAG, "Scan - Predicted: " + result.label + ", Confidence: " + result.confidence);
        logScanToFirestore(result.label, result.confidence);

        tempFile.delete();

        if (result.label.toLowerCase().contains("unknown")) {
            showCustomToast("This object is unknown");
        } else {
            try {
                String tempPath = saveBitmapToCache(bitmap);
                Intent intent = new Intent(getContext(), ResultActivity.class);
                intent.putExtra("photoUri", "file://" + tempPath);
                intent.putExtra("prediction", result.label);
                intent.putExtra("confidence", result.confidence);
                intent.putExtra("confidencePercentage", result.confidence * 100);
                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
                showCustomToast("Failed to process scan");
            }
        }
    }

    private void takeCapturePhoto() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MushroomApp");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(
                        requireContext().getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(getContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        showCustomToast("Photo capture failed!");
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = output.getSavedUri();
                        showCustomToast("Photo saved to Gallery!");
                        Log.d(TAG, "Photo captured and saved: " + savedUri);
                    }
                });
    }

    private String saveBitmapToCache(Bitmap bitmap) throws IOException {
        File cacheFile = new File(requireContext().getCacheDir(), "result_" + System.currentTimeMillis() + ".jpg");
        java.io.FileOutputStream out = new java.io.FileOutputStream(cacheFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        out.flush();
        out.close();
        return cacheFile.getAbsolutePath();
    }

    /**
     * Logs a mushroom scan to Firestore
     * Creates a document in the "scanned" subcollection with timestamp and predicted label
     * Also increments the total scan counter
     */
    private void logScanToFirestore(String predictedLabel, float confidence) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User not logged in, cannot log scan to Firestore");
            return;
        }

        String userId = user.getUid();

        String verifiedStatus;
        if ("Unknown".equalsIgnoreCase(predictedLabel)) {
            verifiedStatus = "not verified";
        } else if (confidence >= 85.0f) {
            verifiedStatus = "verified";
        } else {
            verifiedStatus = "unreliable";
        }

        Map<String, Object> scanData = new HashMap<>();
        scanData.put("timestamp", FieldValue.serverTimestamp());
        scanData.put("predictedLabel", predictedLabel);
        scanData.put("confidence", confidence);
        scanData.put("verified", verifiedStatus);

        db.collection("users")
                .document(userId)
                .collection("scanned")
                .add(scanData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Scan logged successfully with ID: " + documentReference.getId());

                    db.collection("users")
                            .document(userId)
                            .update("scanned", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Scan counter incremented successfully"))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error incrementing scan counter", e);
                                Map<String, Object> counterData = new HashMap<>();
                                counterData.put("scanned", 1);
                                db.collection("users").document(userId)
                                        .set(counterData, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener(aVoid2 -> Log.d(TAG, "Scan counter initialized"))
                                        .addOnFailureListener(e2 -> Log.e(TAG, "Error initializing scan counter", e2));
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error logging scan to Firestore", e);
                    showCustomToast("Failed to log scan");
                });
    }

    private void classifyPhoto(File photoFile) {
        try {
            FileInputStream fis = new FileInputStream(photoFile);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();

            ClassificationResult result = tfliteHelper.classify(bitmap);

            Intent intent = new Intent(getContext(), ResultActivity.class);
            intent.putExtra("photoPath", photoFile.getAbsolutePath());
            intent.putExtra("prediction", result.label);
            intent.putExtra("confidence", result.confidence);
            startActivity(intent);

        } catch (IOException e) {
            e.printStackTrace();
            showCustomToast("Failed to load photo for classification");
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                showCustomToast("Permissions not granted by the user.");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProviderFuture != null) {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error shutting down camera", e);
            }
        }
    }

    private Uri getLastImageUri() {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED };
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, sortOrder + " LIMIT 1")) {
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                long id = cursor.getLong(idIndex);
                return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showCustomToast(String message) {
        if (!isAdded()) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View layout = inflater.inflate(R.layout.custom_toast, null);

        TextView toastText = layout.findViewById(R.id.toast_text);
        toastText.setText(message);

        Toast toast = new Toast(requireContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

}