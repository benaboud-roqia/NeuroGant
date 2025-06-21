package com.dianerverotect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ALSHistoryAdapter extends RecyclerView.Adapter<ALSHistoryAdapter.ALSHistoryViewHolder> {

    private final List<ALSHistoryFragment.ALSTestResult> testResults;
    private final Context context;

    public ALSHistoryAdapter(Context context, List<ALSHistoryFragment.ALSTestResult> testResults) {
        this.context = context;
        this.testResults = testResults;
    }

    @NonNull
    @Override
    public ALSHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_als_history, parent, false);
        return new ALSHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ALSHistoryViewHolder holder, int position) {
        ALSHistoryFragment.ALSTestResult result = testResults.get(position);
        
        // Format the timestamp to a readable date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(result.getTimestamp()));
        
        // Set test date
        holder.testDateText.setText(formattedDate);
        
        // Set EMG scores
        int emgAmplitude = result.getEmgAmplitude();
        int emgFrequency = result.getEmgFrequency();
        int emgDuration = result.getEmgDuration();
        int emgOverallScore = result.getEmgOverallScore();
        
        holder.emgAmplitudeProgress.setProgress(emgAmplitude);
        holder.emgFrequencyProgress.setProgress(emgFrequency);
        holder.emgDurationProgress.setProgress(emgDuration);
        
        holder.emgAmplitudeValue.setText(emgAmplitude + " μV");
        holder.emgFrequencyValue.setText(emgFrequency + " Hz");
        holder.emgDurationValue.setText(emgDuration + " ms");
        holder.emgOverallValue.setText(emgOverallScore + "/100");
        
        // Set prediction result
        String predictionResult = result.getPredictionResult();
        String predictionDescription = result.getPredictionDescription();
        int predictionConfidence = result.getPredictionConfidence();
        
        if (predictionResult == null || predictionResult.isEmpty()) {
            predictionResult = "Unknown";
        }
        
        if (predictionDescription == null || predictionDescription.isEmpty()) {
            predictionDescription = "No detailed prediction information available.";
        }
        
        holder.predictionTitle.setText(predictionResult);
        holder.predictionDescription.setText(predictionDescription);
        holder.predictionConfidence.setText("Confidence: " + predictionConfidence + "%");
        
        // Set prediction card color based on result
        int cardColor;
        int textColor;
        int iconTint;
        
        if (predictionResult.toLowerCase().contains("low") || 
            predictionResult.toLowerCase().contains("negative")) {
            // Low risk / negative result
            cardColor = context.getResources().getColor(android.R.color.holo_green_light);
            textColor = context.getResources().getColor(android.R.color.holo_green_dark);
            iconTint = context.getResources().getColor(android.R.color.holo_green_dark);
            holder.predictionIcon.setImageResource(android.R.drawable.ic_menu_info_details);
        } else if (predictionResult.toLowerCase().contains("medium") || 
                  predictionResult.toLowerCase().contains("moderate")) {
            // Medium risk
            cardColor = context.getResources().getColor(android.R.color.holo_orange_light);
            textColor = context.getResources().getColor(android.R.color.holo_orange_dark);
            iconTint = context.getResources().getColor(android.R.color.holo_orange_dark);
            holder.predictionIcon.setImageResource(android.R.drawable.ic_dialog_alert);
        } else if (predictionResult.toLowerCase().contains("high") || 
                  predictionResult.toLowerCase().contains("positive")) {
            // High risk / positive result
            cardColor = context.getResources().getColor(android.R.color.holo_red_light);
            textColor = context.getResources().getColor(android.R.color.holo_red_dark);
            iconTint = context.getResources().getColor(android.R.color.holo_red_dark);
            holder.predictionIcon.setImageResource(android.R.drawable.ic_dialog_alert);
        } else {
            // Default/unknown
            cardColor = context.getResources().getColor(android.R.color.darker_gray);
            textColor = context.getResources().getColor(android.R.color.black);
            iconTint = context.getResources().getColor(android.R.color.black);
            holder.predictionIcon.setImageResource(android.R.drawable.ic_menu_help);
        }
        
        holder.predictionCard.setCardBackgroundColor(cardColor);
        holder.predictionTitle.setTextColor(textColor);
        holder.predictionDescription.setTextColor(textColor);
        holder.predictionIcon.setColorFilter(iconTint);
        
        // Set clinical notes
        String clinicalNotes = result.getClinicalNotes();
        if (clinicalNotes == null || clinicalNotes.isEmpty()) {
            clinicalNotes = "No clinical notes available for this test.";
        }
        holder.clinicalNotesText.setText(clinicalNotes);
        
        // Set up expand/collapse functionality
        holder.contentLayout.setVisibility(View.GONE); // Initially collapsed
        
        holder.expandButton.setOnClickListener(v -> {
            boolean isExpanded = holder.contentLayout.getVisibility() == View.VISIBLE;
            holder.contentLayout.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            holder.expandButton.setRotation(isExpanded ? 0 : 180); // Rotate arrow
        });
    }

    @Override
    public int getItemCount() {
        return testResults.size();
    }

    static class ALSHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView testDateText;
        ImageButton expandButton;
        View contentLayout;
        
        // Prediction section
        ImageView predictionIcon;
        TextView predictionTitle;
        TextView predictionDescription;
        TextView predictionConfidence;
        androidx.cardview.widget.CardView predictionCard;
        
        // EMG section
        ProgressBar emgAmplitudeProgress;
        ProgressBar emgFrequencyProgress;
        ProgressBar emgDurationProgress;
        TextView emgAmplitudeValue;
        TextView emgFrequencyValue;
        TextView emgDurationValue;
        TextView emgOverallValue;
        
        // Clinical notes
        TextView clinicalNotesText;

        ALSHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            testDateText = itemView.findViewById(R.id.text_test_date);
            expandButton = itemView.findViewById(R.id.button_expand);
            contentLayout = itemView.findViewById(R.id.content_layout);
            
            // Prediction section
            predictionIcon = itemView.findViewById(R.id.prediction_icon);
            predictionTitle = itemView.findViewById(R.id.prediction_title);
            predictionDescription = itemView.findViewById(R.id.prediction_description);
            predictionConfidence = itemView.findViewById(R.id.prediction_confidence);
            predictionCard = itemView.findViewById(R.id.prediction_card);
            
            // EMG section
            emgAmplitudeProgress = itemView.findViewById(R.id.emg_amplitude_progress);
            emgFrequencyProgress = itemView.findViewById(R.id.emg_frequency_progress);
            emgDurationProgress = itemView.findViewById(R.id.emg_duration_progress);
            emgAmplitudeValue = itemView.findViewById(R.id.emg_amplitude_value);
            emgFrequencyValue = itemView.findViewById(R.id.emg_frequency_value);
            emgDurationValue = itemView.findViewById(R.id.emg_duration_value);
            emgOverallValue = itemView.findViewById(R.id.emg_overall_value);
            
            // Clinical notes
            clinicalNotesText = itemView.findViewById(R.id.text_clinical_notes);
        }
    }
}
