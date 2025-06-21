package com.dianerverotect.alerts;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preference manager for storing reminder settings
 */
public class ReminderPreferences {
    private static final String PREF_NAME = "reminder_preferences";
    
    // Keys for preferences
    private static final String KEY_GLUCOSE_REMINDER_ENABLED = "glucose_reminder_enabled";
    private static final String KEY_GLUCOSE_REMINDER_HOUR = "glucose_reminder_hour";
    private static final String KEY_GLUCOSE_REMINDER_MINUTE = "glucose_reminder_minute";
    private static final String KEY_VITAL_DATA_REMINDERS_ENABLED = "vital_data_reminders_enabled";
    private static final String KEY_GLOVE_CONNECTION_ALERTS_ENABLED = "glove_connection_alerts_enabled";
    private static final String KEY_TECHNICAL_ALERTS_ENABLED = "technical_alerts_enabled";
    
    // Default values
    private static final boolean DEFAULT_REMINDER_ENABLED = false;
    private static final int DEFAULT_REMINDER_HOUR = 9; // 9 AM
    private static final int DEFAULT_REMINDER_MINUTE = 0;
    
    private final SharedPreferences preferences;
    
    public ReminderPreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Glucose reminder settings
     */
    public boolean isGlucoseReminderEnabled() {
        return preferences.getBoolean(KEY_GLUCOSE_REMINDER_ENABLED, DEFAULT_REMINDER_ENABLED);
    }
    
    public void setGlucoseReminderEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_GLUCOSE_REMINDER_ENABLED, enabled).apply();
    }
    
    public int getGlucoseReminderHour() {
        return preferences.getInt(KEY_GLUCOSE_REMINDER_HOUR, DEFAULT_REMINDER_HOUR);
    }
    
    public void setGlucoseReminderHour(int hour) {
        preferences.edit().putInt(KEY_GLUCOSE_REMINDER_HOUR, hour).apply();
    }
    
    public int getGlucoseReminderMinute() {
        return preferences.getInt(KEY_GLUCOSE_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE);
    }
    
    public void setGlucoseReminderMinute(int minute) {
        preferences.edit().putInt(KEY_GLUCOSE_REMINDER_MINUTE, minute).apply();
    }
    
    /**
     * Save glucose reminder time and enable it
     */
    public void saveGlucoseReminderTime(int hour, int minute) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_GLUCOSE_REMINDER_HOUR, hour);
        editor.putInt(KEY_GLUCOSE_REMINDER_MINUTE, minute);
        editor.putBoolean(KEY_GLUCOSE_REMINDER_ENABLED, true);
        editor.apply();
    }
    
    /**
     * Other notification settings
     */
    public boolean isVitalDataRemindersEnabled() {
        return preferences.getBoolean(KEY_VITAL_DATA_REMINDERS_ENABLED, true);
    }
    
    public void setVitalDataRemindersEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_VITAL_DATA_REMINDERS_ENABLED, enabled).apply();
    }
    
    public boolean isGloveConnectionAlertsEnabled() {
        return preferences.getBoolean(KEY_GLOVE_CONNECTION_ALERTS_ENABLED, true);
    }
    
    public void setGloveConnectionAlertsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_GLOVE_CONNECTION_ALERTS_ENABLED, enabled).apply();
    }
    
    /**
     * Technical alerts settings
     */
    public boolean isTechnicalAlertsEnabled() {
        return preferences.getBoolean(KEY_TECHNICAL_ALERTS_ENABLED, true);
    }
    
    public void setTechnicalAlertsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_TECHNICAL_ALERTS_ENABLED, enabled).apply();
    }
} 