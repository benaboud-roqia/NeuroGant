package com.dianerverotect;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment for Sclérose latérale amyotrophique (ALS) specific functionality.
 * This fragment is displayed when the user selects ALS as their disease.
 */
public class ALSFragment extends Fragment {

    private static final String TAG = "ALSFragment";
    private static final int REQUEST_LOAD_EMG_FILE = 1001;
    private static final int TOTAL_STEPS = 4;
    private static final int COUNTDOWN_SECONDS = 20;
    private static final int COUNTDOWN_INTERVAL = 1000; // 1 second
    private static final int REQUEST_STORAGE_PERMISSION = 1002;
    
    // Countdown dialog
    private AlertDialog dialog;
    
    // UI Components
    private ViewPager2 viewPager;
    private TextView stepIndicator;
    private Button buttonPrevious;
    private Button buttonNext;
    
    // Step Views
    private View step1View;
    private View step2View;
    private View step3View;
    private View step4View;
    
    // Data Storage
    private Map<String, Object> alsData;
    
    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    
    // EMG Data
    private float[] emgSignal;
    private boolean isRecording = false;
    private boolean hasValidEmgData = false;
    
    // UI Components for EMG recording
    private Button startRecordingButton;
    private Button stopRecordingButton;
    private Button connectGloveButton;
    private Button loadFileButton;
    private TextView connectionStatusText;
    
    public ALSFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_als, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Ensure user is authenticated (even anonymously)
        ensureUserAuthentication();
        
        // Initialize data storage
        alsData = new HashMap<>();
        
        // Initialize UI components
        viewPager = view.findViewById(R.id.view_pager_als);
        stepIndicator = view.findViewById(R.id.text_step_indicator);
        buttonPrevious = view.findViewById(R.id.button_previous);
        buttonNext = view.findViewById(R.id.button_next);
        
        // Set up ViewPager
        setupViewPager();
        
