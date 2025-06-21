package com.dianerverotect;

import android.app.Application;
import com.google.firebase.FirebaseApp;

/**
 * Application class for DiaNerve Protect
 * Handles application-wide initialization
 */
public class DiaNerveApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Any other app-wide initialization can go here
    }
}
