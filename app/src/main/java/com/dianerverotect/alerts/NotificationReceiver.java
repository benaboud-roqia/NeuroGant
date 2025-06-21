package com.dianerverotect.alerts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver for handling notification-related intents
 * such as alarm triggers for reminders.
 */
public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    
    // Intent action constants
    public static final String ACTION_GLUCOSE_REMINDER = "com.dianerverotect.action.GLUCOSE_REMINDER";
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intent: " + intent.getAction());
        
        // Create notification helper
        NotificationHelper notificationHelper = new NotificationHelper(context);
        
        // Handle different intents
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_GLUCOSE_REMINDER:
                    // Show glucose reminder notification
                    notificationHelper.showGlucoseReminderNotification();
                    break;
                    
                case ACTION_BOOT_COMPLETED:
                    // Reschedule all alarms after device reboot
                    rescheduleAlarmsAfterReboot(context);
                    break;
            }
        }
    }
    
    /**
     * Reschedule alarms after device reboot
     * This ensures reminders are not lost when device is restarted
     */
    private void rescheduleAlarmsAfterReboot(Context context) {
        Log.d(TAG, "Rescheduling alarms after device reboot");
        
        // Get saved reminder settings from preferences
        ReminderPreferences preferences = new ReminderPreferences(context);
        
        // Reschedule glucose reminder if enabled
        if (preferences.isGlucoseReminderEnabled()) {
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.scheduleGlucoseReminder(
                    preferences.getGlucoseReminderHour(),
                    preferences.getGlucoseReminderMinute(),
                    true);
            Log.d(TAG, "Glucose reminder rescheduled after reboot");
        }
    }
} 