        // Set up navigation buttons
        setupNavigationButtons();
    }
    
    /**
     * Checks if the user is authenticated
     */
    private void ensureUserAuthentication() {
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "No user signed in");
        } else {
            Log.d(TAG, "User already signed in: " + mAuth.getCurrentUser().getUid());
        }
    }
    
    /**
     * Sets up the ViewPager with the step layouts
     */
    private void setupViewPager() {
        // Create adapter for the ViewPager
        ALSPagerAdapter adapter = new ALSPagerAdapter(getLayoutInflater());
        viewPager.setAdapter(adapter);
        
        // Disable swiping to ensure users follow the proper sequence
        viewPager.setUserInputEnabled(false);
        
        // Update step indicator when page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateStepIndicator(position);
                updateNavigationButtons(position);
            }
        });
    }
    
    /**
     * Sets up the previous and next buttons
     */
    private void setupNavigationButtons() {
        buttonPrevious.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            if (currentPosition > 0) {
                viewPager.setCurrentItem(currentPosition - 1);
            }
        });
        
        buttonNext.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            if (currentPosition < TOTAL_STEPS - 1) {
                // Validate current step before proceeding
                if (validateCurrentStep(currentPosition)) {
                    viewPager.setCurrentItem(currentPosition + 1);
                }
            } else {
                // On the last step, save the result
                saveResult();
            }
        });
    }
    
    /**
     * Updates the step indicator text based on the current position
     */
    private void updateStepIndicator(int position) {
        switch (position) {
            case 0:
                stepIndicator.setText(R.string.step_1_of_4);
                break;
            case 1:
                stepIndicator.setText(R.string.step_2_of_4);
                break;
            case 2:
                stepIndicator.setText(R.string.step_3_of_4);
                break;
            case 3:
                stepIndicator.setText(R.string.step_4_of_4);
                break;
        }
    }
    
    /**
     * Updates the navigation buttons based on the current position
     */
    private void updateNavigationButtons(int position) {
        // Show/hide previous button
        buttonPrevious.setVisibility(position > 0 ? View.VISIBLE : View.INVISIBLE);
        
        // Update next button text on last step
        if (position == TOTAL_STEPS - 1) {
            buttonNext.setText(R.string.finish);
        } else {
            buttonNext.setText(R.string.next);
        }
    }
    
    /**
     * Validates the current step before proceeding to the next
     */
    private boolean validateCurrentStep(int currentStep) {
        switch (currentStep) {
            case 0:
                return validatePersonalInfo();
            case 1:
                return validateQuestionnaire();
            case 2:
                return validateEmgSignal();
            default:
                return true;
        }
    }
    
    /**
     * Validates the personal information step
     */
    private boolean validatePersonalInfo() {
        // TODO: Implement validation for personal information
        // For now, return true to allow proceeding
        return true;
    }
    
    /**
     * Validates the questionnaire step
     */
    private boolean validateQuestionnaire() {
        // TODO: Implement validation for questionnaire
        // For now, return true to allow proceeding
        return true;
    }
    
    /**
     * Validates the EMG signal step
     */
    private boolean validateEmgSignal() {
        // Check if we have valid EMG data
        if (!hasValidEmgData) {
            Toast.makeText(requireContext(), "Please record or load EMG data first", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    
    /**
     * Saves the result to Firebase
     */
    private void saveResult() {
        // Check if user is authenticated
        if (mAuth.getCurrentUser() == null) {
            // Try to get the current user again in case there was an auth state change
            mAuth = FirebaseAuth.getInstance();
            
            if (mAuth.getCurrentUser() == null) {
                // User is not authenticated, show login dialog
                showLoginRequiredDialog();
                return;
            }
        }
        
        // Get the prediction score
        float predictionScore = calculatePredictionScore();
        
        // Create a result object with all collected data
        Map<String, Object> result = new HashMap<>();
        result.put("userId", mAuth.getCurrentUser().getUid());
        result.put("timestamp", new Date().getTime());
        result.put("predictionScore", predictionScore);
        result.put("personalInfo", alsData);
        result.put("emgDataCollected", hasValidEmgData);
        result.put("dateTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        
        // Get user info for reference
        String userEmail = mAuth.getCurrentUser().getEmail();
        result.put("userEmail", userEmail != null ? userEmail : "unknown");
        
        // Generate a unique key for this result
        String resultKey = mDatabase.child("als_results").push().getKey();
        if (resultKey == null) {
            Toast.makeText(requireContext(), "Failed to generate database key", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to generate Firebase key for result");
            return;
        }
        
        // Show loading indicator
        Toast.makeText(requireContext(), "Saving result...", Toast.LENGTH_SHORT).show();
        
        // Save to Firebase under the user's ID and also in a general results collection
        mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).child("als_results").child(resultKey).setValue(result)
                .addOnSuccessListener(aVoid -> {
                    // Also save to general results collection
                    mDatabase.child("als_results").child(resultKey).setValue(result)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(requireContext(), "Result saved successfully", Toast.LENGTH_SHORT).show();
                                // Update the UI to show the result has been saved
                                updateResultSavedStatus(true);
                                Log.d(TAG, "ALS result saved successfully with ID: " + resultKey);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(), "Partially saved: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error saving to general results", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to save result: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving result", e);
                });
    }
    
    /**
     * Calculates the prediction score based on the collected data and EMG signal
     */
    private float calculatePredictionScore() {
        // TODO: Implement the actual prediction using TensorFlow Lite model
        // For now, return a dummy score
        return 0.3f;
    }
    
    /**
     * Updates the UI to show the result has been saved
     */
    private void updateResultSavedStatus(boolean saved) {
        // Update UI to reflect saved status
        View view = getView();
        if (view != null) {
            Button saveResultButton = view.findViewById(R.id.button_save_result);
            if (saved) {
                saveResultButton.setText("Result Saved");
                saveResultButton.setEnabled(false);
                Toast.makeText(requireContext(), "Result saved successfully!", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Shows a dialog informing the user they need to be logged in
     */
    private void showLoginRequiredDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Login Required")
               .setMessage("You need to be logged in to save results. Would you like to log in now?")
               .setPositiveButton("Log In", (dialog, which) -> {
                   // Navigate to login screen
                   Toast.makeText(requireContext(), "Please log in to save your results", Toast.LENGTH_LONG).show();
                   // Redirect to login activity if available
                   try {
                       ((MainActivity) requireActivity()).navigateToLoginScreen();
                   } catch (Exception e) {
                       Log.e(TAG, "Error navigating to login: " + e.getMessage());
                       Toast.makeText(requireContext(), "Please log in from the main screen", Toast.LENGTH_LONG).show();
                   }
               })
               .setNegativeButton("Cancel", (dialog, which) -> {
                   dialog.dismiss();
                   Toast.makeText(requireContext(), "Result not saved", Toast.LENGTH_SHORT).show();
               })
               .setCancelable(false)
               .show();
    }
    
    /**
     * Adapter for the ViewPager that displays the ALS Predictor steps
     */
    private class ALSPagerAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ALSPagerAdapter.ViewHolder> {
        
        private final LayoutInflater inflater;
        private final int[] layouts = {
                R.layout.layout_als_step1_personal_info,
                R.layout.layout_als_step2_questionnaire,
                R.layout.layout_als_step3_emg_signal,
                R.layout.layout_als_step4_result
        };
        
        public ALSPagerAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = inflater.inflate(layouts[viewType], parent, false);
            return new ViewHolder(view, viewType);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            switch (position) {
                case 0:
                    setupPersonalInfoView(holder.itemView);
                    break;
                case 1:
                    setupQuestionnaireView(holder.itemView);
                    break;
                case 2:
                    setupEmgSignalView(holder.itemView);
                    break;
                case 3:
                    setupResultView(holder.itemView);
                    break;
            }
        }
        
        @Override
        public int getItemCount() {
            return layouts.length;
        }
        
        @Override
        public int getItemViewType(int position) {
            return position;
        }
        
        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView, int viewType) {
                super(itemView);
                
                // Store references to the step views
                switch (viewType) {
                    case 0:
                        step1View = itemView;
                        break;
                    case 1:
                        step2View = itemView;
                        break;
                    case 2:
                        step3View = itemView;
                        break;
                    case 3:
                        step4View = itemView;
                        break;
                }
            }
        }
    }
    
    /**
     * Sets up the personal information view with listeners
     */
    private void setupPersonalInfoView(View view) {
        // Set up gender radio group
        RadioGroup genderGroup = view.findViewById(R.id.radio_group_gender);
        genderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String gender = checkedId == R.id.radio_male ? "Male" : "Female";
            alsData.put("gender", gender);
        });
        
        // Set up family history radio group
        RadioGroup familyHistoryGroup = view.findViewById(R.id.radio_group_family_history);
        familyHistoryGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean hasFamilyHistory = checkedId == R.id.radio_family_history_yes;
            alsData.put("familyHistory", hasFamilyHistory);
        });
        
        // Set up text input fields
        TextInputEditText ageInput = view.findViewById(R.id.edit_age);
        TextInputEditText weightInput = view.findViewById(R.id.edit_weight);
        TextInputEditText symptomDurationInput = view.findViewById(R.id.edit_symptom_duration);
        
        // Save data when text changes
        ageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String ageText = ageInput.getText().toString();
                if (!ageText.isEmpty()) {
                    alsData.put("age", Integer.parseInt(ageText));
                }
            }
        });
        
        weightInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String weightText = weightInput.getText().toString();
                if (!weightText.isEmpty()) {
                    alsData.put("weight", Float.parseFloat(weightText));
                }
            }
        });
        
        symptomDurationInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String durationText = symptomDurationInput.getText().toString();
                if (!durationText.isEmpty()) {
                    alsData.put("symptomDuration", Integer.parseInt(durationText));
                }
            }
        });
    }
    
    /**
     * Sets up the questionnaire view with listeners
     */
    private void setupQuestionnaireView(View view) {
        // Set up all radio groups for the questionnaire
        setupQuestionnaireRadioGroup(view, R.id.radio_group_q1, "q1_muscle_weakness");
        setupQuestionnaireRadioGroup(view, R.id.radio_group_q2, "q2_muscle_cramps");
        setupQuestionnaireRadioGroup(view, R.id.radio_group_q3, "q3_fasciculations");
        setupQuestionnaireRadioGroup(view, R.id.radio_group_q4, "q4_walking_difficulty");
        setupQuestionnaireRadioGroup(view, R.id.radio_group_q5, "q5_dysarthria");
        setupQuestionnaireRadioGroup(view, R.id.radio_group_q6, "q6_dysphagia");

    }
    
    /**
     * Helper method to set up a questionnaire radio group
     */
    private void setupQuestionnaireRadioGroup(View view, int radioGroupId, String dataKey) {
        RadioGroup radioGroup = view.findViewById(radioGroupId);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // Get the selected radio button's position (0-3)
            int selectedIndex = -1;
            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                if (radioGroup.getChildAt(i).getId() == checkedId) {
                    selectedIndex = i;
                    break;
                }
            }
            
            if (selectedIndex >= 0) {
                alsData.put(dataKey, selectedIndex);
            }
        });
    }
    
    /**
     * Sets up the EMG signal view with listeners
     */
    private void setupEmgSignalView(View view) {
        // Initialize the EMG UI components
        connectGloveButton = view.findViewById(R.id.button_connect_glove);
        loadFileButton = view.findViewById(R.id.button_load_file);
        startRecordingButton = view.findViewById(R.id.button_start_recording);
        stopRecordingButton = view.findViewById(R.id.button_stop_recording);
        connectionStatusText = view.findViewById(R.id.text_connection_status);
        
        // Set up connect glove button
        connectGloveButton.setOnClickListener(v -> {
            // Show countdown dialog before connecting to glove
            showCountdownDialog();
        });
        
        // Set up load file button
        loadFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_LOAD_EMG_FILE);
        });
        
        // Set up recording buttons
        startRecordingButton.setOnClickListener(v -> {
            // Show countdown dialog before starting recording
            showCountdownDialog();
        });
        
        stopRecordingButton.setOnClickListener(v -> {
            stopEmgRecording();
        });
    }
    
    /**
     * Creates dummy EMG data for demonstration purposes
     */
    private void createDummyEmgData() {
        // Create a dummy EMG signal with 4000 samples
        emgSignal = new float[4000];
        for (int i = 0; i < emgSignal.length; i++) {
            emgSignal[i] = (float) (Math.random() * 2 - 1); // Random values between -1 and 1
        }
        hasValidEmgData = true;
        
        // Make sure the EMG chart is visible
        com.github.mikephil.charting.charts.LineChart emgChart = step3View.findViewById(R.id.emg_chart);
        emgChart.setVisibility(View.VISIBLE);
        
        // Update the EMG chart with the dummy data
        setupEmgChart(emgChart);
        displayEmgData(emgChart, emgSignal);
    }
    
    /**
     * Sets up the result view
     */
    private void setupResultView(View view) {
        TextView scoreValueText = view.findViewById(R.id.text_score_value);
        TextView dateTimeText = view.findViewById(R.id.text_date_time);
        TextView recommendationDetailsText = view.findViewById(R.id.text_recommendation_details);
        Button saveResultButton = view.findViewById(R.id.button_save_result);
        Button exportResultsButton = view.findViewById(R.id.button_export_results);
        ProgressBar progressScore = view.findViewById(R.id.progress_score);
        
        // Set current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());
        dateTimeText.setText(currentDateTime);
        
        // Calculate and display prediction score
        float predictionScore = calculatePredictionScore();
        scoreValueText.setText(String.format(Locale.getDefault(), "%.2f", predictionScore));
        
        // Update progress bar
        int progressValue = (int) (predictionScore * 100);
        progressScore.setProgress(progressValue);
        
        // Update recommendation based on score
        updateRecommendation(view, predictionScore);
        
        // Set recommendation details based on score
        String recommendationDetails;
        if (predictionScore > 0.5f) {
            // High risk details
            recommendationDetails = "Based on your test results and symptoms, we recommend consulting with a neurologist " +
                "as soon as possible. Early intervention is crucial for managing ALS symptoms and " +
                "improving quality of life.";
        } else {
            // Low risk details
            recommendationDetails = "Based on your test results, your risk of ALS appears to be low. However, it is still " +
                "recommended to follow up with your healthcare provider during your regular check-ups " +
                "and monitor any changes in symptoms.";
        }
        recommendationDetailsText.setText(recommendationDetails);
        
        // Set up save button
        saveResultButton.setOnClickListener(v -> saveResult());
        
        // Set up export results button
        exportResultsButton.setOnClickListener(v -> {
            // Check for storage permission
            if (checkStoragePermission()) {
                // Export results to PDF
                exportResultsToPdf(view, predictionScore, currentDateTime, recommendationDetails);
            } else {
                // Request storage permission
                requestStoragePermission();
            }
        });
    }
    
    /**
     * Updates the recommendation text and color based on the prediction score
     */
    private void updateRecommendation(View view, float score) {
        TextView recommendationText = view.findViewById(R.id.text_recommendation);
        
        if (score > 0.5f) {
            // High risk
            recommendationText.setText(R.string.high_risk);
            recommendationText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            // No risk
            recommendationText.setText(R.string.no_risk);
            recommendationText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        
        // Update the Next Steps card based on risk level
        updateNextSteps(view, score);
    }
    
    /**
     * Updates the next steps recommendations based on the prediction score
     */
    private void updateNextSteps(View view, float score) {
        TextView step1Text = view.findViewById(R.id.text_step1);
        TextView step2Text = view.findViewById(R.id.text_step2);
        TextView step3Text = view.findViewById(R.id.text_step3);
        
        if (score > 0.5f) {
            // High risk next steps
            step1Text.setText("1. Schedule an appointment with a neurologist specializing in motor neuron diseases");
            step2Text.setText("2. Bring your EMG results and assessment from this application");
            step3Text.setText("3. Consider joining an ALS support group for additional resources");
        } else {
            // Low risk next steps
            step1Text.setText("1. Discuss these results with your primary care physician during your next visit");
            step2Text.setText("2. Monitor any changes in symptoms and report them to your doctor");
            step3Text.setText("3. Maintain a healthy lifestyle with regular exercise and balanced nutrition");
        }
    }
    
        /**
     * Shows a countdown dialog with animation before starting EMG recording
     */
    private void showCountdownDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_countdown, null);
        TextView countdownText = dialogView.findViewById(R.id.text_countdown);
        TextView titleText = dialogView.findViewById(R.id.text_countdown_title);
        TextView instructionText = dialogView.findViewById(R.id.textView);
        ImageView gifImageView = dialogView.findViewById(R.id.gif_animation);
        
        // Update text for ALS context
        titleText.setText("Preparing EMG Test...");
        instructionText.setText("Please relax your hand and prepare for EMG recording...");
        
        // Load animation GIF
        Glide.with(this)
                .load(R.drawable.handcrush) // Using the same animation as HomeFragment
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
                startEmgRecording();
            }
        }.start();
    }
    
    /**
     * Starts the EMG recording after countdown completes
     */
    private void startEmgRecording() {
        isRecording = true;
        startRecordingButton.setEnabled(false);
        stopRecordingButton.setEnabled(true);
        
        // Update connection status
        connectionStatusText.setText("Connected");
        
        // Show the EMG chart
        com.github.mikephil.charting.charts.LineChart emgChart = step3View.findViewById(R.id.emg_chart);
        emgChart.setVisibility(View.VISIBLE);
        
        // Configure the chart appearance
        setupEmgChart(emgChart);
        
        // Update UI to show recording in progress
        TextView statusText = step3View.findViewById(R.id.text_recording_status);
        statusText.setText("Recording in progress...");
        statusText.setVisibility(View.VISIBLE);
        
        // Show progress bar
        android.widget.ProgressBar progressBar = step3View.findViewById(R.id.progress_recording);
        progressBar.setVisibility(View.VISIBLE);
        
        // Simulate progress updates and generate EMG data in real-time
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            int progress = 0;
            
            @Override
            public void run() {
                if (isRecording && progress < 100) {
                    progress += 5;
                    progressBar.setProgress(progress);
                    
                    // Update the EMG chart with new data points
                    updateEmgChart(emgChart, progress);
                    
                    handler.postDelayed(this, 500); // Update every 0.5 seconds
                } else if (progress >= 100) {
                    // Auto-stop when reaching 100%
                    stopEmgRecording();
                }
            }
        }, 500);
        
        // TODO: Implement actual EMG recording
        Toast.makeText(requireContext(), "EMG Recording started", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Stops the EMG recording
     */
    private void stopEmgRecording() {
        isRecording = false;
        stopRecordingButton.setEnabled(false);
        startRecordingButton.setEnabled(true);
        
        // Update UI
        TextView statusText = step3View.findViewById(R.id.text_recording_status);
        statusText.setText("Recording completed");
        
        // TODO: Implement stopping actual EMG recording
        Toast.makeText(requireContext(), "Recording stopped", Toast.LENGTH_SHORT).show();
        
        // For demo purposes, create dummy EMG data
        createDummyEmgData();
    }
    
    /**
     * Sets up the EMG chart appearance and configuration
     */
    private void setupEmgChart(com.github.mikephil.charting.charts.LineChart emgChart) {
        // Configure the chart appearance
        emgChart.getDescription().setEnabled(false);
        emgChart.setTouchEnabled(true);
        emgChart.setDragEnabled(true);
        emgChart.setScaleEnabled(true);
        emgChart.setPinchZoom(true);
        emgChart.setDrawGridBackground(false);
        
        // Configure X axis
        com.github.mikephil.charting.components.XAxis xAxis = emgChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(100f);
        xAxis.setLabelCount(5);
        
        // Configure Y axis
        com.github.mikephil.charting.components.YAxis leftAxis = emgChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(-40f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setLabelCount(7);
        
        com.github.mikephil.charting.components.YAxis rightAxis = emgChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Configure legend
        com.github.mikephil.charting.components.Legend legend = emgChart.getLegend();
        legend.setEnabled(false);
    }


    /**
     * Displays the complete EMG data on the chart
     */
    private void displayEmgData(com.github.mikephil.charting.charts.LineChart emgChart, float[] emgData) {
        // Create entries for the chart
        java.util.List<com.github.mikephil.charting.data.Entry> entries = new java.util.ArrayList<>();
        
        // Use a subset of the data for display (to avoid overwhelming the chart)
        int step = emgData.length / 100;
        if (step < 1) step = 1;
        
        for (int i = 0; i < emgData.length; i += step) {
            if (entries.size() < 100) { // Limit to 100 data points for better visualization
                // Scale the data to make it visible on the chart
                float scaledValue = 50 + emgData[i] * 40; // Scale to range approximately 10-90
                entries.add(new com.github.mikephil.charting.data.Entry(entries.size(), scaledValue));
            }
        }
        
        // Create a dataset from the entries
        com.github.mikephil.charting.data.LineDataSet dataSet = new com.github.mikephil.charting.data.LineDataSet(entries, "EMG Signal");
        dataSet.setColor(android.graphics.Color.BLUE);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.LINEAR);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(android.graphics.Color.parseColor("#80D6F9FF"));
        dataSet.setFillAlpha(100);
        
        // Set the data to the chart
        com.github.mikephil.charting.data.LineData lineData = new com.github.mikephil.charting.data.LineData(dataSet);
        emgChart.setData(lineData);
        
        // Animate the chart for better visual effect
        emgChart.animateX(1000);
    }



    
    /**
     * Stops the EMG recording process
     */
    private void stopRecording() {
        // Update UI
        TextView statusText = step3View.findViewById(R.id.text_recording_status);
        statusText.setText("Recording completed");
                
        // TODO: Implement stopping actual EMG recording
        Toast.makeText(requireContext(), "Recording stopped", Toast.LENGTH_SHORT).show();
                
        // For demo purposes, create dummy EMG data
        createDummyEmgData();
    }

   

    /**
     * Updates the EMG chart with new data points in real-time
     */
    private void updateEmgChart(com.github.mikephil.charting.charts.LineChart emgChart, int progress) {
        // Create entries for the chart
        java.util.List<com.github.mikephil.charting.data.Entry> entries = new java.util.ArrayList<>();
                
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
                value += amplitude * (1 + 0.5f * (float)Math.sin(i * 0.5f));
            }
                    
            // Add additional noise based on progress (more noise as recording continues)
            value += (progress / 100f) * (float) (Math.random() * 10 - 5);
                    
                entries.add(new com.github.mikephil.charting.data.Entry(i, value));
        }
                
        // Create a dataset from the entries
        com.github.mikephil.charting.data.LineDataSet dataSet = new com.github.mikephil.charting.data.LineDataSet(entries, "EMG Signal");
        dataSet.setColor(android.graphics.Color.BLUE);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.LINEAR);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(android.graphics.Color.parseColor("#80D6F9FF"));
        dataSet.setFillAlpha(100);
                
        // Set the data to the chart
        com.github.mikephil.charting.data.LineData lineData = new com.github.mikephil.charting.data.LineData(dataSet);
        emgChart.setData(lineData);
        emgChart.invalidate(); // Refresh the chart
    }



    /**
     * Checks if storage permission is granted
     */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        } else {
            return ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Requests storage permission
     */
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_STORAGE_PERMISSION);
    }

    /**
     * Exports the results to a PDF file
     */
    private void exportResultsToPdf(View view, float predictionScore, String currentDateTime, String recommendationDetails) {
        try {
            if (view == null) {
                Toast.makeText(requireContext(), "Storage access is available. Try exporting again.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "DiaNerverotect_ALS_Results_" + timestamp + ".pdf";
            File pdfFile;
            
            // Create a PDF document
            PdfDocument document = new PdfDocument();
            
            // Handle file creation based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                
                // Create a temporary file for preview
                pdfFile = new File(requireContext().getCacheDir(), fileName);
            } else {
                // For older Android versions, use direct file access
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                pdfFile = new File(dir, fileName);
            }
            
            // Create a page info with A4 dimensions
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            
            // Start a page
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            // Get views for PDF content
            View headerView = step4View.findViewById(R.id.card_header);
            View scoreView = step4View.findViewById(R.id.card_prediction_score);
            View recommendationView = step4View.findViewById(R.id.card_recommendation);
            View nextStepsView = step4View.findViewById(R.id.card_next_steps);
            
            // Draw the views to the canvas
            int y = 50; // Starting Y position
            
            // Draw title
            android.graphics.Paint titlePaint = new android.graphics.Paint();
            titlePaint.setColor(Color.rgb(2, 119, 189)); // #0277BD
            titlePaint.setTextSize(24);
            titlePaint.setFakeBoldText(true);
            canvas.drawText("DiaNerverotect - ALS Assessment Results", 50, y, titlePaint);
            
            // Draw date
            y += 30;
            android.graphics.Paint datePaint = new android.graphics.Paint();
            datePaint.setColor(Color.DKGRAY);
            datePaint.setTextSize(14);
            canvas.drawText("Date: " + currentDateTime, 50, y, datePaint);
            
            // Draw score section
            y += 50;
            android.graphics.Paint sectionPaint = new android.graphics.Paint();
            sectionPaint.setColor(Color.rgb(1, 87, 155)); // #01579B
            sectionPaint.setTextSize(18);
            sectionPaint.setFakeBoldText(true);
            canvas.drawText("Prediction Score", 50, y, sectionPaint);
            
            // Draw line
            y += 10;
            android.graphics.Paint linePaint = new android.graphics.Paint();
            linePaint.setColor(Color.LTGRAY);
            linePaint.setStrokeWidth(2);
            canvas.drawLine(50, y, 545, y, linePaint);
            
            // Draw score
            y += 40;
            android.graphics.Paint scorePaint = new android.graphics.Paint();
            scorePaint.setColor(predictionScore > 0.5f ? Color.RED : Color.rgb(76, 175, 80)); // #4CAF50
            scorePaint.setTextSize(36);
            scorePaint.setFakeBoldText(true);
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", predictionScore), 50, y, scorePaint);
            
            // Draw risk level
            y += 20;
            android.graphics.Paint riskPaint = new android.graphics.Paint();
            riskPaint.setColor(predictionScore > 0.5f ? Color.RED : Color.rgb(76, 175, 80));
            riskPaint.setTextSize(18);
            riskPaint.setFakeBoldText(true);
            canvas.drawText(predictionScore > 0.5f ? "High Risk" : "Low Risk", 50, y, riskPaint);
            
            // Draw recommendation section
            y += 50;
            canvas.drawText("Recommendation", 50, y, sectionPaint);
            
            // Draw line
            y += 10;
            canvas.drawLine(50, y, 545, y, linePaint);
            
            // Draw recommendation
            y += 40;
            android.graphics.Paint recTitlePaint = new android.graphics.Paint();
            recTitlePaint.setColor(predictionScore > 0.5f ? Color.RED : Color.rgb(46, 125, 50)); // #2E7D32
            recTitlePaint.setTextSize(20);
            recTitlePaint.setFakeBoldText(true);
            canvas.drawText(predictionScore > 0.5f ? "High Risk of ALS" : "Low Risk of ALS", 50, y, recTitlePaint);
            
            // Draw recommendation details
            y += 30;
            android.graphics.Paint detailsPaint = new android.graphics.Paint();
            detailsPaint.setColor(Color.BLACK);
            detailsPaint.setTextSize(14);
            
            // Split text into lines for better formatting
            String[] lines = splitTextIntoLines(recommendationDetails, 70);
            for (String line : lines) {
                canvas.drawText(line, 50, y, detailsPaint);
                y += 20;
            }
            
            // Draw next steps section
            y += 30;
            canvas.drawText("Next Steps", 50, y, sectionPaint);
        
            // Draw line
            y += 10;
            canvas.drawLine(50, y, 545, y, linePaint);
            
            // Get next steps text
            TextView step1Text = step4View.findViewById(R.id.text_step1);
            TextView step2Text = step4View.findViewById(R.id.text_step2);
            TextView step3Text = step4View.findViewById(R.id.text_step3);
            
            // Draw steps
            y += 40;
            canvas.drawText(step1Text.getText().toString(), 50, y, detailsPaint);
            y += 30;
            canvas.drawText(step2Text.getText().toString(), 50, y, detailsPaint);
            y += 30;
            canvas.drawText(step3Text.getText().toString(), 50, y, detailsPaint);
            
            // Draw footer
            y = 800;
            android.graphics.Paint footerPaint = new android.graphics.Paint();
            footerPaint.setColor(Color.DKGRAY);
            footerPaint.setTextSize(12);
            canvas.drawText("DiaNerverotect - AI-Powered Neurological Assessment", 50, y, footerPaint);
            
            // Finish the page
            document.finishPage(page);
            
            // For all Android versions, write to the cache directory first
            document.writeTo(new java.io.FileOutputStream(pdfFile));
            
            // For Android 10+, also save to Downloads using MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    ContentResolver resolver = requireContext().getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    
                    Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                    
                    if (uri != null) {
                        try (OutputStream os = resolver.openOutputStream(uri)) {
                            if (os != null) {
                                document.writeTo(os);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error saving to Downloads: " + e.getMessage(), e);
                    // Continue anyway since we have the cache file
                }
            } else if (checkStoragePermission()) {
                // For older Android versions with permission, also save to Downloads
                try {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }
                    File downloadFile = new File(downloadsDir, fileName);
                    document.writeTo(new java.io.FileOutputStream(downloadFile));
                } catch (Exception e) {
                    Log.e(TAG, "Error saving to Downloads: " + e.getMessage(), e);
                    // Continue anyway since we have the cache file
                }
            }
            
            // Close the document
            document.close();
            
            // Show success message with file path
            Toast.makeText(requireContext(), "PDF saved to Downloads folder: " + fileName, Toast.LENGTH_LONG).show();
            
            // Open the PDF file
            openPdfFile(pdfFile);
        } catch (Exception e) {
            Log.e(TAG, "Error creating PDF: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error creating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Splits text into lines of specified maximum length
     */
    private String[] splitTextIntoLines(String text, int maxCharsPerLine) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxCharsPerLine) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }
    
    /**
     * Opens a PDF file using an external app
     */
    private void openPdfFile(File file) {
        try {
            if (!file.exists()) {
                Toast.makeText(requireContext(), "PDF file not found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Uri uri = FileProvider.getUriForFile(requireContext(), 
                    requireContext().getApplicationContext().getPackageName() + ".provider", file);
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "No PDF viewer app found. PDF saved to Downloads folder.", 
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening PDF: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error opening PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, export PDF
                TextView dateTimeText = step4View.findViewById(R.id.text_date_time);
                TextView recommendationDetailsText = step4View.findViewById(R.id.text_recommendation_details);
                float predictionScore = calculatePredictionScore();
                exportResultsToPdf(step4View, predictionScore, dateTimeText.getText().toString(), 
                        recommendationDetailsText.getText().toString());
            } else {
                // Permission denied
                Toast.makeText(requireContext(), "Storage permission is required to export PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_LOAD_EMG_FILE && resultCode == Activity.RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            // TODO: Implement loading EMG data from file
            Toast.makeText(requireContext(), "EMG file loaded: " + fileUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
            
            // For demo purposes, create dummy EMG data
            createDummyEmgData();
        }
    }
}
