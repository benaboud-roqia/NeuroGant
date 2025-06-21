package com.dianerverotect;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast; // Import Toast
import java.util.Locale;

import androidx.activity.EdgeToEdge; // Keep EdgeToEdge if still desired, though AppBarLayout might handle some aspects
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
// Removed ViewCompat and WindowInsetsCompat imports as we'll remove the listener for now
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.widget.Toolbar; // Import Toolbar

import com.google.android.material.appbar.MaterialToolbar; // Import MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Base64;
import de.hdodenhof.circleimageview.CircleImageView;

import com.dianerverotect.BluetoothManager;
import com.dianerverotect.alerts.NotificationHelper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.widget.FrameLayout;

// Remove the problematic import

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    
    // Static instance for accessing MainActivity from other activities
    private static MainActivity instance;

    private MaterialToolbar topAppBar; // Changed to MaterialToolbar
    private BottomNavigationView bottomNavigationView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FragmentManager fragmentManager;
    private Fragment activeFragment;
    
    // Fragment instances
    private HomeFragment homeFragment;
    private HistoryFragment historyFragment;
    private SettingsFragment settingsFragment;
    private ALSFragment alsFragment; // Added ALSFragment instance
    private ChatbotFragment chatbotFragment; // Added ChatbotFragment instance
    private RewardsFragment rewardsFragment; // Ajout du fragment Récompenses
    
    // Disease selection
    private static final String PREFS_NAME = "DiseasePrefs";
    private static final String SELECTED_DISEASE = "selected_disease";
    private String selectedDisease;

    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;

    private BluetoothManager bluetoothManager;

    /**
     * Get the current instance of MainActivity
     * @return The current MainActivity instance or null if not available
     */
    public static MainActivity getInstance() {
        return instance;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set the static instance
        instance = this;
        
        // Apply saved language settings before setting content view
        applyLanguageSettings();
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Can be kept or removed depending on desired edge-to-edge behavior with AppBar
        setContentView(R.layout.activity_main);

        // Get the selected disease
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        selectedDisease = settings.getString(SELECTED_DISEASE, DiseaseSelectionActivity.DISEASE_DIABETIC_NEUROPATHY);
        
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadDrawerHeaderData();

        // --- Setup Toolbar ---
        topAppBar = findViewById(R.id.top_app_bar);
        setSupportActionBar(topAppBar);
        // Enable the navigation icon (hamburger)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Use the custom ic_menu drawable provided by the user
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu); // Changed to use custom drawable
            getSupportActionBar().setTitle("DiaNerveProtect"); // Keep the title
        }
        // ---------------------

        // Initialize fragments
        homeFragment = new HomeFragment();
        historyFragment = new HistoryFragment();
        settingsFragment = new SettingsFragment();
        alsFragment = new ALSFragment();
        chatbotFragment = new ChatbotFragment();
        rewardsFragment = new RewardsFragment(); // Initialisation du fragment Récompenses
        
        // Set the initial fragment based on selected disease
        fragmentManager = getSupportFragmentManager();
        
        // Log the selected disease for debugging
        Log.d("MainActivity", "Initial disease selection: " + selectedDisease);
        
        // Add all fragments and hide all except the active one
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // Add all fragments first
        transaction.add(R.id.fragment_container_view, homeFragment, "home");
        transaction.add(R.id.fragment_container_view, alsFragment, "als");
        transaction.add(R.id.fragment_container_view, chatbotFragment, "nervebot");
        transaction.add(R.id.fragment_container_view, historyFragment, "history");
        transaction.add(R.id.fragment_container_view, settingsFragment, "settings");
        transaction.add(R.id.fragment_container_view, rewardsFragment, "rewards"); // Ajout du fragment Récompenses
        
        // Hide all fragments initially
        transaction.hide(homeFragment);
        transaction.hide(alsFragment);
        transaction.hide(chatbotFragment);
        transaction.hide(historyFragment);
        transaction.hide(settingsFragment);
        transaction.hide(rewardsFragment); // Masquer le fragment Récompenses au départ
        
        // Show the appropriate fragment based on disease selection
        if (selectedDisease.equals(DiseaseSelectionActivity.DISEASE_ALS)) {
            // If ALS is selected, use ALSFragment as the main fragment
            Log.d("MainActivity", "Setting ALS fragment as active");
            activeFragment = alsFragment;
            transaction.show(alsFragment);
        } else {
            // Default to diabetic neuropathy - use HomeFragment as the main fragment
            Log.d("MainActivity", "Setting Home fragment as active");
            activeFragment = homeFragment;
            transaction.show(homeFragment);
        }
        
        transaction.commit();
        
        // Initialize the bottom navigation view
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);
        
        // Set the default selected item
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        // Handle navigation drawer item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new android.content.Intent(MainActivity.this, ProfileActivity.class));
            } else if (id == R.id.nav_settings) {
                Toast.makeText(MainActivity.this, "Settings clicked", Toast.LENGTH_SHORT).show();
                // startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (id == R.id.nav_history) {
                switchFragment(historyFragment);
            } else if (id == R.id.nav_questionnaire) {
                switchFragment(chatbotFragment); // ou un fragment dédié si tu veux
            } else if (id == R.id.nav_about) {
                Toast.makeText(MainActivity.this, "À propos de l'application", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // Initialize the BluetoothManager
        bluetoothManager = new BluetoothManager(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Bluetooth resources
        if (bluetoothManager != null) {
            bluetoothManager.cleanup();
        }
        // Clear the static instance
        instance = null;
    }

    /**
     * Navigates to the login screen
     */
    public void navigateToLoginScreen() {
        // Check if we have a login activity to navigate to
        try {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
        } catch (Exception e) {
            Log.e("MainActivity", "Error navigating to login: " + e.getMessage());
            // Show a login dialog instead
            showLoginDialog();
        }
    }

    /**
     * Shows a login dialog if no login activity is available
     */
    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_login, null);
        builder.setView(dialogView);
        
        final EditText emailInput = dialogView.findViewById(R.id.edit_email);
        final EditText passwordInput = dialogView.findViewById(R.id.edit_password);
        Button loginButton = dialogView.findViewById(R.id.button_login);
        Button cancelButton = dialogView.findViewById(R.id.button_cancel);
        
        final AlertDialog dialog = builder.create();
        
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show loading
            Toast.makeText(MainActivity.this, "Logging in...", Toast.LENGTH_SHORT).show();
            
            // Attempt to sign in
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        // Refresh user data
                        loadDrawerHeaderData();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("MainActivity", "Login failed", e);
                    });
        });
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    // Removed onCreateOptionsMenu as we are not using the right-side options menu anymore

    private void loadDrawerHeaderData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference profileRef = usersRef.child(userId);

        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = "";
                String email = "";
                if (snapshot.child("fullName").exists()) {
                    username = snapshot.child("fullName").getValue(String.class);
                }
                if (snapshot.child("email").exists()) {
                    email = snapshot.child("email").getValue(String.class);
                }

                // Access header views
                android.view.View headerView = navigationView.getHeaderView(0);
                TextView usernameText = headerView.findViewById(R.id.drawer_username);
                TextView emailText = headerView.findViewById(R.id.drawer_email);
                CircleImageView profileImage = headerView.findViewById(R.id.drawer_profile_image);

                usernameText.setText(username);
                emailText.setText(email);
                
                // Load profile image if available
                if (snapshot.child("profile").exists() && 
                    snapshot.child("profile").child("profileImageUrl").exists()) {
                    String imageData = snapshot.child("profile").child("profileImageUrl").getValue(String.class);
                    if (imageData != null && !imageData.isEmpty()) {
                        try {
                            // Try to decode as Base64
                            byte[] decodedString = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                decodedString = Base64.getDecoder().decode(imageData);
                            }
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            
                            if (decodedBitmap != null) {
                                // Successfully decoded as Base64
                                Log.d("MainActivity", "Successfully loaded profile image from Base64");
                                profileImage.setImageBitmap(decodedBitmap);
                            } else {
                                // Fallback to Glide
                                Log.d("MainActivity", "Failed to decode Base64, trying as URL");
                                Glide.with(MainActivity.this)
                                        .load(imageData)
                                        .placeholder(R.drawable.ic_launcher_foreground)
                                        .error(R.drawable.ic_launcher_foreground)
                                        .centerCrop()
                                        .into(profileImage);
                            }
                        } catch (Exception e) {
                            // If Base64 decoding fails, try as URL
                            Log.e("MainActivity", "Error loading profile image: " + e.getMessage());
                            Glide.with(MainActivity.this)
                                    .load(imageData)
                                    .placeholder(R.drawable.ic_launcher_foreground)
                                    .error(R.drawable.ic_launcher_foreground)
                                    .centerCrop()
                                    .into(profileImage);
                        }
                    } else {
                        Log.d("MainActivity", "Profile image data is null or empty");
                    }
                } else {
                    Log.d("MainActivity", "Profile image URL node doesn't exist");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore for now
            }
        });
    }

    // --- Handle Toolbar Navigation/Menu Item Clicks ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle the navigation icon (hamburger) click
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        // Handle other menu items if they were added programmatically or via fragments
        return super.onOptionsItemSelected(item);
    }
    // -------------------------------------

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        // Refresh selected disease from preferences before making any decisions
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        selectedDisease = settings.getString(SELECTED_DISEASE, DiseaseSelectionActivity.DISEASE_DIABETIC_NEUROPATHY);
        Log.d("MainActivity", "Current selected disease: " + selectedDisease);
        
        if (itemId == R.id.navigation_home) {
            // Check which disease is selected and show the appropriate home fragment
            if (selectedDisease.equals(DiseaseSelectionActivity.DISEASE_ALS)) {
                Log.d("MainActivity", "Switching to ALS fragment");
                switchFragment(alsFragment);
            } else {
                Log.d("MainActivity", "Switching to Home fragment");
                switchFragment(homeFragment);
            }
            return true;
        } else if (itemId == R.id.navigation_nervebot) {
            // Switch to the chatbot fragment
            Log.d("MainActivity", "Switching to NerveBot fragment");
            switchFragment(chatbotFragment);
            return true;
        } else if (itemId == R.id.navigation_history) {
            switchFragment(historyFragment);
            return true;
        } else if (itemId == R.id.navigation_settings) {
            switchFragment(settingsFragment);
            return true;
        } else if (itemId == R.id.navigation_rewards) {
            switchFragment(rewardsFragment);
            return true;
        }
        
        return false;
    }
    
    private void switchFragment(Fragment fragment) {
        if (fragment != activeFragment) {
            Log.d("MainActivity", "Switching from " + activeFragment.getClass().getSimpleName() + 
                  " to " + fragment.getClass().getSimpleName());
            
            // Create a new transaction
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            
            // Hide all fragments first to prevent overlap issues
            transaction.hide(homeFragment);
            transaction.hide(alsFragment);
            transaction.hide(chatbotFragment); // Ajout du chatbotFragment à cacher
            transaction.hide(historyFragment);
            transaction.hide(settingsFragment);
            transaction.hide(rewardsFragment); // Masquer le fragment Récompenses au départ
            
            // Show only the requested fragment
            transaction.show(fragment);
            
            // Commit the transaction
            transaction.commit();
            
            // Update the active fragment reference
            activeFragment = fragment;
        }
    }
    
    /**
     * Refreshes the active fragment based on the current disease selection
     * This should be called after the disease selection changes
     */
    public void refreshActiveFragmentBasedOnDisease() {
        // Get the latest disease selection
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        selectedDisease = settings.getString(SELECTED_DISEASE, DiseaseSelectionActivity.DISEASE_DIABETIC_NEUROPATHY);
        Log.d("MainActivity", "Refreshing fragments based on disease: " + selectedDisease);
        
        // Force a complete refresh of all fragments
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // Hide all fragments first
        transaction.hide(homeFragment);
        transaction.hide(alsFragment);
        transaction.hide(chatbotFragment); // Ajout du chatbotFragment à cacher
        transaction.hide(historyFragment);
        transaction.hide(settingsFragment);
        transaction.hide(rewardsFragment); // Masquer le fragment Récompenses au départ
        
        // Determine which fragment should be active now
        if (activeFragment == settingsFragment) {
            // If we're in settings, stay in settings
            transaction.show(settingsFragment);
            activeFragment = settingsFragment;
        } else if (activeFragment == historyFragment) {
            // If we're in history, stay in history
            transaction.show(historyFragment);
            activeFragment = historyFragment;
        } else if (activeFragment == chatbotFragment) {
            // If we're in chatbot, stay in chatbot
            transaction.show(chatbotFragment);
            activeFragment = chatbotFragment;
        } else {
            // For home/ALS fragments, show the correct one based on disease selection
            if (selectedDisease.equals(DiseaseSelectionActivity.DISEASE_ALS)) {
                Log.d("MainActivity", "Switching to ALS fragment after refresh");
                transaction.show(alsFragment);
                activeFragment = alsFragment;
            } else {
                Log.d("MainActivity", "Switching to Home fragment after refresh");
                transaction.show(homeFragment);
                activeFragment = homeFragment;
            }
        }
        
        // Apply the changes
        transaction.commit();
        
        // Update the bottom navigation to reflect the current fragment
        if (activeFragment == homeFragment || activeFragment == alsFragment) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        } else if (activeFragment == chatbotFragment) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_nervebot);
        } else if (activeFragment == historyFragment) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_history);
        } else if (activeFragment == settingsFragment) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_settings);
        } else if (activeFragment == rewardsFragment) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_rewards);
        }
    }
    
    /**
     * Applies the saved language settings from SharedPreferences
     */
    private void applyLanguageSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences("DiaNerverotectPrefs", Context.MODE_PRIVATE);
        String languageCode = sharedPreferences.getString("language", "en");
        
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Configuration config = new Configuration();
        config.locale = locale;
        
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    /**
     * Test method to verify Bluetooth notifications
     * Accessible for use in the settings fragment
     */
    public void testBluetoothNotifications() {
        // Simple dialog to ask what to simulate
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Test Bluetooth Notifications");
        String[] options = {
                "Simulate Connection",
                "Simulate Disconnection",
                "Simulate Low Battery (15%)",
                "Simulate Sync Issue",
                "Simulate App Update Available"
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Connection
                    // Simulate connection
                    Toast.makeText(this, "Simulating glove connection...", Toast.LENGTH_SHORT).show();
                    simulateConnection(true);
                    break;
                    
                case 1: // Disconnection
                    // Simulate disconnection
                    Toast.makeText(this, "Simulating glove disconnection...", Toast.LENGTH_SHORT).show();
                    simulateConnection(false);
                    break;
                    
                case 2: // Low Battery
                    // Simulate low battery
                    Toast.makeText(this, "Simulating low battery (15%)...", Toast.LENGTH_SHORT).show();
                    bluetoothManager.handleBatteryLevelUpdate(15);
                    break;
                    
                case 3: // Sync Issue
                    // Simulate sync issue
                    Toast.makeText(this, "Simulating Bluetooth sync issue...", Toast.LENGTH_SHORT).show();
                    bluetoothManager.processGloveData("SYNC:ERROR", null);
                    break;
                    
                case 4: // App Update
                    // Simulate app update available
                    Toast.makeText(this, "Simulating app update notification...", Toast.LENGTH_SHORT).show();
                    NotificationHelper notificationHelper = new NotificationHelper(this);
                    notificationHelper.showAppUpdateNotification("2.0.0");
                    break;
            }
        });
        
        builder.setNegativeButton("Close", null);
        builder.show();
    }
    
    /**
     * Helper method to simulate connection/disconnection for testing
     */
    public void simulateConnection(boolean isConnected) {
        // Create notification directly since handleConnectionStateChange is private
        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.showGloveConnectionNotification(isConnected);
    }
}
