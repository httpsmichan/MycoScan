package com.example.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encyclopedia);

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

        String mushroomName = getIntent().getStringExtra("mushroomName");
        if (mushroomName != null) {
            mushroomNameView.setText(mushroomName);
            loadMushroomData(mushroomName);
        }
    }

    private void loadMushroomData(String mushroomName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

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
        if (value != null && !value.toString().trim().isEmpty()) {
            textView.setText(value.toString());
            container.setVisibility(View.VISIBLE);
        } else {
            container.setVisibility(View.GONE);
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
        if (values != null && !values.isEmpty()) {

            List<String> filtered = new ArrayList<>();
            for (String s : values) {
                if (s != null && !s.trim().isEmpty()) {
                    filtered.add(s);
                }
            }

            if (!filtered.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (String s : filtered) {
                    builder.append("• ").append(s).append("\n");
                }
                textView.setText(builder.toString().trim());
                container.setVisibility(View.VISIBLE);
                return;
            }
        }
        container.setVisibility(View.GONE);
    }
}