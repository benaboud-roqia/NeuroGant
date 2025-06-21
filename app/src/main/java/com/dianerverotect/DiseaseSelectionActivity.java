package com.dianerverotect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;

public class DiseaseSelectionActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "DiseasePrefs";
    private static final String SELECTED_DISEASE = "selected_disease";
    public static final String DISEASE_DIABETIC_NEUROPATHY = "diabetic_neuropathy";
    public static final String DISEASE_ALS = "als";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_disease_selection);

        // Check if disease is already selected
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String selectedDisease = settings.getString(SELECTED_DISEASE, null);
        
        // If disease is already selected, skip this activity
        if (selectedDisease != null) {
            startMainActivity();
            finish();
            return;
        }

        // Set up click listeners for disease selection cards
        CardView cardDiabeticNeuropathy = findViewById(R.id.card_diabetic_neuropathy);
        CardView cardALS = findViewById(R.id.card_als);

        cardDiabeticNeuropathy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDisease(DISEASE_DIABETIC_NEUROPATHY);
            }
        });

        cardALS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDisease(DISEASE_ALS);
            }
        });
    }

    private void selectDisease(String disease) {
        // Save the selected disease
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SELECTED_DISEASE, disease);
        editor.apply();

        // Show confirmation toast
        String diseaseName = disease.equals(DISEASE_DIABETIC_NEUROPATHY) 
                ? getString(R.string.diabetic_neuropathy) 
                : getString(R.string.als);
        Toast.makeText(this, diseaseName + " " + getString(R.string.selected), Toast.LENGTH_SHORT).show();

        // Start the main activity
        startMainActivity();
        finish();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
