package com.dianerverotect.alerts;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.dianerverotect.R;

import java.util.Locale;

/**
 * Fragment for configuring alert settings
 */
public class AlertsSettingsFragment extends Fragment {
    
    private ReminderPreferences preferences;
    private NotificationHelper notificationHelper;
    
    // UI components
    private SwitchCompat glucoseReminderSwitch;
    private ConstraintLayout glucoseReminderTimeLayout;
    private TextView glucoseReminderTimeText;
    private SwitchCompat vitalDataUpdatesSwitch;
    private SwitchCompat gloveConnectionSwitch;
    private SwitchCompat technicalAlertsSwitch;
    private Toolbar toolbar;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts_settings, container, false);
        
        // Initialize helpers
        preferences = new ReminderPreferences(requireContext());
        notificationHelper = new NotificationHelper(requireContext());
        
        // Initialize views
        glucoseReminderSwitch = view.findViewById(R.id.glucose_reminder_switch);
        glucoseReminderTimeLayout = view.findViewById(R.id.glucose_reminder_time_layout);
        glucoseReminderTimeText = view.findViewById(R.id.glucose_reminder_time_text);
        vitalDataUpdatesSwitch = view.findViewById(R.id.vital_data_updates_switch);
        gloveConnectionSwitch = view.findViewById(R.id.glove_connection_switch);
        technicalAlertsSwitch = view.findViewById(R.id.technical_alerts_switch);
        toolbar = view.findViewById(R.id.alerts_toolbar);
        
        // Setup toolbar with back button
        setupToolbar();
        
        // Load saved preferences
        loadSavedPreferences();
        
        // Set up listeners
        setupListeners();
        
        return view;
    }
    
    /**
     * Set up the toolbar with a back button
     */
    private void setupToolbar() {
        if (toolbar != null) {
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
            
            if (((AppCompatActivity) requireActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
                ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.alerts_settings_title);
            }
            
            toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        }
    }
    
    /**
     * Load saved preferences and update UI
     */
    private void loadSavedPreferences() {
        // Glucose reminder
        boolean glucoseReminderEnabled = preferences.isGlucoseReminderEnabled();
        glucoseReminderSwitch.setChecked(glucoseReminderEnabled);
        
        // Update time selector enabled state
        updateGlucoseTimeSelector(glucoseReminderEnabled);
        
        // Set the saved time
        int hour = preferences.getGlucoseReminderHour();
        int minute = preferences.getGlucoseReminderMinute();
        setTimeText(hour, minute);
        
        // Other alerts
        vitalDataUpdatesSwitch.setChecked(preferences.isVitalDataRemindersEnabled());
        gloveConnectionSwitch.setChecked(preferences.isGloveConnectionAlertsEnabled());
        technicalAlertsSwitch.setChecked(preferences.isTechnicalAlertsEnabled());
    }
    
    /**
     * Set up click listeners
     */
    private void setupListeners() {
        // Glucose reminder switch
        glucoseReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setGlucoseReminderEnabled(isChecked);
            updateGlucoseTimeSelector(isChecked);
            
            if (isChecked) {
                // Schedule the reminder
                int hour = preferences.getGlucoseReminderHour();
                int minute = preferences.getGlucoseReminderMinute();
                notificationHelper.scheduleGlucoseReminder(hour, minute, true);
                Toast.makeText(requireContext(), 
                        "Glucose reminder set for " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute), 
                        Toast.LENGTH_SHORT).show();
            } else {
                // Cancel the reminder
                notificationHelper.cancelGlucoseReminder();
                Toast.makeText(requireContext(), "Glucose reminder disabled", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Time picker
        glucoseReminderTimeLayout.setOnClickListener(v -> {
            if (glucoseReminderSwitch.isChecked()) {
                showTimePickerDialog();
            }
        });
        
        // Vital data updates switch
        vitalDataUpdatesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setVitalDataRemindersEnabled(isChecked);
            Toast.makeText(requireContext(), 
                    isChecked ? "Vital data alerts enabled" : "Vital data alerts disabled", 
                    Toast.LENGTH_SHORT).show();
        });
        
        // Glove connection switch
        gloveConnectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setGloveConnectionAlertsEnabled(isChecked);
            Toast.makeText(requireContext(), 
                    isChecked ? "Glove connection alerts enabled" : "Glove connection alerts disabled", 
                    Toast.LENGTH_SHORT).show();
        });
        
        // Technical alerts switch
        technicalAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setTechnicalAlertsEnabled(isChecked);
            Toast.makeText(requireContext(), 
                    isChecked ? "Technical alerts enabled" : "Technical alerts disabled", 
                    Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * Update time selector UI
     */
    private void updateGlucoseTimeSelector(boolean enabled) {
        glucoseReminderTimeLayout.setAlpha(enabled ? 1.0f : 0.5f);
        glucoseReminderTimeLayout.setClickable(enabled);
    }
    
    /**
     * Set the time text
     */
    private void setTimeText(int hour, int minute) {
        glucoseReminderTimeText.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
    }
    
    /**
     * Show time picker dialog
     */
    private void showTimePickerDialog() {
        int hour = preferences.getGlucoseReminderHour();
        int minute = preferences.getGlucoseReminderMinute();
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minuteOfHour) -> {
                    // Save the selected time
                    preferences.saveGlucoseReminderTime(hourOfDay, minuteOfHour);
                    
                    // Update UI
                    setTimeText(hourOfDay, minuteOfHour);
                    
                    // Reschedule the reminder
                    notificationHelper.cancelGlucoseReminder();
                    notificationHelper.scheduleGlucoseReminder(hourOfDay, minuteOfHour, true);
                    
                    Toast.makeText(requireContext(), 
                            "Glucose reminder updated to " + String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour), 
                            Toast.LENGTH_SHORT).show();
                },
                hour, minute, true);
        
        timePickerDialog.setTitle("Select Time");
        timePickerDialog.show();
    }
} 