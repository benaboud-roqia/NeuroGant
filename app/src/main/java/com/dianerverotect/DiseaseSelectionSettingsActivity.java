package com.dianerverotect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DiseaseSelectionSettingsActivity extends AppCompatActivity {

    private static final String TAG = "DiseaseSelectSettings";
    private static final String PREFS_NAME = "DiseasePrefs";
    private static final String SELECTED_DISEASE = "selected_disease";

    private RadioGroup radioGroupDiseases;
    private RadioButton radioDiabeticNeuropathy;
    private RadioButton radioALS;
    private Button buttonSave;
    private Button buttonCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disease_selection_settings);

        // Initialize views
        radioGroupDiseases = findViewById(R.id.radio_group_diseases);
        radioDiabeticNeuropathy = findViewById(R.id.radio_diabetic_neuropathy);
        radioALS = findViewById(R.id.radio_als);
        buttonSave = findViewById(R.id.button_save);
        buttonCancel = findViewById(R.id.button_cancel);

        // Get current selected disease
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String selectedDisease = settings.getString(SELECTED_DISEASE, DiseaseSelectionActivity.DISEASE_DIABETIC_NEUROPATHY);
        
        // Set the appropriate radio button
        if (selectedDisease.equals(DiseaseSelectionActivity.DISEASE_ALS)) {
            radioALS.setChecked(true);
        } else {
            radioDiabeticNeuropathy.setChecked(true);
        }

        // Set up save button
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSelectedDisease();
            }
        });

        // Set up cancel button
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveSelectedDisease() {
        try {
            // Get selected disease
            String selectedDisease = DiseaseSelectionActivity.DISEASE_DIABETIC_NEUROPATHY;
            if (radioALS.isChecked()) {
                selectedDisease = DiseaseSelectionActivity.DISEASE_ALS;
            }

            // Log the selection for debugging
            Log.d(TAG, "Saving selected disease: " + selectedDisease);

            // Save to preferences
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(SELECTED_DISEASE, selectedDisease);
            editor.apply();

            // Show confirmation
            String diseaseName = selectedDisease.equals(DiseaseSelectionActivity.DISEASE_ALS) 
                    ? getString(R.string.als) 
                    : getString(R.string.diabetic_neuropathy);
            Toast.makeText(this, diseaseName + " " + getString(R.string.selected), Toast.LENGTH_SHORT).show();

            // Return result to calling activity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("disease_changed", true);
            setResult(RESULT_OK, resultIntent);
            
            // COMPLETELY RESTART THE MAIN ACTIVITY
            // This is the most reliable way to ensure proper fragment initialization
            try {
                Log.d(TAG, "Completely restarting MainActivity");
                
                // Create an intent to restart MainActivity
                Intent restartIntent = new Intent(this, MainActivity.class);
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                restartIntent.putExtra("disease_changed", true);
                
                // Start the new MainActivity instance
                startActivity(restartIntent);
                
                // Finish all activities in the stack including this one
                finishAffinity();
            } catch (Exception e) {
                Log.e(TAG, "Error restarting MainActivity: " + e.getMessage());
                // Just finish this activity if we can't restart MainActivity
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving disease selection: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
