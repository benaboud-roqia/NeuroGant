package com.dianerverotect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NeuropathyHistoryAdapter extends RecyclerView.Adapter<NeuropathyHistoryAdapter.NeuropathyHistoryViewHolder> {

    private final List<NeuropathyHistoryFragment.NeuropathyTestResult> testResults;
    private final Context context;

    public NeuropathyHistoryAdapter(Context context, List<NeuropathyHistoryFragment.NeuropathyTestResult> testResults) {
        this.context = context;
        this.testResults = testResults;
    }

    @NonNull
    @Override
    public NeuropathyHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_neuropathy_history, parent, false);
        return new NeuropathyHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NeuropathyHistoryViewHolder holder, int position) {
        NeuropathyHistoryFragment.NeuropathyTestResult result = testResults.get(position);
        
        // Format the timestamp to a more readable date format
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(result.getTimestamp()));
        
        // Set test date with a more descriptive label
        holder.testDateText.setText("Test: " + formattedDate);
        
        // Set EMG score with improved visual feedback
        int emgScore = result.getEmgScore();
        holder.emgScoreProgress.setProgress(emgScore);
        holder.emgScoreValue.setText(emgScore + "/100");
        
        // Set color based on EMG score
        int scoreColor;
        if (emgScore >= 75) {
            scoreColor = context.getResources().getColor(android.R.color.holo_green_dark);
        } else if (emgScore >= 50) {
            scoreColor = context.getResources().getColor(android.R.color.holo_orange_light);
        } else {
            scoreColor = context.getResources().getColor(android.R.color.holo_red_light);
        }
        holder.emgScoreValue.setTextColor(scoreColor);
        
        // Set temperature sensation with improved visual feedback
        String temperatureResponse = result.isTemperatureSensation() ? 
                "Normal \u2713" : "Reduced \u26a0";
        holder.temperatureSensationText.setText(temperatureResponse);
        holder.temperatureSensationText.setTextColor(result.isTemperatureSensation() ? 
                context.getResources().getColor(android.R.color.holo_green_dark) : 
                context.getResources().getColor(android.R.color.holo_orange_dark));
        
        // Set pressure sensation with improved visual feedback
        String pressureResponse = result.isPressureSensation() ? 
                "Normal \u2713" : "Reduced \u26a0";
        holder.pressureSensationText.setText(pressureResponse);
        holder.pressureSensationText.setTextColor(result.isPressureSensation() ? 
                context.getResources().getColor(android.R.color.holo_green_dark) : 
                context.getResources().getColor(android.R.color.holo_orange_dark));
        
        // Set AI analysis results
        // Calculate risk level based on EMG score and sensations
        String riskLevel;
        String riskScore;
        String riskDescription;
        int riskColor;
        
        if (emgScore >= 75 && result.isTemperatureSensation() && result.isPressureSensation()) {
            riskLevel = "Risk Level: Low";
            riskScore = "Risk Score: 0.25";
            riskDescription = "Based on your test results, your risk of diabetic neuropathy is low.";
            riskColor = context.getResources().getColor(android.R.color.holo_green_dark);
        } else if (emgScore >= 50 || (result.isTemperatureSensation() && result.isPressureSensation())) {
            riskLevel = "Risk Level: Moderate";
            riskScore = "Risk Score: 0.50";
            riskDescription = "Based on your test results, you have a moderate risk of diabetic neuropathy. Regular monitoring is recommended.";
            riskColor = context.getResources().getColor(android.R.color.holo_orange_light);
        } else {
            riskLevel = "Risk Level: High";
            riskScore = "Risk Score: 0.85";
            riskDescription = "Based on your test results, you have a high risk of diabetic neuropathy. Consult with a healthcare professional.";
            riskColor = context.getResources().getColor(android.R.color.holo_red_light);
        }
        
        holder.riskLevelText.setText(riskLevel);
        holder.riskLevelText.setTextColor(riskColor);
        holder.riskScoreText.setText(riskScore);
        holder.riskDescriptionText.setText(riskDescription);
        
        // Set recommendation with improved formatting
        String recommendation = result.getRecommendation();
        if (recommendation == null || recommendation.isEmpty()) {
            recommendation = "No specific recommendations provided for this test.";
        }
        holder.recommendationText.setText(recommendation);
        
        // Set up questionnaire responses with improved styling
        List<NeuropathyHistoryFragment.QuestionResponse> questionResponses = result.getQuestionResponses();
        if (questionResponses != null && !questionResponses.isEmpty()) {
            holder.questionnaireRecyclerView.setVisibility(View.VISIBLE);
            QuestionnaireAdapter questionnaireAdapter = new QuestionnaireAdapter(questionResponses);
            holder.questionnaireRecyclerView.setAdapter(questionnaireAdapter);
        } else {
            holder.questionnaireRecyclerView.setVisibility(View.GONE);
        }
        
        // Set up expand/collapse functionality with smooth animation
        holder.contentLayout.setVisibility(View.GONE); // Initially collapsed
        
        holder.expandButton.setOnClickListener(v -> {
            boolean isExpanded = holder.contentLayout.getVisibility() == View.VISIBLE;
            
            // Use animation for smoother transition
            if (isExpanded) {
                holder.contentLayout.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> holder.contentLayout.setVisibility(View.GONE));
                holder.expandButton.animate().rotation(0).setDuration(200);
            } else {
                holder.contentLayout.setAlpha(0f);
                holder.contentLayout.setVisibility(View.VISIBLE);
                holder.contentLayout.animate().alpha(1f).setDuration(200);
                holder.expandButton.animate().rotation(180).setDuration(200);
            }
        });
        
        // Add a subtle ripple effect to the header for better UX
        holder.headerLayout.setOnClickListener(v -> holder.expandButton.performClick());
    }

    @Override
    public int getItemCount() {
        return testResults.size();
    }

    static class NeuropathyHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView testDateText;
        ImageButton expandButton;
        View contentLayout;
        View headerLayout;
        
        ProgressBar emgScoreProgress;
        TextView emgScoreValue;
        TextView temperatureSensationText;
        TextView pressureSensationText;
        TextView recommendationText;
        RecyclerView questionnaireRecyclerView;
        
        // AI Analysis Results fields
        TextView riskLevelText;
        TextView riskScoreText;
        TextView riskDescriptionText;

        NeuropathyHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            testDateText = itemView.findViewById(R.id.text_test_date);
            expandButton = itemView.findViewById(R.id.button_expand);
            contentLayout = itemView.findViewById(R.id.content_layout);
            headerLayout = itemView.findViewById(R.id.header_layout);
            
            emgScoreProgress = itemView.findViewById(R.id.emg_score_progress);
            emgScoreValue = itemView.findViewById(R.id.emg_score_value);
            temperatureSensationText = itemView.findViewById(R.id.text_temperature_sensation);
            pressureSensationText = itemView.findViewById(R.id.text_pressure_sensation);
            recommendationText = itemView.findViewById(R.id.text_recommendation);
            questionnaireRecyclerView = itemView.findViewById(R.id.recycler_questionnaire);
            
            // Initialize AI Analysis Results fields
            riskLevelText = itemView.findViewById(R.id.text_risk_level);
            riskScoreText = itemView.findViewById(R.id.text_risk_score);
            riskDescriptionText = itemView.findViewById(R.id.text_risk_description);
            
            // Set up nested RecyclerView
            questionnaireRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }
    }
    
    // Adapter for questionnaire responses
    private static class QuestionnaireAdapter extends RecyclerView.Adapter<QuestionnaireAdapter.QuestionnaireViewHolder> {
        
        private final List<NeuropathyHistoryFragment.QuestionResponse> responses;
        
        QuestionnaireAdapter(List<NeuropathyHistoryFragment.QuestionResponse> responses) {
            this.responses = responses;
        }
        
        @NonNull
        @Override
        public QuestionnaireViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_questionnaire_response, parent, false);
            return new QuestionnaireViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull QuestionnaireViewHolder holder, int position) {
            NeuropathyHistoryFragment.QuestionResponse response = responses.get(position);
            
            holder.questionText.setText(response.getQuestion());
            
            String responseText = response.isResponse() ? "Yes" : "No";
            holder.responseText.setText(responseText);
            
            // Set background color based on response
            int backgroundColor = response.isResponse() ? 
                    holder.itemView.getResources().getColor(android.R.color.holo_red_light) : 
                    holder.itemView.getResources().getColor(android.R.color.holo_green_light);
            holder.responseText.getBackground().setTint(backgroundColor);
        }
        
        @Override
        public int getItemCount() {
            return responses.size();
        }
        
        static class QuestionnaireViewHolder extends RecyclerView.ViewHolder {
            TextView questionText;
            TextView responseText;
            
            QuestionnaireViewHolder(@NonNull View itemView) {
                super(itemView);
                questionText = itemView.findViewById(R.id.text_question);
                responseText = itemView.findViewById(R.id.text_response);
            }
        }
    }
}
