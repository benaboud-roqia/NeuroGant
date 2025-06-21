package com.dianerverotect.alerts;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.dianerverotect.MainActivity;
import com.dianerverotect.R;

import java.util.Calendar;

/**
 * Helper class to manage all notifications in the app.
 * Handles glucose reminders, vital data updates, and glove connection alerts.
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    
    // Notification Channel IDs
    public static final String CHANNEL_GLUCOSE_REMINDERS = "glucose_reminders";
    public static final String CHANNEL_VITAL_DATA = "vital_data";
    public static final String CHANNEL_GLOVE_CONNECTION = "glove_connection";
    public static final String CHANNEL_TECHNICAL = "technical_alerts";
    
    // Notification IDs
    public static final int NOTIFICATION_ID_GLUCOSE_REMINDER = 1001;
    public static final int NOTIFICATION_ID_VITAL_DATA = 1002;
    public static final int NOTIFICATION_ID_GLOVE_CONNECTION = 1003;
    public static final int NOTIFICATION_ID_GLOVE_BATTERY = 1004;
    public static final int NOTIFICATION_ID_APP_UPDATE = 1005;
    public static final int NOTIFICATION_ID_SYNC_ISSUE = 1006;
    
    // Request codes for pending intents
    public static final int REQUEST_CODE_GLUCOSE_REMINDER = 2001;
    
    private final Context context;
    private final NotificationManagerCompat notificationManager;
    
    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        
        // Create notification channels for Android O and above
        createNotificationChannels();
    }
    
    /**
     * Create notification channels for different alert types.
     * Only needed for Android O (API 26) and above.
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Glucose reminders channel (high importance)
            NotificationChannel glucoseChannel = new NotificationChannel(
                    CHANNEL_GLUCOSE_REMINDERS,
                    "Glucose Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            glucoseChannel.setDescription("Reminders to check your glucose level");
            glucoseChannel.enableLights(true);
            glucoseChannel.setLightColor(Color.RED);
            glucoseChannel.enableVibration(true);
            
            // Vital data channel (default importance)
            NotificationChannel vitalDataChannel = new NotificationChannel(
                    CHANNEL_VITAL_DATA,
                    "Vital Data Updates",
                    NotificationManager.IMPORTANCE_DEFAULT);
            vitalDataChannel.setDescription("Updates about your vital health data");
            
            // Glove connection channel (high importance)
            NotificationChannel gloveConnectionChannel = new NotificationChannel(
                    CHANNEL_GLOVE_CONNECTION,
                    "Glove Connection Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            gloveConnectionChannel.setDescription("Alerts about glove connection status");
            gloveConnectionChannel.enableLights(true);
            gloveConnectionChannel.setLightColor(Color.BLUE);
            gloveConnectionChannel.enableVibration(true);
            
            // Technical alerts channel (high importance)
            NotificationChannel technicalChannel = new NotificationChannel(
                    CHANNEL_TECHNICAL,
                    "Technical Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            technicalChannel.setDescription("Technical notifications about device and app");
            technicalChannel.enableLights(true);
            technicalChannel.setLightColor(Color.YELLOW);
            technicalChannel.enableVibration(true);
            
            // Register all channels
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(glucoseChannel);
            manager.createNotificationChannel(vitalDataChannel);
            manager.createNotificationChannel(gloveConnectionChannel);
            manager.createNotificationChannel(technicalChannel);
            
            Log.d(TAG, "Notification channels created");
        }
    }
    
    /**
     * Schedule a glucose reminder at the specified time
     * @param hour Hour of day (24-hour format)
     * @param minute Minute
     * @param isRepeating Whether the reminder should repeat daily
     */
    public void scheduleGlucoseReminder(int hour, int minute, boolean isRepeating) {
        // Create intent for the notification
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_GLUCOSE_REMINDER);
        
        // Create pending intent with unique request code
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_GLUCOSE_REMINDER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Set the alarm time
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        
        // If the time is in the past, add one day
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        // Schedule the alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (isRepeating) {
                // Repeat daily at the same time
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent);
                Log.d(TAG, "Repeating glucose reminder scheduled for " + hour + ":" + minute);
            } else {
                // One-time reminder
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent);
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent);
                }
                Log.d(TAG, "One-time glucose reminder scheduled for " + hour + ":" + minute);
            }
        }
    }
    
    /**
     * Cancel a scheduled glucose reminder
     */
    public void cancelGlucoseReminder() {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(NotificationReceiver.ACTION_GLUCOSE_REMINDER);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_GLUCOSE_REMINDER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Glucose reminder canceled");
        }
    }
    
    /**
     * Show a notification for glucose level check
     */
    public void showGlucoseReminderNotification() {
        // Create an intent to open the app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GLUCOSE_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification_glucose)
                .setContentTitle(context.getString(R.string.glucose_reminder_title))
                .setContentText(context.getString(R.string.glucose_reminder_message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500});
        
        // Show the notification
        try {
            notificationManager.notify(NOTIFICATION_ID_GLUCOSE_REMINDER, builder.build());
            Log.d(TAG, "Glucose reminder notification shown");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for showing notification: " + e.getMessage());
        }
    }
    
    /**
     * Show a notification for vital data update
     * @param title Notification title
     * @param message Notification message
     */
    public void showVitalDataNotification(String title, String message) {
        // Create an intent to open the app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_VITAL_DATA)
                .setSmallIcon(R.drawable.ic_notification_vital)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        // Show the notification
        try {
            notificationManager.notify(NOTIFICATION_ID_VITAL_DATA, builder.build());
            Log.d(TAG, "Vital data notification shown: " + title);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for showing notification: " + e.getMessage());
        }
    }
    
    /**
     * Show a notification for glove connection status
     * @param isConnected Whether the glove is connected
     */
    public void showGloveConnectionNotification(boolean isConnected) {
        // Create an intent to open the app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        // Build title and message based on connection status
        String title = isConnected ? 
                context.getString(R.string.glove_connected_title) : 
                context.getString(R.string.glove_disconnected_title);
        
        String message = isConnected ? 
                context.getString(R.string.glove_connected_message) : 
                context.getString(R.string.glove_disconnected_message);
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GLOVE_CONNECTION)
                .setSmallIcon(R.drawable.ic_notification_glove)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        if (!isConnected) {
            // Add vibration pattern for disconnection
            builder.setVibrate(new long[]{0, 500, 200, 500});
        }
        
        // Show the notification
        try {
            notificationManager.notify(NOTIFICATION_ID_GLOVE_CONNECTION, builder.build());
            Log.d(TAG, "Glove connection notification shown: " + (isConnected ? "Connected" : "Disconnected"));
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for showing notification: " + e.getMessage());
        }
    }
    
    /**
     * Show a notification for low glove battery
     * @param batteryPercentage Current battery percentage
     */
    public void showLowBatteryNotification(int batteryPercentage) {
        // Check if technical alerts are enabled
        ReminderPreferences preferences = new ReminderPreferences(context);
        if (!preferences.isTechnicalAlertsEnabled()) {
            return;
        }
        
        // Create an intent to open the app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        String message = context.getString(R.string.glove_battery_low_message) + 
                " Current level: " + batteryPercentage + "%";
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_TECHNICAL)
                .setSmallIcon(R.drawable.ic_notification_battery)
                .setContentTitle(context.getString(R.string.glove_battery_low_title))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setColor(Color.RED);
        
        // Show the notification
        try {
            notificationManager.notify(NOTIFICATION_ID_GLOVE_BATTERY, builder.build());
            Log.d(TAG, "Low battery notification shown");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for showing notification: " + e.getMessage());
        }
    }
    
    /**
     * Show a notification for app update availability
     * @param versionName The new app version
     */
    public void showAppUpdateNotification(String versionName) {
        // Check if technical alerts are enabled
        ReminderPreferences preferences = new ReminderPreferences(context);
        if (!preferences.isTechnicalAlertsEnabled()) {
            return;
        }
        
        // Create an intent to open the app update page
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("com.dianerverotect.action.APP_UPDATE");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        String message = context.getString(R.string.app_update_message) + 
                " Version " + versionName + " is now available.";
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_TECHNICAL)
                .setSmallIcon(R.drawable.ic_notification_update)
                .setContentTitle(context.getString(R.string.app_update_title))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        // Show the notification
        try {
            notificationManager.notify(NOTIFICATION_ID_APP_UPDATE, builder.build());
            Log.d(TAG, "App update notification shown");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for showing notification: " + e.getMessage());
        }
    }
    
    /**
     * Show a notification for sync/connectivity issues
     * @param issueType Type of issue (e.g., "Bluetooth", "Wi-Fi")
     */
    public void showSyncIssueNotification(String issueType) {
        // Check if technical alerts are enabled
        ReminderPreferences preferences = new ReminderPreferences(context);
        if (!preferences.isTechnicalAlertsEnabled()) {
            return;
        }
        
        // Create an intent to open the app's settings page
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("com.dianerverotect.action.CONNECTIVITY_SETTINGS");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        String message = issueType + " " + context.getString(R.string.sync_issue_message);
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_TECHNICAL)
                .setSmallIcon(R.drawable.ic_notification_sync_problem)
                .setContentTitle(context.getString(R.string.sync_issue_title))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 300, 200, 300});
        
        // Show the notification
        try {
            notificationManager.notify(NOTIFICATION_ID_SYNC_ISSUE, builder.build());
            Log.d(TAG, "Sync issue notification shown: " + issueType);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for showing notification: " + e.getMessage());
        }
    }
} 