package com.example.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EncyclopediaActivity extends AppCompatActivity {

    private TextView mushroomNameView, mushroomEdibility, resultDescription, resultHabitat,
            resultFirst, resultSecond, resultCulinary, resultMedicinal,
            resultToxicity, resultSymptoms, resultDuration, resultLongTerm, resultFacts;

    private LinearLayout edibilityContainer, descriptionContainer, habitatContainer, charLayout,
            culinaryContainer, medicinalContainer, toxicityContainer, symptomsContainer,
            durationContainer, longTermContainer, factsContainer;

    private ViewPager2 imagePager;
    private TabLayout imageIndicator;
    private MushroomImageAdapter imageAdapter;

    private FirebaseFirestore db;
    private String currentUserId;
    private View rootView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encyclopedia);

        rootView = findViewById(android.R.id.content);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        mushroomNameView = findViewById(R.id.mushroomName);
        mushroomEdibility = findViewById(R.id.mushroomEdibility);
        resultDescription = findViewById(R.id.resultDescription);
        resultHabitat = findViewById(R.id.resultHabitat);
        resultFirst = findViewById(R.id.resultFirst);
        resultSecond = findViewById(R.id.resultSecond);
        resultCulinary = findViewById(R.id.resultCulinary);
        resultMedicinal = findViewById(R.id.resultMedicinal);
        resultToxicity = findViewById(R.id.resultToxicity);
        resultSymptoms = findViewById(R.id.resultSymptoms);
        resultDuration = findViewById(R.id.resultDuration);
        resultLongTerm = findViewById(R.id.resultLongTerm);
        resultFacts = findViewById(R.id.resultFacts);

        edibilityContainer = findViewById(R.id.edibilityContainer);
        descriptionContainer = findViewById(R.id.descriptionContainer);
        habitatContainer = findViewById(R.id.habitatContainer);
        charLayout = findViewById(R.id.charLayout);
        culinaryContainer = findViewById(R.id.culinaryContainer);
        medicinalContainer = findViewById(R.id.medicinalContainer);
        toxicityContainer = findViewById(R.id.toxicityContainer);
        symptomsContainer = findViewById(R.id.symptomsContainer);
        durationContainer = findViewById(R.id.durationContainer);
        longTermContainer = findViewById(R.id.longTermContainer);
        factsContainer = findViewById(R.id.factsContainer);

        imagePager = findViewById(R.id.mushroomImagePager);
        imageIndicator = findViewById(R.id.imageIndicator);

        ImageButton fab = findViewById(R.id.fab);
        fab.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private int lastAction;
            private int screenWidth, screenHeight;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (screenWidth == 0 || screenHeight == 0) {
                    screenWidth = ((View) view.getParent()).getWidth();
                    screenHeight = ((View) view.getParent()).getHeight();
                }

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        lastAction = MotionEvent.ACTION_DOWN;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        newY = Math.max(0, Math.min(newY, screenHeight - view.getHeight()));

                        view.setX(newX);
                        view.setY(newY);
                        lastAction = MotionEvent.ACTION_MOVE;
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (lastAction == MotionEvent.ACTION_DOWN) {

                            showNoteBottomSheet();
                        } else {

                            float middle = screenWidth / 2f;
                            float targetX = (view.getX() < middle) ? 0 : screenWidth - view.getWidth();

                            view.animate()
                                    .x(targetX)
                                    .setDuration(200)
                                    .start();
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });

        String mushroomName = getIntent().getStringExtra("mushroomName");
        if (mushroomName != null) {
            mushroomNameView.setText(mushroomName);
            loadMushroomData(mushroomName);
        }
    }

    private void loadMushroomData(String mushroomName) {
        db.collection("mushroom-encyclopedia")
                .whereEqualTo("mushroomName", mushroomName)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                        String edibility = doc.getString("edibility");
                        String reason = doc.getString("reason");
                        updateEdibilityUI(edibility, reason);

                        setTextOrHide(doc.get("description"), resultDescription, descriptionContainer);

                        setArrayOrHide((List<String>) doc.get("habitats"), resultHabitat, habitatContainer);

                        List<String> characteristics = (List<String>) doc.get("characteristics");
                        if (characteristics != null && !characteristics.isEmpty()) {
                            resultFirst.setText(characteristics.size() > 0 ? characteristics.get(0) : "");
                            resultSecond.setText(characteristics.size() > 1 ? characteristics.get(1) : "");
                        } else {
                            charLayout.setVisibility(View.GONE);
                        }

                        setArrayOrHide((List<String>) doc.get("culinaryUses"), resultCulinary, culinaryContainer);

                        setArrayOrHide((List<String>) doc.get("medicinalUses"), resultMedicinal, medicinalContainer);

                        setTextOrHide(doc.get("toxicity"), resultToxicity, toxicityContainer);

                        setArrayOrHide((List<String>) doc.get("onset"), resultSymptoms, symptomsContainer);

                        setTextOrHide(doc.get("duration"), resultDuration, durationContainer);

                        setTextOrHide(doc.get("longTerm"), resultLongTerm, longTermContainer);

                        setArrayOrHide((List<String>) doc.get("funFacts"), resultFacts, factsContainer);

                        List<String> images = (List<String>) doc.get("images");
                        if (images != null && !images.isEmpty()) {
                            setupImageCarousel(images);
                        } else {
                            imagePager.setVisibility(View.GONE);
                            imageIndicator.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void setupImageCarousel(List<String> images) {
        imageAdapter = new MushroomImageAdapter(this, images);
        imagePager.setAdapter(imageAdapter);

        imagePager.setOffscreenPageLimit(1);
        imagePager.setClipToPadding(false);
        imagePager.setClipChildren(false);

        int horizontalPadding = getResources().getDimensionPixelOffset(R.dimen.viewpager_page_offset);
        int verticalPadding = (int) (5 * getResources().getDisplayMetrics().density);
        imagePager.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        imagePager.setPageTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(View page, float position) {
                float absPos = Math.abs(position);

                page.setScaleY(1f - (absPos * 0.15f));

                page.setAlpha(1f - (absPos * 0.3f));
            }
        });

        new TabLayoutMediator(imageIndicator, imagePager, (tab, position) -> {

        }).attach();

        imagePager.setVisibility(View.VISIBLE);
        imageIndicator.setVisibility(View.VISIBLE);
    }

    private void setTextOrHide(Object value, TextView textView, LinearLayout container) {
        if (value == null) {
            container.setVisibility(View.GONE);
            return;
        }

        String text = value.toString().trim();
        if (text.isEmpty() || text.equalsIgnoreCase("null")) {
            container.setVisibility(View.GONE);
        } else {
            textView.setText(text);
            container.setVisibility(View.VISIBLE);
        }
    }


    @SuppressLint("SetTextI18n")
    private void updateEdibilityUI(String edibility, String reason) {
        if (edibility == null || edibility.trim().isEmpty()) {
            edibility = "unknown";
        }

        String displayValue;
        int bgColor = Color.TRANSPARENT;
        int borderColor = Color.TRANSPARENT;

        switch (edibility.toLowerCase()) {
            case "edible":
                displayValue = "Edible";
                bgColor = Color.parseColor("#804CAF50");
                borderColor = Color.parseColor("#4CAF50");
                break;
            case "poisonous":
                displayValue = "Poisonous";
                bgColor = Color.parseColor("#80D11406");
                borderColor = Color.parseColor("#D11406");
                break;
            case "ediblew":
                displayValue = "Edible with Caution";
                bgColor = Color.parseColor("#80FFC107");
                borderColor = Color.parseColor("#FFC107");
                break;
            case "inediblemed":
                displayValue = "Inedible (Medicinal)";
                bgColor = Color.parseColor("#80857D7D");
                borderColor = Color.parseColor("#857D7D");
                break;
            case "unknown":
                displayValue = "Unknown";
                bgColor = Color.parseColor("#80808080");
                borderColor = Color.parseColor("#A0A0A0");
                break;
            default:
                displayValue = edibility;
                break;
        }

        if (reason != null && !reason.isEmpty()) {
            String combined = displayValue + ": " + reason;
            mushroomEdibility.setText(combined);
        } else {
            mushroomEdibility.setText(displayValue);
        }

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(20f);
        drawable.setStroke(2, borderColor);
        edibilityContainer.setBackground(drawable);
    }

    private void setArrayOrHide(List<String> values, TextView textView, LinearLayout container) {
        if (values == null || values.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        List<String> filtered = new ArrayList<>();
        for (String s : values) {
            if (s != null && !s.trim().isEmpty() && !s.equalsIgnoreCase("null")) {
                filtered.add(s.trim());
            }
        }

        if (!filtered.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String s : filtered) {
                builder.append("• ").append(s).append("\n");
            }
            textView.setText(builder.toString().trim());
            container.setVisibility(View.VISIBLE);
        } else {
            container.setVisibility(View.GONE);
        }
    }

    private void showNoteBottomSheet() {
        if (rootView == null || isFinishing()) return;

        View sheetView = getLayoutInflater().inflate(R.layout.bottomsheet_note, null);
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        bottomSheetDialog.setContentView(sheetView);

        EditText etTitle = sheetView.findViewById(R.id.etDialogTitleInput);
        EditText etNote = sheetView.findViewById(R.id.etDialogNote);
        Button btnSave = sheetView.findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String text = etNote.getText().toString().trim();

            if (title.isEmpty() || text.isEmpty()) {
                showCustomToast("Title and note cannot be empty");
                return;
            }

            saveNoteToFirebase(title, text);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void saveNoteToFirebase(String title, String text) {
        if (currentUserId == null) {
            showCustomToast("Login required to save notes");
            return;
        }

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("title", title);
        noteData.put("text", text);
        noteData.put("date", com.google.firebase.Timestamp.now());

        db.collection("users")
                .document(currentUserId)
                .collection("notes")
                .add(noteData)
                .addOnSuccessListener(docRef -> {
                    showCustomToast("Note saved!");
                    Log.d("EncyclopediaActivity", "Note saved with ID: " + docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("EncyclopediaActivity", "Failed to save note", e);
                    showCustomToast("Failed to save note");
                });
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.toast_root));

        TextView toastText = layout.findViewById(R.id.toast_text);
        toastText.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}