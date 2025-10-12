package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class TermsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "TermsPrefs";
    private static final String KEY_TERMS_ACCEPTED = "termsAccepted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean termsAccepted = prefs.getBoolean(KEY_TERMS_ACCEPTED, false);

        if (termsAccepted) {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_terms);

        TextView termsText = findViewById(R.id.termsText);
        AppCompatButton btnAccept = findViewById(R.id.btnAccept);
        AppCompatButton btnDecline = findViewById(R.id.btnDecline);

        termsText.setText(getString(R.string.terms_text));

        btnAccept.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_TERMS_ACCEPTED, true);
            editor.apply();

            Intent intent = new Intent(TermsActivity.this, SignupActivity.class);
            startActivity(intent);
            finish();
        });

        if (btnDecline != null) {
            btnDecline.setOnClickListener(v -> {
                Intent intent = new Intent(TermsActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
}