package com.dianerverotect;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Import the correct Document class from iText
import com.itextpdf.text.Document;
import com.bumptech.glide.Glide;
import com.dianerverotect.model.NeuropathyPredictor;
import com.dianerverotect.model.RecommendationAdapter;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;

import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView greetingNameText;
    private EditText glucoseValueInput;
    private Button getStartedButton;
    private NestedScrollView nestedScrollView;
    
    // Test section views
    private TextView testSectionTitle;
    private CardView emgChartCard, question1Card, question2Card;
    private LineChart emgChart;
    private Button yesButton1, noButton1, yesButton2, noButton2, analyzeResultsButton;
    
    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;
    
    // Constants for the test
    private static final int COUNTDOWN_SECONDS = 20;
    private static final int COUNTDOWN_INTERVAL = 1000; // 1 second
    
    // Test results
    private boolean temperatureResponse = false;
    private boolean pressureResponse = false;
    private AlertDialog dialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        greetingNameText = view.findViewById(R.id.text_greeting_name);
        glucoseValueInput = view.findViewById(R.id.text_glucose_value);
        getStartedButton = view.findViewById(R.id.button_get_started);
        nestedScrollView = view.findViewById(R.id.nested_scroll_view);
        
        // Initialize test section views
        testSectionTitle = view.findViewById(R.id.text_emg_section_title);
        emgChartCard = view.findViewById(R.id.card_emg_chart);
        question1Card = view.findViewById(R.id.card_question1);
        question2Card = view.findViewById(R.id.card_question2);
        emgChart = view.findViewById(R.id.emg_chart);
        
        yesButton1 = view.findViewById(R.id.yes_button1);
        noButton1 = view.findViewById(R.id.no_button1);
        yesButton2 = view.findViewById(R.id.yes_button2);
        noButton2 = view.findViewById(R.id.no_button2);
        analyzeResultsButton = view.findViewById(R.id.analyze_results_button);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Load user data
        loadUsername();
        
        // Set up button click listeners
        setupButtonListeners();
        
        // Set up EMG chart
        setupEmgChart();

        return view;
    }

    private void loadUsername() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("name").exists()) {
                    String username = snapshot.child("name").getValue(String.class);
                    greetingNameText.setText(username);
                } else if (snapshot.child("fullName").exists()) {
                    // Fallback to fullName if name doesn't exist
                    String username = snapshot.child("fullName").getValue(String.class);
                    greetingNameText.setText(username);
                } else {
                    // If neither name nor fullName exists, use the email as fallback
                    String email = mAuth.getCurrentUser().getEmail();
                    if (email != null) {
                        // Extract username part from email
                        String username = email.substring(0, email.indexOf('@'));
                        greetingNameText.setText(username);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Failed to load user data", error.toException());
            }
        });
    }
    
    private boolean validateGlucoseInput() {
        String glucoseValue = glucoseValueInput.getText().toString().trim();
        if (TextUtils.isEmpty(glucoseValue)) {
            Toast.makeText(getContext(), "Please enter your glucose value", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            float value = Float.parseFloat(glucoseValue);
            if (value <= 0 || value > 500) { // Reasonable glucose range check
                Toast.makeText(getContext(), "Please enter a valid glucose value", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void saveGlucoseValue() {
        if (mAuth.getCurrentUser() == null) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        String glucoseValue = glucoseValueInput.getText().toString().trim();
        
        // Save glucose value with timestamp
        long timestamp = System.currentTimeMillis();
        GlucoseReading reading = new GlucoseReading(Float.parseFloat(glucoseValue), timestamp);
        
        usersRef.child(userId).child("glucoseReadings").child(String.valueOf(timestamp))
                .setValue(reading)
                .addOnSuccessListener(aVoid -> {
                    // Successfully saved
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save glucose reading", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showCountdownDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_countdown, null);
        TextView countdownText = dialogView.findViewById(R.id.text_countdown);
        ImageView gifImageView = dialogView.findViewById(R.id.gif_animation);
        Glide.with(this)
                .load(R.drawable.handcrush) // if GIF is in res/raw
                .into(gifImageView);
        
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        dialog = builder.create();
        dialog.show();
        
        new CountDownTimer(COUNTDOWN_SECONDS * 1000, COUNTDOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                countdownText.setText(String.valueOf(secondsRemaining));
            }
            
            @Override
            public void onFinish() {
                dialog.dismiss();
                showTestSection();
            }
        }.start();
    }
    
    private void showTestSection() {
        // Make test section visible
        testSectionTitle.setVisibility(View.VISIBLE);
        emgChartCard.setVisibility(View.VISIBLE);
        question1Card.setVisibility(View.VISIBLE);
        question2Card.setVisibility(View.VISIBLE);
        analyzeResultsButton.setVisibility(View.VISIBLE);
        
        // Generate sample EMG data
        generateSampleEmgData();
        
        // Scroll to the test section
        new Handler().postDelayed(() -> {
            nestedScrollView.smoothScrollTo(0, testSectionTitle.getTop());
        }, 500);
    }
    
    private void setupButtonListeners() {
        // Get Started button
        getStartedButton.setOnClickListener(v -> {
            if (validateGlucoseInput()) {
                saveGlucoseValue();
                showCountdownDialog();
            }
        });
        
        // Question 1 (Temperature) buttons
        yesButton1.setOnClickListener(v -> {
            temperatureResponse = true;
            highlightSelectedButton(yesButton1, noButton1);
            checkEnableAnalyzeButton();
        });
        
        noButton1.setOnClickListener(v -> {
            temperatureResponse = false;
            highlightSelectedButton(noButton1, yesButton1);
            checkEnableAnalyzeButton();
        });
        
        // Question 2 (Pressure) buttons
        yesButton2.setOnClickListener(v -> {
            pressureResponse = true;
            highlightSelectedButton(yesButton2, noButton2);
            checkEnableAnalyzeButton();
        });
        
        noButton2.setOnClickListener(v -> {
            pressureResponse = false;
            highlightSelectedButton(noButton2, yesButton2);
            checkEnableAnalyzeButton();
        });
        
        // Analyze Results button
        analyzeResultsButton.setOnClickListener(v -> {
            saveTestResults();
            showResultsSummary();
        });
        
        // Initially disable the analyze button until all questions are answered
        analyzeResultsButton.setEnabled(false);
    }
    
    private void highlightSelectedButton(Button selectedButton, Button otherButton) {
        selectedButton.setAlpha(1.0f);
        otherButton.setAlpha(0.5f);
    }
    
    private void checkEnableAnalyzeButton() {
        // Enable the analyze button only when both questions have been answered
        boolean question1Answered = yesButton1.getAlpha() == 1.0f || noButton1.getAlpha() == 1.0f;
        boolean question2Answered = yesButton2.getAlpha() == 1.0f || noButton2.getAlpha() == 1.0f;
        
        analyzeResultsButton.setEnabled(question1Answered && question2Answered);
    }
    
    private void setupEmgChart() {
        // Configure the chart appearance
        emgChart.getDescription().setEnabled(false);
        emgChart.setTouchEnabled(true);
        emgChart.setDragEnabled(true);
        emgChart.setScaleEnabled(true);
        emgChart.setPinchZoom(true);
        emgChart.setDrawGridBackground(false);
        
        // Configure X axis
        XAxis xAxis = emgChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(100f);
        xAxis.setLabelCount(5);
        
        // Configure Y axis
        YAxis leftAxis = emgChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(-40f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setLabelCount(7);
        
        YAxis rightAxis = emgChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Configure legend
        Legend legend = emgChart.getLegend();
        legend.setEnabled(false);
    }
    
    private void generateSampleEmgData() {
        List<Entry> entries = new ArrayList<>();
        
        // Generate simulated EMG data with varying patterns
        float baseValue = 20f;
        float amplitude = 15f;
        float noiseLevel = 5f;
        
        // Create a realistic EMG pattern with bursts of activity
        for (int i = 0; i < 100; i++) {
            float value;
            
            // Create baseline with noise
            value = baseValue + (float) (Math.random() * noiseLevel - noiseLevel/2);
            
            // Add periodic muscle contractions
            if (i % 20 < 5) {
                // Burst of activity (simulating muscle contraction)
                value += amplitude * Math.sin(i * Math.PI / 5) + (Math.random() * amplitude/2);
            } else if (i > 50 && i < 60) {
                // Sustained contraction in the middle
                value += amplitude + (Math.random() * noiseLevel);
            } else if (i > 80) {
                // Increasing fatigue pattern toward the end
                value += (amplitude/2) * Math.sin(i * Math.PI / 4) * (100-i)/20;
            }
            
            entries.add(new Entry(i, value));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "EMG Signal");
        dataSet.setColor(Color.BLUE);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#80D6F9FF"));
        dataSet.setFillAlpha(100);
        
        LineData lineData = new LineData(dataSet);
        emgChart.setData(lineData);
        emgChart.invalidate(); // Refresh the chart
        
        // Animate the chart for better visual effect
        emgChart.animateX(1000);
    }
    
    private void saveTestResults() {
        if (mAuth.getCurrentUser() == null) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();
        
        // Create test result object
        Map<String, Object> testResult = new HashMap<>();
        testResult.put("timestamp", timestamp);
        testResult.put("temperatureSensation", temperatureResponse);
        testResult.put("pressureSensation", pressureResponse);
        
        // Save to Firebase
        usersRef.child(userId).child("testResults").child(String.valueOf(timestamp))
                .setValue(testResult)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Test results saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save test results", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showResultsSummary() {
        try {
            Log.d("HomeFragment", "Starting analysis process...");
            
            // Get the glucose value that was entered
            String glucoseValueStr = glucoseValueInput.getText().toString().trim();
            Log.d("HomeFragment", "Glucose value string: " + glucoseValueStr);
            
            float glucoseValue = Float.parseFloat(glucoseValueStr);
            Log.d("HomeFragment", "Parsed glucose value: " + glucoseValue);
            
            // Extract EMG data features from the chart
            Log.d("HomeFragment", "Extracting EMG features...");
            float[] emgFeatures = extractEmgFeatures();
            Log.d("HomeFragment", "EMG features extracted: " + 
                    "max=" + emgFeatures[0] + 
                    ", range=" + emgFeatures[1] + 
                    ", mean=" + emgFeatures[2] + 
                    ", stdDev=" + emgFeatures[3] + 
                    ", crossings=" + emgFeatures[4]);
            
            // Log sensory responses
            Log.d("HomeFragment", "Temperature response: " + temperatureResponse);
            Log.d("HomeFragment", "Pressure response: " + pressureResponse);
            
            // Create and show the analysis results dialog
            Log.d("HomeFragment", "Showing analysis results dialog...");
            showAnalysisResultsDialog(glucoseValue, emgFeatures, temperatureResponse, pressureResponse);
        } catch (Exception e) {
            Log.e("HomeFragment", "Error in showResultsSummary: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Analysis error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Extracts features from the EMG chart data for use in the prediction model.
     */
    private float[] extractEmgFeatures() {
        // Get the EMG data from the chart
        LineData lineData = emgChart.getData();
        if (lineData == null || lineData.getDataSetCount() == 0) {
            return new float[]{0, 0, 0, 0, 0}; // Default values if no data
        }
        
        LineDataSet dataSet = (LineDataSet) lineData.getDataSetByIndex(0);
        List<Entry> entries = dataSet.getValues();
        
        // Calculate basic features from the EMG data
        float maxAmplitude = 0;
        float minAmplitude = Float.MAX_VALUE;
        float sumAmplitude = 0;
        float sumSquaredDiff = 0;
        int crossings = 0;
        float prevValue = entries.get(0).getY();
        float baseline = 20f; // The baseline value used in generateSampleEmgData
        
        for (Entry entry : entries) {
            float value = entry.getY();
            
            // Update max and min
            maxAmplitude = Math.max(maxAmplitude, value);
            minAmplitude = Math.min(minAmplitude, value);
            
            // Sum for mean calculation
            sumAmplitude += value;
            
            // Count baseline crossings
            if ((prevValue < baseline && value >= baseline) || 
                (prevValue >= baseline && value < baseline)) {
                crossings++;
            }
            
            prevValue = value;
        }
        
        // Calculate mean
        float meanAmplitude = sumAmplitude / entries.size();
        
        // Calculate standard deviation
        for (Entry entry : entries) {
            float diff = entry.getY() - meanAmplitude;
            sumSquaredDiff += diff * diff;
        }
        float stdDeviation = (float) Math.sqrt(sumSquaredDiff / entries.size());
        
        // Return extracted features
        return new float[]{
            maxAmplitude,
            maxAmplitude - minAmplitude, // Range
            meanAmplitude,
            stdDeviation,
            crossings
        };
    }

    /**
     * Shows a dialog with the analysis results and recommendations.
     */
    private void showAnalysisResultsDialog(float glucoseValue, float[] emgFeatures, 
                                          boolean hasTemperatureSensation, boolean hasPressureSensation) {
        try {
            Log.d("HomeFragment", "Starting analysis dialog creation...");
            
            // Create the predictor
            Log.d("HomeFragment", "Creating NeuropathyPredictor...");
            NeuropathyPredictor predictor = new NeuropathyPredictor(requireContext());
            Log.d("HomeFragment", "NeuropathyPredictor created successfully");
            
            // Create input features for the model
            Log.d("HomeFragment", "Creating model features...");
            float[] modelFeatures = createModelFeatures(glucoseValue, emgFeatures, 
                                                      hasTemperatureSensation, hasPressureSensation);
            Log.d("HomeFragment", "Model features created successfully");
            
            // Get prediction
            Log.d("HomeFragment", "Running prediction...");
            // Log the features we're sending to the model
            StringBuilder featureLog = new StringBuilder("Model features: ");
            for (int i = 0; i < modelFeatures.length; i++) {
                featureLog.append(modelFeatures[i]);
                if (i < modelFeatures.length - 1) featureLog.append(", ");
            }
            Log.d("HomeFragment", featureLog.toString());
            
            float prediction = predictor.predict(modelFeatures);
            Log.d("HomeFragment", "Prediction result: " + prediction + 
                  ", Used real model: " + predictor.usedRealModel());
            
            // Evaluate risk
            Log.d("HomeFragment", "Evaluating risk...");
            NeuropathyPredictor.RiskAssessment assessment = 
                    predictor.evaluateRisk(prediction, glucoseValue, 
                                         hasTemperatureSensation, hasPressureSensation);
            Log.d("HomeFragment", "Risk level: " + assessment.getRiskLevel());
            Log.d("HomeFragment", "Risk score: " + assessment.getPredictionScore());
            
            // Create dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_analysis_results, null);
            
            // Set up dialog views
            TextView riskLevelText = dialogView.findViewById(R.id.text_risk_level);
            TextView riskScoreText = dialogView.findViewById(R.id.text_risk_score);
            TextView riskDescriptionText = dialogView.findViewById(R.id.text_risk_description);
            TextView glucoseValueText = dialogView.findViewById(R.id.text_glucose_value_summary);
            TextView temperatureSensationText = dialogView.findViewById(R.id.text_temperature_sensation);
            TextView pressureSensationText = dialogView.findViewById(R.id.text_pressure_sensation);
            TextView emgSummaryText = dialogView.findViewById(R.id.text_emg_summary);
            RecyclerView recommendationsRecycler = dialogView.findViewById(R.id.recycler_recommendations);
            Button closeButton = dialogView.findViewById(R.id.button_close_analysis);
            
            // Set up risk level information
            NeuropathyPredictor.RiskLevel riskLevel = assessment.getRiskLevel();
            String riskLevelStr = riskLevel.toString();
            int riskColor = Color.GREEN;
            
            switch (riskLevel) {
                case HIGH:
                    riskColor = Color.RED;
                    riskDescriptionText.setText("Based on your test results, your risk of diabetic neuropathy is high. " +
                            "Please consult with your healthcare provider as soon as possible.");
                    break;
                case MODERATE:
                    riskColor = Color.parseColor("#FFA500"); // Orange
                    riskDescriptionText.setText("Based on your test results, your risk of diabetic neuropathy is moderate. " +
                            "Discuss these results with your healthcare provider.");
                    break;
                case LOW:
                    riskDescriptionText.setText("Based on your test results, your risk of diabetic neuropathy is low. " +
                            "Continue with your regular diabetes management plan.");
                    break;
            }
            
            // Add indicator showing whether real model or fallback algorithm was used
            TextView modelSourceText = dialogView.findViewById(R.id.text_model_source);
            if (modelSourceText != null) {
                boolean usedRealModel = assessment.usedRealModel();
                modelSourceText.setVisibility(View.VISIBLE);
                if (usedRealModel) {
                    modelSourceText.setText("✓ Analysis by ML Model");
                    modelSourceText.setTextColor(Color.rgb(0, 128, 0)); // Dark Green
                    Log.d("HomeFragment", "UI showing: Analysis by ML Model");
                } else {
                    modelSourceText.setText("⚠ Using Fallback Algorithm");
                    modelSourceText.setTextColor(Color.rgb(255, 140, 0)); // Dark Orange
                    Log.d("HomeFragment", "UI showing: Using Fallback Algorithm");
                }
                Log.d("HomeFragment", "Using real ML model: " + usedRealModel + ", Risk score: " + assessment.getPredictionScore());
                Log.d("HomeFragment", "Using real ML model: " + usedRealModel);
            }
            
            riskLevelText.setText("Risk Level: " + riskLevelStr);
            riskLevelText.setTextColor(riskColor);
            riskScoreText.setText("Risk Score: " + String.format("%.2f", assessment.getPredictionScore()));
            
            // Set up test data summary
            glucoseValueText.setText("Glucose Value: " + glucoseValue + " mg/dL");
            temperatureSensationText.setText("Temperature Sensation: " + (hasTemperatureSensation ? "Yes" : "No"));
            pressureSensationText.setText("Pressure Sensation: " + (hasPressureSensation ? "Yes" : "No"));
            
            // EMG summary based on features
            String emgSummary = "EMG Analysis: ";
            if (emgFeatures[1] > 30) { // Range
                emgSummary += "High variability pattern";
            } else if (emgFeatures[4] > 10) { // Crossings
                emgSummary += "Frequent oscillation pattern";
            } else {
                emgSummary += "Normal pattern";
            }
            emgSummaryText.setText(emgSummary);
            
            // Set up recommendations recycler
            recommendationsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            RecommendationAdapter adapter = new RecommendationAdapter(assessment.getRecommendations());
            recommendationsRecycler.setAdapter(adapter);
            
            // Set up export results button
            Button exportButton = dialogView.findViewById(R.id.button_export_results);
            exportButton.setOnClickListener(v -> {
                // Convert Map<String, String> to List<String> for recommendations
                List<String> recommendationsList = new ArrayList<>();
                for (Map.Entry<String, String> entry : assessment.getRecommendations().entrySet()) {
                    recommendationsList.add(entry.getValue());
                }
                exportResultsToPdf(dialogView, assessment.getPredictionScore(), assessment.getRiskLevel().toString(), 
                        riskDescriptionText.getText().toString(), recommendationsList);
            });
            
            // Set up close button
            closeButton.setOnClickListener(v -> {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            });
            
            // Show dialog
            builder.setView(dialogView);
            dialog = builder.create();
            dialog.show();
            
            // Close the predictor when done
            predictor.close();
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error analyzing results: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error analyzing results: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private float[] createModelFeatures(float glucoseValue, float[] emgFeatures, 
                                       boolean hasTemperatureSensation, boolean hasPressureSensation) {
        // In a real implementation, we would need to match the exact feature set expected by the model
        // This is a simplified version that uses the available data
        
        // Use default values for age and diabetes duration
        // We're not trying to get actual values from Firebase here to avoid lambda issues
        // In a real implementation, you would want to fetch these values earlier and store them
        final float age = 50;
        final float diabetesDuration = 5;
        
        // Create features array
        // Order: age, diabetes_duration, fasting_sugar, emg_features (5), sensory_features (2)
        return new float[]{
            age,
            diabetesDuration,
            glucoseValue,
            emgFeatures[0], // Max amplitude
            emgFeatures[1], // Range
            emgFeatures[2], // Mean
            emgFeatures[3], // Standard deviation
            emgFeatures[4], // Crossings
            hasTemperatureSensation ? 1f : 0f,
            hasPressureSensation ? 1f : 0f
        };
    }
    
    // Model class for glucose readings
    public static class GlucoseReading {
        public float value;
        public long timestamp;
        
        public GlucoseReading() {
            // Required empty constructor for Firebase
        }
        
        public GlucoseReading(float value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
    
    // Model class for test results
    public static class TestResult {
        public long timestamp;
        public boolean temperatureSensation;
        public boolean pressureSensation;
        
        public TestResult() {
            // Required empty constructor for Firebase
        }
        
        public TestResult(long timestamp, boolean temperatureSensation, boolean pressureSensation) {
            this.timestamp = timestamp;
            this.temperatureSensation = temperatureSensation;
            this.pressureSensation = pressureSensation;
        }
    }
    
    /**
     * Exports the analysis results to a PDF file
     * @param view The dialog view containing the results
     * @param predictionScore The neuropathy risk prediction score
     * @param riskLevel The risk level (LOW, MODERATE, HIGH)
     * @param riskDescription The description of the risk level
     * @param recommendations The list of recommendations
     */
    private void exportResultsToPdf(View view, float predictionScore, String riskLevel, 
                                   String riskDescription, List<String> recommendations) {
        if (checkStoragePermission()) {
            createPdf(view, predictionScore, riskLevel, riskDescription, recommendations);
        } else {
            requestStoragePermission();
        }
    }
    
    /**
     * Checks if the app has permission to write to external storage
     * @return true if permission is granted, false otherwise
     */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above, we can use the MediaStore API without needing WRITE_EXTERNAL_STORAGE
            return true;
        } else {
            // For Android 9 and below, we need the WRITE_EXTERNAL_STORAGE permission
            return ContextCompat.checkSelfPermission(requireContext(), 
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * Requests permission to write to external storage
     */
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above, we don't need to request WRITE_EXTERNAL_STORAGE
            // Instead, we can use the MediaStore API to write to external storage
            createPdf(null, 0, "", "", new ArrayList<>());
        } else {
            // For Android 9 and below, we need to request the WRITE_EXTERNAL_STORAGE permission
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, retry export
            Toast.makeText(requireContext(), "Permission granted. Please try exporting again.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Storage permission is required to export results", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Creates a PDF document with the analysis results
     * @param view The dialog view containing the results
     * @param predictionScore The neuropathy risk prediction score
     * @param riskLevel The risk level (LOW, MODERATE, HIGH)
     * @param riskDescription The description of the risk level
     * @param recommendations The list of recommendations
     */
    private void createPdf(View view, float predictionScore, String riskLevel, 
                          String riskDescription, List<String> recommendations) {
        try {
            // If view is null, it means we're just testing permissions
            if (view == null) {
                Toast.makeText(requireContext(), "Storage access is available. Try exporting again.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "DiaNerverotect_Results_" + timestamp + ".pdf";
            File pdfFile;
            Document document = new Document();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use MediaStore
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                
                ContentResolver resolver = requireContext().getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                
                if (uri == null) {
                    Toast.makeText(requireContext(), "Failed to create PDF file", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Create a temporary file to use with FileProvider later
                pdfFile = new File(requireContext().getCacheDir(), fileName);
                
                // Write to both the MediaStore and our temp file
                PdfWriter.getInstance(document, requireContext().getContentResolver().openOutputStream(uri));
                PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
                document.open();
            } else {
                // For Android 9 and below, use direct file access
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                
                pdfFile = new File(dir, fileName);
                
                // Use the document created earlier
                PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
                document.open();
            }
            
            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLUE);
            Paragraph title = new Paragraph("DiaNerverotect Analysis Results", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            // Add date
            Font dateFont = new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC, BaseColor.GRAY);
            Paragraph date = new Paragraph("Generated on: " + 
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()), dateFont);
            date.setAlignment(Element.ALIGN_CENTER);
            document.add(date);
            document.add(new Paragraph("\n"));
            
            // Add risk assessment section
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph riskSection = new Paragraph("Neuropathy Risk Assessment", sectionFont);
            document.add(riskSection);
            document.add(new LineSeparator());
            document.add(new Paragraph("\n"));
            
            // Add risk level and score
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
            
            BaseColor riskColor;
            if (riskLevel.equals("HIGH")) {
                riskColor = BaseColor.RED;
            } else if (riskLevel.equals("MODERATE")) {
                riskColor = new BaseColor(255, 165, 0); // Orange
            } else {
                riskColor = BaseColor.GREEN;
            }
            
            Font riskLevelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, riskColor);
            
            Paragraph riskLevelPara = new Paragraph();
            riskLevelPara.add(new Chunk("Risk Level: ", boldFont));
            riskLevelPara.add(new Chunk(riskLevel, riskLevelFont));
            document.add(riskLevelPara);
            
            Paragraph riskScorePara = new Paragraph();
            riskScorePara.add(new Chunk("Risk Score: ", boldFont));
            riskScorePara.add(new Chunk(String.format("%.2f", predictionScore), normalFont));
            document.add(riskScorePara);
            document.add(new Paragraph("\n"));
            
            // Add risk description
            document.add(new Paragraph(riskDescription, normalFont));
            document.add(new Paragraph("\n"));
            
            // Add test data summary
            document.add(new Paragraph("Test Data Summary", sectionFont));
            document.add(new LineSeparator());
            document.add(new Paragraph("\n"));
            
            // Get test data from the view
            TextView glucoseValueText = view.findViewById(R.id.text_glucose_value_summary);
            TextView temperatureSensationText = view.findViewById(R.id.text_temperature_sensation);
            TextView pressureSensationText = view.findViewById(R.id.text_pressure_sensation);
            TextView emgSummaryText = view.findViewById(R.id.text_emg_summary);
            
            // Create a table for test data
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            
            // Set column widths
            float[] columnWidths = {1f, 2f};
            table.setWidths(columnWidths);
            
            // Add table headers
            PdfPCell cell1 = new PdfPCell(new Phrase("Test", boldFont));
            PdfPCell cell2 = new PdfPCell(new Phrase("Result", boldFont));
            cell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell1.setPadding(5);
            cell2.setPadding(5);
            table.addCell(cell1);
            table.addCell(cell2);
            
            // Add glucose value
            table.addCell(new PdfPCell(new Phrase("Glucose Level", boldFont)));
            table.addCell(new PdfPCell(new Phrase(glucoseValueText.getText().toString(), normalFont)));
            
            // Add temperature sensation
            table.addCell(new PdfPCell(new Phrase("Temperature Sensation", boldFont)));
            table.addCell(new PdfPCell(new Phrase(temperatureSensationText.getText().toString(), normalFont)));
            
            // Add pressure sensation
            table.addCell(new PdfPCell(new Phrase("Pressure Sensation", boldFont)));
            table.addCell(new PdfPCell(new Phrase(pressureSensationText.getText().toString(), normalFont)));
            
            // Add EMG summary
            table.addCell(new PdfPCell(new Phrase("EMG Analysis", boldFont)));
            table.addCell(new PdfPCell(new Phrase(emgSummaryText.getText().toString(), normalFont)));
            
            document.add(table);
            document.add(new Paragraph("\n"));
            
            // Add recommendations section
            document.add(new Paragraph("Recommendations for Your Doctor", sectionFont));
            document.add(new LineSeparator());
            document.add(new Paragraph("\n"));
            
            // Add recommendations list
            for (String recommendation : recommendations) {
                Paragraph recommendationPara = new Paragraph();
                recommendationPara.add(new Chunk("• ", boldFont));
                recommendationPara.add(new Chunk(recommendation, normalFont));
                document.add(recommendationPara);
                document.add(new Paragraph("\n"));
            }
            
            // Add disclaimer
            Font disclaimerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY);
            Paragraph disclaimer = new Paragraph("Disclaimer: This report is generated for informational purposes only. " +
                    "It is not a substitute for professional medical advice, diagnosis, or treatment. " +
                    "Always seek the advice of your physician or other qualified health provider with any questions you may have.", 
                    disclaimerFont);
            document.add(disclaimer);
            
            // Close document
            document.close();
            
            // Show success message and offer to open the file
            showPdfSavedDialog(pdfFile);
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error creating PDF: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error creating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Shows a dialog informing the user that the PDF has been saved and offering to open it
     * @param file The saved PDF file
     */
    private void showPdfSavedDialog(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("PDF Saved");
        builder.setMessage("Results have been saved to Downloads folder as:\n" + file.getName());
        builder.setPositiveButton("Open", (dialog, which) -> openPdfFile(file));
        builder.setNegativeButton("Close", null);
        builder.show();
    }
    
    /**
     * Opens the PDF file using an external app
     * @param file The PDF file to open
     */
    private void openPdfFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(requireContext(), 
                    requireContext().getPackageName() + ".provider", file);
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(intent);
        } catch (Exception e) {
            Log.e("HomeFragment", "Error opening PDF: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "No PDF viewer app found", Toast.LENGTH_SHORT).show();
        }
    }
}
