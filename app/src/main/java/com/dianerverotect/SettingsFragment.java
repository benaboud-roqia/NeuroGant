package com.dianerverotect;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
// Using fully qualified name for Android's BluetoothManager to avoid conflict with our custom class
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.dianerverotect.billing.BillingManager;
import com.dianerverotect.billing.DemoPaymentActivity;
import com.android.billingclient.api.ProductDetails;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.dianerverotect.alerts.AlertsSettingsFragment;
import com.dianerverotect.alerts.NotificationHelper;

public class SettingsFragment extends Fragment implements com.dianerverotect.BluetoothManager.ConnectionListener, BillingManager.BillingListener {

    private static final String TAG = "SettingsFragment";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;
    private static final String TARGET_DEVICE_NAME = "DiaNerve";
    
    // Disease selection constants
    private static final String DISEASE_PREFS_NAME = "DiseasePrefs";
    private static final String SELECTED_DISEASE = "selected_disease";

    private TextView currentLanguageText;
    private ConstraintLayout languageLayout;
    private ConstraintLayout aboutUsLayout;
    private ConstraintLayout logoutLayout;
    private ConstraintLayout alertsSettingsLayout;
    private ConstraintLayout testNotificationsLayout;
    private ConstraintLayout diseaseSelectionLayout;
    private TextView currentDiseaseText;
    private Button changeDiseaseButton;
    private SwitchMaterial typingEffectSwitch;
    private SharedPreferences sharedPreferences;
    private SharedPreferences diseasePreferences;
    private FirebaseAuth mAuth;
    private static final String PREF_NAME = "DiaNerverotectPrefs";
    private static final String PREF_LANGUAGE = "language";
    
    // Premium features
    private ConstraintLayout premiumLayout;
    private TextView premiumStatusText;
    private Button upgradeButton;
    private PremiumManager premiumManager;
    private BillingManager billingManager;
    private ProductDetails premiumProductDetails;
    private AlertDialog premiumDialog;
    private static final int REQUEST_BARIDIMOB_PAYMENT = 1001;
    
    // Bluetooth related variables
    private TextView deviceStatusText;
    private Button scanButton;
    private Button batteryStatusButton;
    private Button sensorReadingsButton;
    private ImageView bluetoothIcon;
    private BluetoothAdapter bluetoothAdapter;
    private com.dianerverotect.BluetoothManager bluetoothManager;
    private List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private ArrayAdapter<String> deviceListAdapter;
    private AlertDialog scanDialog;
    
    // BroadcastReceiver for Bluetooth device discovery
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Device found
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    String deviceName = device.getName();
                    
                    // Check if this is our target device
                    if (deviceName != null && deviceName.equals(TARGET_DEVICE_NAME)) {
                        // Found our target device
                        Toast.makeText(requireContext(), R.string.device_found, Toast.LENGTH_SHORT).show();
                        
                        // Stop discovery before connecting
                        bluetoothAdapter.cancelDiscovery();
                        
                        // Connect to the device
                        connectToDevice(device);
                        
                        // Dismiss the dialog
                        if (scanDialog != null && scanDialog.isShowing()) {
                            scanDialog.dismiss();
                        }
                        
                        return;
                    }
                    
                    // Add to the list of devices if not already there
                    if (!discoveredDevices.contains(device)) {
                        discoveredDevices.add(device);
                        updateDeviceList();
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Discovery finished
                if (scanDialog != null && scanDialog.isShowing()) {
                    if (discoveredDevices.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.no_devices_found, Toast.LENGTH_SHORT).show();
                        scanDialog.dismiss();
                    } else {
                        // Update dialog title
                        scanDialog.setTitle(R.string.select_device);
                    }
                }
            }
        }
    };
    
    // Flag to control visibility of test options
    private static final boolean SHOW_TEST_OPTIONS = true;

    private static final int REQUEST_GOOGLEPLAY_TEST = 1002;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        diseasePreferences = requireActivity().getSharedPreferences(DISEASE_PREFS_NAME, Context.MODE_PRIVATE);
        
        // Initialize Premium Manager
        premiumManager = PremiumManager.getInstance(requireContext());
        
        // Initialize Billing Manager
        billingManager = new BillingManager(requireContext(), this);
        
        // Initialize Bluetooth
        initializeBluetooth();
        
        // Initialize views
        currentLanguageText = view.findViewById(R.id.current_language_text);
        languageLayout = view.findViewById(R.id.language_layout);
        logoutLayout = view.findViewById(R.id.logout_layout);
        aboutUsLayout = view.findViewById(R.id.about_us_layout);
        alertsSettingsLayout = view.findViewById(R.id.alerts_settings_layout);
        testNotificationsLayout = view.findViewById(R.id.test_notifications_layout);
        diseaseSelectionLayout = view.findViewById(R.id.disease_selection_layout);
        currentDiseaseText = view.findViewById(R.id.current_disease_text);
        changeDiseaseButton = view.findViewById(R.id.change_disease_button);
        typingEffectSwitch = view.findViewById(R.id.switch_typing_effect);
        
        // Initialize Premium views
        premiumLayout = view.findViewById(R.id.premium_layout);
        premiumStatusText = view.findViewById(R.id.premium_status);
        upgradeButton = view.findViewById(R.id.upgrade_button);
        
        // Initialize Bluetooth views
        deviceStatusText = view.findViewById(R.id.text_device_subtitle);
        scanButton = view.findViewById(R.id.button_scan_devices);
        batteryStatusButton = view.findViewById(R.id.button_battery_status);
        sensorReadingsButton = view.findViewById(R.id.button_sensor_readings);
        bluetoothIcon = view.findViewById(R.id.icon_bluetooth);
        
        // Hide test options if not enabled
        if (!SHOW_TEST_OPTIONS && testNotificationsLayout != null) {
            testNotificationsLayout.setVisibility(View.GONE);
        }
        
        // Set up Premium features
        setupPremiumFeatures(view);
        
        // Set up Bluetooth scanning
        setupBluetoothScanning();
        
        // Set up battery status button
        setupBatteryStatusButton();
        
        // Set up sensor readings button
        setupSensorReadingsButton();
        
        // Set up language selection
        setupLanguageSelection();
        
        // Set up disease selection
        setupDiseaseSelection();
        
        // Set up about us dialog
        setupAboutUsDialog();
        
        // Set up logout
        setupLogout();
        
        // Set up alerts settings
        setupAlertsSettings();
        
        // Set up typing effect switch
        setupTypingEffectSwitch();
        
        // Update current language display
        updateCurrentLanguageText();
        
        // Update current disease text
        updateCurrentDiseaseText();
        
        // Update premium status
        updatePremiumStatus();
        
        // Set up test notifications if enabled
        if (SHOW_TEST_OPTIONS && testNotificationsLayout != null) {
            setupTestNotifications();
        }
        
        // Ajout du bouton FAQ
        Button faqButton = view.findViewById(R.id.button_faq);
        faqButton.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
            View dialogView = inflater.inflate(R.layout.dialog_faq, null);

            TextView title = dialogView.findViewById(R.id.faq_dialog_title);
            TextView content = dialogView.findViewById(R.id.faq_dialog_content);
            Button closeBtn = dialogView.findViewById(R.id.faq_dialog_close);

            String faqText = "1. Comment activer le Bluetooth ?\n"
                    + "Pour activer le Bluetooth, cliquez sur le bouton \"Scan Device\" sur l'écran principal.\n\n"
                    + "2. Que dois-je faire en ouvrant l'application ?\n"
                    + "Après avoir ouvert l'application, cliquez d'abord sur \"Scan Device\" pour activer le Bluetooth et détecter votre appareil. Ensuite, ouvrez ou créez votre compte.\n\n"
                    + "3. Comment créer un compte ?\n"
                    + "Cliquez sur \"Créer un compte\" sur l'écran de connexion et remplissez les informations demandées.\n\n"
                    + "4. Comment exporter mes résultats ?\n"
                    + "Allez dans la section Historique, sélectionnez un test, puis cliquez sur \"Exporter\".\n\n"
                    + "5. Que faire si l'application ne détecte pas mon appareil ?\n"
                    + "- Vérifiez que le Bluetooth est activé\n"
                    + "- Que l'appareil est allumé et proche\n"
                    + "- Cliquez à nouveau sur \"Scan Device\"\n"
                    + "- Redémarrez l'application ou l'appareil si besoin\n\n"
                    + "6. Comment contacter le support ?\n"
                    + "Par email : benaboud.roqia@univ-oeb.dz ou zouaoui.sirine@univ-oeb.dz\n\n"
                    + "Version de l'application : 1.0.0";
            content.setText(faqText);

            builder.setView(dialogView);
            android.app.AlertDialog dialog = builder.create();
            closeBtn.setOnClickListener(v2 -> dialog.dismiss());
            dialog.show();
        });
        
        return view;
    }
    
    private void setupLanguageSelection() {
        languageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageSelectionDialog();
            }
        });
    }
    
    private void setupPremiumFeatures(View view) {
        if (premiumLayout != null) {
            premiumLayout.setOnClickListener(v -> showPremiumDialog());
        }
        
        if (upgradeButton != null) {
            upgradeButton.setOnClickListener(v -> showPremiumDialog());
        }
        
        // Ajouter le bouton de test direct
        Button testPremiumButton = view.findViewById(R.id.test_premium_button);
        if (testPremiumButton != null) {
            testPremiumButton.setOnClickListener(v -> startGooglePlayTest());
        }
        
        // Query available products
        billingManager.queryAvailableProducts();
    }
    
    private static final int DISEASE_SELECTION_REQUEST_CODE = 1001;
    
    private void setupDiseaseSelection() {
        try {
            Log.d(TAG, "Setting up disease selection UI");
            
            // Check if UI elements are properly initialized
            if (diseaseSelectionLayout == null) {
                Log.e(TAG, "Disease selection layout is null!");
                return;
            }
            
            if (currentDiseaseText == null) {
                Log.e(TAG, "Current disease text is null!");
                return;
            }
            
            if (changeDiseaseButton == null) {
                Log.e(TAG, "Change disease button is null!");
                return;
            }
            
            // Get the currently selected disease
            String selectedDisease = diseasePreferences.getString(SELECTED_DISEASE, DiseaseSelectionActivity.DISEASE_DIABETIC_NEUROPATHY);
            Log.d(TAG, "Current selected disease: " + selectedDisease);
            
            // Update the current disease text
            if (selectedDisease.equals(DiseaseSelectionActivity.DISEASE_ALS)) {
                currentDiseaseText.setText(getString(R.string.als));
            } else {
                currentDiseaseText.setText(getString(R.string.diabetic_neuropathy));
            }
            
            // Set up the change disease button to launch the dedicated activity
            View.OnClickListener diseaseSelectionClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Disease selection requested");
                    Toast.makeText(requireContext(), "Opening disease selection...", Toast.LENGTH_SHORT).show();
                    launchDiseaseSelectionActivity();
                }
            };
            
            // Apply the click listener to both the button and the layout
            changeDiseaseButton.setOnClickListener(diseaseSelectionClickListener);
            diseaseSelectionLayout.setOnClickListener(diseaseSelectionClickListener);
            
            // Ensure the layout is visible and clickable
            diseaseSelectionLayout.setVisibility(View.VISIBLE);
            diseaseSelectionLayout.setClickable(true);
            diseaseSelectionLayout.setFocusable(true);
            
            Log.d(TAG, "Disease selection UI setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up disease selection: " + e.getMessage());
            Toast.makeText(requireContext(), "Error setting up disease selection: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void launchDiseaseSelectionActivity() {
        try {
            Intent intent = new Intent(requireActivity(), DiseaseSelectionSettingsActivity.class);
            startActivityForResult(intent, DISEASE_SELECTION_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Error launching disease selection activity: " + e.getMessage());
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // Disease selection functionality has been moved to the main onActivityResult method
    
    private void showDiseaseSelectionDialog() {
        try {
            Log.d(TAG, "Opening disease selection dialog");
            final String[] diseases = {getString(R.string.diabetic_neuropathy), getString(R.string.als)};
            final String[] diseaseCodes = {DiseaseSelectionActivity.DISEASE_DIABETIC_NEUROPATHY, DiseaseSelectionActivity.DISEASE_ALS};
            
            // Get current disease
            String currentDiseaseCode = diseasePreferences.getString(SELECTED_DISEASE, DiseaseSelectionActivity.DISEASE_DIABETIC_NEUROPATHY);
            Log.d(TAG, "Current disease code: " + currentDiseaseCode);
            
            int selectedIndex = 0;
            for (int i = 0; i < diseaseCodes.length; i++) {
                if (diseaseCodes[i].equals(currentDiseaseCode)) {
                    selectedIndex = i;
                    break;
                }
            }
            
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(getString(R.string.change_disease_title));
            builder.setMessage(getString(R.string.change_disease_message));
            builder.setSingleChoiceItems(diseases, selectedIndex, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        // Save selected disease
                        String selectedDiseaseCode = diseaseCodes[which];
                        Log.d(TAG, "Selected disease: " + diseases[which] + " (" + selectedDiseaseCode + ")");
                        
                        SharedPreferences.Editor editor = diseasePreferences.edit();
                        editor.putString(SELECTED_DISEASE, selectedDiseaseCode);
                        editor.apply();
                        
                        // Update the current disease text
                        currentDiseaseText.setText(diseases[which]);
                        
                        // Show a toast message
                        Toast.makeText(requireContext(), diseases[which] + " " + getString(R.string.selected), Toast.LENGTH_SHORT).show();
                        
                        // Close the dialog
                        dialog.dismiss();
                        
                        // Restart the activity to apply the change
                        Log.d(TAG, "Restarting activity to apply disease change");
                        requireActivity().recreate();
                    } catch (Exception e) {
                        Log.e(TAG, "Error in disease selection dialog onClick: " + e.getMessage());
                        Toast.makeText(requireContext(), "Error selecting disease: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Dialog will be dismissed automatically
                    Log.d(TAG, "Disease selection dialog dismissed");
                }
            });
            
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing disease selection dialog: " + e.getMessage());
            Toast.makeText(requireContext(), "Error showing disease selection dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupAboutUsDialog() {
        aboutUsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutUsDialog();
            }
        });
    }
    
    private void showAboutUsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        
        // Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_about_us, null);
        
        builder.setView(dialogView);
        builder.setPositiveButton(getString(R.string.button_close), null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showLanguageSelectionDialog() {
        final String[] languages = {getString(R.string.language_english), getString(R.string.language_french), getString(R.string.language_spanish)};
        final String[] languageCodes = {"en", "fr", "es"};
        
        // Get current language
        String currentLangCode = sharedPreferences.getString(PREF_LANGUAGE, "en");
        int selectedIndex = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLangCode)) {
                selectedIndex = i;
                break;
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.language));
        builder.setSingleChoiceItems(languages, selectedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Save selected language
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PREF_LANGUAGE, languageCodes[which]);
                editor.apply();
                
                // Change app language
                setLocale(languageCodes[which]);
                
                dialog.dismiss();
                
                // Restart activity to apply language change
                Intent intent = requireActivity().getIntent();
                requireActivity().finish();
                startActivity(intent);
            }
        });
        
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }
    
    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Configuration config = new Configuration();
        config.locale = locale;
        
        requireActivity().getResources().updateConfiguration(
                config, 
                requireActivity().getResources().getDisplayMetrics()
        );
    }
    
    private void setupTestNotifications() {
        if (testNotificationsLayout != null) {
            testNotificationsLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Test Bluetooth notifications
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).testBluetoothNotifications();
                        Toast.makeText(requireContext(), getString(R.string.test_notification_sent), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    
    private void updateCurrentLanguageText() {
        String langCode = sharedPreferences.getString(PREF_LANGUAGE, "en");
        switch (langCode) {
            case "fr":
                currentLanguageText.setText(R.string.language_french);
                break;
            case "es":
                currentLanguageText.setText(R.string.language_spanish);
                break;
            default:
                currentLanguageText.setText(R.string.language_english);
                break;
        }
    }
    
    /**
     * Met à jour l'affichage de la maladie sélectionnée
     */
    private void updateCurrentDiseaseText() {
        String selectedDisease = diseasePreferences.getString(SELECTED_DISEASE, "diabetic_neuropathy");
        String diseaseName;
        
        switch (selectedDisease) {
            case "als":
                diseaseName = getString(R.string.als);
                break;
            case "diabetic_neuropathy":
            default:
                diseaseName = getString(R.string.diabetic_neuropathy);
                break;
        }
        
        currentDiseaseText.setText(diseaseName);
    }
    
    private void setupLogout() {
        logoutLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmationDialog();
            }
        });
    }
    
    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.logout_confirmation_title));
        builder.setMessage(getString(R.string.logout_confirmation_message));
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                performLogout();
            }
        });
        builder.setNegativeButton(getString(R.string.no), null);
        builder.show();
    }
    
    private void performLogout() {
        try {
            // Sign out from Firebase
            mAuth.signOut();
            
            // Clear any user-specific preferences if needed
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            sharedPreferences.edit().clear().apply();
            
            // Show logout success message
            Toast.makeText(requireContext(), getString(R.string.logout_success), Toast.LENGTH_SHORT).show();
            
            // Create a new task for the LoginActivity but don't clear existing tasks
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            
            // Finish all activities in the current task
            if (getActivity() != null) {
                getActivity().finishAffinity();
            }
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error during logout: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Logout failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupAlertsSettings() {
        alertsSettingsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to alerts settings fragment
                AlertsSettingsFragment alertsSettingsFragment = new AlertsSettingsFragment();
                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                
                // Add animation
                transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                );
                
                // Replace and add to back stack
                transaction.replace(R.id.fragment_container_view, alertsSettingsFragment)
                          .addToBackStack(null)
                          .commit();
            }
        });
    }
    
    /**
     * Show dialog with notification test options
     */
    private void showTestNotificationsDialog() {
        // Only show in development builds
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Test Notifications");
        
        String[] options = {
                "Glove Connected",
                "Glove Disconnected",
                "Low Battery (15%)",
                "Sync Issue",
                "App Update Available"
        };
        
        builder.setItems(options, (dialog, which) -> {
            // Use the existing NotificationHelper instance
            NotificationHelper notificationHelper = new NotificationHelper(requireContext());
            
            switch (which) {
                case 0: // Connection
                    notificationHelper.showGloveConnectionNotification(true);
                    Toast.makeText(requireContext(), "Testing connected notification", Toast.LENGTH_SHORT).show();
                    break;
                    
                case 1: // Disconnection
                    notificationHelper.showGloveConnectionNotification(false);
                    Toast.makeText(requireContext(), "Testing disconnected notification", Toast.LENGTH_SHORT).show();
                    break;
                    
                case 2: // Low Battery
                    notificationHelper.showLowBatteryNotification(15);
                    Toast.makeText(requireContext(), "Testing low battery notification", Toast.LENGTH_SHORT).show();
                    break;
                    
                case 3: // Sync Issue
                    notificationHelper.showSyncIssueNotification("Bluetooth");
                    Toast.makeText(requireContext(), "Testing sync issue notification", Toast.LENGTH_SHORT).show();
                    break;
                    
                case 4: // App Update
                    notificationHelper.showAppUpdateNotification("2.0.0");
                    Toast.makeText(requireContext(), "Testing app update notification", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
        
        builder.setNegativeButton("Close", null);
        builder.show();
    }
    
    private void initializeBluetooth() {
        try {
            // First check if Bluetooth is supported by the device hardware
            if (!requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                Log.w(TAG, "Bluetooth is not supported by device hardware");
                // Don't show toast here - we'll show it when user tries to use Bluetooth
                return;
            }
            
            // Try multiple methods to get a valid BluetoothAdapter (for Samsung compatibility)
            // Method 1: Get the BluetoothAdapter from Android's built-in BluetoothManager
            android.bluetooth.BluetoothManager systemBluetoothManager = 
                (android.bluetooth.BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            if (systemBluetoothManager != null) {
                bluetoothAdapter = systemBluetoothManager.getAdapter();
            }
            
            // Method 2: If adapter is still null, try the direct method (works better on some Samsung devices)
            if (bluetoothAdapter == null) {
                Log.d(TAG, "Trying fallback method for Samsung devices");
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            
            if (bluetoothAdapter != null) {
                Log.d(TAG, "Successfully initialized Bluetooth adapter");
            } else {
                Log.e(TAG, "Failed to initialize Bluetooth adapter despite hardware support");
            }
            
            // Initialize our custom BluetoothManager
            this.bluetoothManager = new com.dianerverotect.BluetoothManager(requireContext());
            this.bluetoothManager.setConnectionListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Error in initializeBluetooth: " + e.getMessage(), e);
        }
    }
    
    private void setupBluetoothScanning() {
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // First check if Bluetooth is supported by the device hardware
                if (!requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    Toast.makeText(requireContext(), R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Try to initialize Bluetooth adapter if it's null
                if (bluetoothAdapter == null) {
                    try {
                        // Samsung-specific fallback method
                        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (bluetoothAdapter == null) {
                            Toast.makeText(requireContext(), R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error initializing Bluetooth adapter: " + e.getMessage());
                        Toast.makeText(requireContext(), R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                
                // Check if Bluetooth is enabled
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        requestBluetoothPermissions();
                        return;
                    }
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    // Bluetooth is enabled, start discovery
                    startDeviceDiscovery();
                }
            }
        });
    }
    
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (API 31+)
            ActivityCompat.requestPermissions(
                requireActivity(),
                new String[] {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                },
                REQUEST_BLUETOOTH_PERMISSIONS
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6-11 (API 23-30)
            ActivityCompat.requestPermissions(
                requireActivity(),
                new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION // Required for BLE scanning on Android 6-11
                },
                REQUEST_BLUETOOTH_PERMISSIONS
            );
        }
        // For Android < 6 (API < 23), permissions are granted at install time
    }
    
    // The startDeviceDiscovery method is implemented below
    
    /**
     * Show dialog for scanning and selecting devices with a modern design
     * Will automatically connect to DianervProtect device when found
     */
    private void showScanningDialog() {
        try {
            // Create a dialog using our custom layout
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_scan_devices, null);
            
            // Find views in the dialog layout
            TextView scanStatusText = dialogView.findViewById(R.id.text_scan_status);
            ProgressBar scanProgress = dialogView.findViewById(R.id.scan_progress);
            RecyclerView deviceListView = dialogView.findViewById(R.id.device_list);
            TextView noDevicesText = dialogView.findViewById(R.id.text_no_devices);
            Button cancelButton = dialogView.findViewById(R.id.button_cancel);
            Button rescanButton = dialogView.findViewById(R.id.button_rescan);
            
            // Set up the RecyclerView
            deviceListView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
            DeviceAdapter deviceAdapter = new DeviceAdapter();
            deviceListView.setAdapter(deviceAdapter);
            
            // Create and show the dialog
            AlertDialog dialog = builder.setView(dialogView).create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            dialog.show();
            
            // Store reference to the dialog
            scanDialog = dialog;
            
            // Set up the cancel button
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ActivityCompat.checkSelfPermission(requireContext(), 
                                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                                requestBluetoothPermissions();
                                return;
                            }
                            bluetoothAdapter.cancelDiscovery();
                        } else {
                            // For Android 11 and below, no runtime permission check needed for cancelDiscovery
                            bluetoothAdapter.cancelDiscovery();
                        }
                        dialog.dismiss();
                    } catch (SecurityException e) {
                        Log.e(TAG, "SecurityException when canceling discovery: " + e.getMessage());
                    }
                }
            });
            
            // Set up the rescan button
            rescanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Clear the device list
                    discoveredDevices.clear();
                    deviceAdapter.notifyDataSetChanged();
                    
                    // Update UI
                    scanStatusText.setText("Looking for DianervProtect...");
                    noDevicesText.setVisibility(View.GONE);
                    scanProgress.setVisibility(View.VISIBLE);
                    
                    try {
                        // Version-specific permission check
                        boolean hasPermission = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (API 31+)
                            hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
                            if (!hasPermission) {
                                Log.d(TAG, "BLUETOOTH_SCAN permission not granted for Android 12+");
                                requestBluetoothPermissions();
                                return;
                            }
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6-11 (API 23-30)
                            hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(requireContext(), 
                                    Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
                            if (!hasPermission) {
                                Log.d(TAG, "BLUETOOTH and/or LOCATION permission not granted for Android 6-11");
                                requestBluetoothPermissions();
                                return;
                            }
                        }
                        
                        // Cancel any ongoing discovery
                        if (bluetoothAdapter.isDiscovering()) {
                            bluetoothAdapter.cancelDiscovery();
                        }
                        
                        // Start discovery
                        boolean discoveryStarted = bluetoothAdapter.startDiscovery();
                        if (!discoveryStarted) {
                            Toast.makeText(requireContext(), "Failed to start Bluetooth discovery", Toast.LENGTH_SHORT).show();
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "SecurityException when restarting Bluetooth discovery: " + e.getMessage());
                        Toast.makeText(requireContext(), "Bluetooth permission error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        requestBluetoothPermissions();
                    }
                }
            });
            
            // Create a BroadcastReceiver for ACTION_FOUND
            BroadcastReceiver receiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        try {
                            // Discovery has found a device
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            if (device != null) {
                                // Version-specific permission check
                                boolean hasPermission = false;
                                String deviceName = null;
                                String deviceAddress = null;
                                
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (API 31+)
                                    if (ActivityCompat.checkSelfPermission(requireContext(), 
                                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                        // Don't request permissions here as it would interrupt the broadcast receiver
                                        Log.d(TAG, "BLUETOOTH_CONNECT permission not granted for Android 12+");
                                        return;
                                    }
                                    deviceName = device.getName();
                                    deviceAddress = device.getAddress();
                                } else { // Android 11 and below
                                    deviceName = device.getName();
                                    deviceAddress = device.getAddress();
                                }
                                
                                // Check if it's our target device
                                if (deviceName != null && deviceName.equals(TARGET_DEVICE_NAME)) {
                                    // Found our target device!
                                    scanStatusText.setText("Found DianervProtect! Connecting...");
                                    
                                    try {
                                        // Stop discovery with appropriate permission check
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            if (ActivityCompat.checkSelfPermission(requireContext(), 
                                                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                                                bluetoothAdapter.cancelDiscovery();
                                            }
                                        } else {
                                            bluetoothAdapter.cancelDiscovery();
                                        }
                                    } catch (SecurityException e) {
                                        Log.e(TAG, "SecurityException when canceling discovery: " + e.getMessage());
                                    }
                                    
                                    // Connect to the device
                                    connectToDevice(device);
                                    
                                    // Dismiss the dialog after a short delay
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (dialog.isShowing()) {
                                                dialog.dismiss();
                                            }
                                        }
                                    }, 1500);
                                    
                                    return;
                                }
                                
                                // Add to the list of devices if not already there
                                if (!discoveredDevices.contains(device)) {
                                    discoveredDevices.add(device);
                                    deviceAdapter.notifyDataSetChanged();
                                    
                                    // Show the list and hide the "no devices" text
                                    noDevicesText.setVisibility(View.GONE);
                                }
                            }
                        } catch (SecurityException e) {
                            Log.e(TAG, "SecurityException in BroadcastReceiver: " + e.getMessage());
                        } catch (Exception e) {
                            Log.e(TAG, "Exception in BroadcastReceiver: " + e.getMessage());
                        }
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        // Discovery finished
                        scanProgress.setVisibility(View.GONE);
                        scanStatusText.setText("Scan complete");
                        
                        // Show "no devices" text if no devices were found
                        if (discoveredDevices.isEmpty()) {
                            noDevicesText.setVisibility(View.VISIBLE);
                        }
                    }
                }
            };
            
            // Register for broadcasts
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            requireActivity().registerReceiver(receiver, filter);
            
            // Start discovery with version-specific permission checks
            try {
                boolean hasPermission = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (API 31+)
                    hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                            Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
                    if (!hasPermission) {
                        Log.d(TAG, "BLUETOOTH_SCAN permission not granted for Android 12+");
                        requestBluetoothPermissions();
                        return;
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6-11 (API 23-30)
                    hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(requireContext(), 
                            Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
                    if (!hasPermission) {
                        Log.d(TAG, "BLUETOOTH and/or LOCATION permission not granted for Android 6-11");
                        requestBluetoothPermissions();
                        return;
                    }
                } else { // Below Android 6 (API < 23)
                    hasPermission = true; // Permissions granted at install time
                }
                
                // Cancel any ongoing discovery
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                
                // Start discovery
                boolean discoveryStarted = bluetoothAdapter.startDiscovery();
                if (!discoveryStarted) {
                    Log.e(TAG, "Failed to start Bluetooth discovery");
                    Toast.makeText(requireContext(), "Failed to start Bluetooth discovery", Toast.LENGTH_SHORT).show();
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when starting Bluetooth discovery: " + e.getMessage());
                Toast.makeText(requireContext(), "Bluetooth permission error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                requestBluetoothPermissions();
            }
            
            // Make sure to unregister the receiver when the dialog is dismissed
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    try {
                        requireActivity().unregisterReceiver(receiver);
                    } catch (Exception e) {
                        Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing scanning dialog: " + e.getMessage());
            Toast.makeText(requireContext(), "Error showing scanning dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Adapter for the device list in the scanning dialog
     */
    private class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
        
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView deviceIcon;
            TextView deviceName;
            TextView deviceAddress;
            Button connectButton;
            
            ViewHolder(View itemView) {
                super(itemView);
                deviceIcon = itemView.findViewById(R.id.device_icon);
                deviceName = itemView.findViewById(R.id.device_name);
                deviceAddress = itemView.findViewById(R.id.device_address);
                connectButton = itemView.findViewById(R.id.device_connect_button);
            }
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bluetooth_device, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BluetoothDevice device = discoveredDevices.get(position);
            
            try {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                
                // Set device name and address
                String deviceName = device.getName();
                if (deviceName == null || deviceName.isEmpty()) {
                    deviceName = device.getAddress();
                }
                holder.deviceName.setText(deviceName);
                holder.deviceAddress.setText(device.getAddress());
                
                // Highlight the target device
                if (deviceName.equals(TARGET_DEVICE_NAME)) {
                    holder.deviceIcon.setImageResource(android.R.drawable.stat_sys_data_bluetooth);
                    holder.deviceIcon.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_light)));
                    holder.deviceIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
                    holder.deviceName.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    holder.connectButton.setText("Connect");
                } else {
                    holder.deviceIcon.setImageResource(android.R.drawable.stat_sys_data_bluetooth);
                    holder.deviceIcon.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.darker_gray)));
                    holder.deviceIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.white)));
                    holder.deviceName.setTextColor(getResources().getColor(android.R.color.black));
                    holder.connectButton.setText("Other");
                }
                
                // Set up connect button
                holder.connectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Stop discovery before connecting
                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            requestBluetoothPermissions();
                            return;
                        }
                        bluetoothAdapter.cancelDiscovery();
                        
                        // Connect to the device
                        connectToDevice(device);
                        
                        // Dismiss the dialog
                        if (scanDialog != null && scanDialog.isShowing()) {
                            scanDialog.dismiss();
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error binding device: " + e.getMessage());
            }
        }
        
        @Override
        public int getItemCount() {
            return discoveredDevices.size();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                // Permissions granted, proceed with Bluetooth operations
                if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                    try {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ActivityCompat.checkSelfPermission(requireContext(), 
                                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                            } else {
                                Log.e(TAG, "BLUETOOTH_CONNECT permission still not granted");
                                Toast.makeText(requireContext(), "Bluetooth connect permission is required", 
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "SecurityException when enabling Bluetooth: " + e.getMessage());
                        Toast.makeText(requireContext(), "Bluetooth permission error: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    startDeviceDiscovery();
                }
            } else {
                // Show a more helpful message about which permissions are needed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Toast.makeText(requireContext(), 
                            "BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions are required", 
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), 
                            "BLUETOOTH and LOCATION permissions are required for device connection", 
                            Toast.LENGTH_LONG).show();
                }
                
                // Show a dialog explaining why permissions are needed
                new AlertDialog.Builder(requireContext())
                        .setTitle("Bluetooth Permissions Required")
                        .setMessage("DiaNerve Protect needs Bluetooth permissions to connect to your device. " +
                                "Please grant these permissions to use the app's features.")
                        .setPositiveButton("Request Again", (dialog, which) -> requestBluetoothPermissions())
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, start scanning
                startDeviceDiscovery();
            } else {
                // User declined to enable Bluetooth
                Toast.makeText(requireContext(), R.string.bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
            }
            return;
        } else if (requestCode == DISEASE_SELECTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Disease selection activity returned with OK result");
                
                // Refresh the disease text display
                String selectedDisease = diseasePreferences.getString(SELECTED_DISEASE, DiseaseSelectionActivity.DISEASE_DIABETIC_NEUROPATHY);
                if (selectedDisease.equals(DiseaseSelectionActivity.DISEASE_ALS)) {
                    currentDiseaseText.setText(getString(R.string.als));
                } else {
                    currentDiseaseText.setText(getString(R.string.diabetic_neuropathy));
                }
                
                // Restart the main activity to apply changes
                if (data != null && data.getBooleanExtra("disease_changed", false)) {
                    Log.d(TAG, "Disease was changed, restarting activity");
                    requireActivity().recreate();
                }
            } else {
                Log.d(TAG, "Disease selection activity was cancelled");
            }
            return;
        } else if (requestCode == REQUEST_GOOGLEPLAY_TEST) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(requireContext(), "Test Google Play Billing réussi !", Toast.LENGTH_LONG).show();
                updatePremiumStatus();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(requireContext(), "Test Google Play annulé ou échoué", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    private void startDeviceDiscovery() {
        // Clear previously discovered devices
        discoveredDevices.clear();
        
        // Check if Bluetooth is supported and initialized
        if (bluetoothAdapter == null) {
            // Try to initialize the adapter again (Samsung fallback)
            try {
                Log.d(TAG, "Bluetooth adapter is null, trying Samsung fallback method");
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                
                if (bluetoothAdapter == null) {
                    Log.e(TAG, "Failed to initialize Bluetooth adapter with fallback method");
                    Toast.makeText(requireContext(), R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Log.d(TAG, "Successfully initialized Bluetooth adapter with fallback method");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting Bluetooth adapter: " + e.getMessage(), e);
                Toast.makeText(requireContext(), R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        requireActivity().registerReceiver(discoveryReceiver, filter);
        
        // Show scanning dialog
        showScanningDialog();
        
        try {
            // Version-specific permission check
            boolean hasPermission = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                        Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
                if (!hasPermission) {
                    Log.d(TAG, "BLUETOOTH_SCAN permission not granted for Android 12+");
                    Toast.makeText(requireContext(), "Bluetooth scan permission is required", Toast.LENGTH_SHORT).show();
                    requestBluetoothPermissions();
                    return;
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6-11
                hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(requireContext(), 
                        Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
                if (!hasPermission) {
                    Log.d(TAG, "BLUETOOTH and/or LOCATION permission not granted for Android 6-11");
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH},
                            REQUEST_BLUETOOTH_PERMISSIONS);
                    return;
                }
            } else { // Below Android 6
                hasPermission = true; // Permissions granted at install time
            }
            
            // Check permission for cancelling discovery (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "BLUETOOTH_SCAN permission not granted for Android 12+ when cancelling discovery");
                    requestBluetoothPermissions();
                    return;
                }
            }
            
            // Cancel discovery if it's already running
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            
            // Start discovery with permission check
            boolean discoveryStarted;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "BLUETOOTH_SCAN permission not granted for Android 12+ when starting discovery");
                    requestBluetoothPermissions();
                    return;
                }
                discoveryStarted = bluetoothAdapter.startDiscovery();
            } else {
                discoveryStarted = bluetoothAdapter.startDiscovery();
            }
            
            if (discoveryStarted) {
                Log.d(TAG, "Successfully started Bluetooth discovery");
                Toast.makeText(requireContext(), R.string.scanning_for_devices, Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to start Bluetooth discovery");
                // Try one more time with the fallback method for Samsung devices
                try {
                    // Re-initialize adapter with fallback method
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter != null) {
                        // Check permission again before starting discovery with fallback
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                                Log.d(TAG, "BLUETOOTH_SCAN permission not granted for Android 12+ when starting fallback discovery");
                                requestBluetoothPermissions();
                                return;
                            }
                        }
                        discoveryStarted = bluetoothAdapter.startDiscovery();
                        if (discoveryStarted) {
                            Log.d(TAG, "Successfully started Bluetooth discovery with fallback");
                            Toast.makeText(requireContext(), R.string.scanning_for_devices, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Failed to start Bluetooth discovery even with fallback");
                            Toast.makeText(requireContext(), "Failed to start Bluetooth discovery", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in fallback discovery: " + e.getMessage(), e);
                    Toast.makeText(requireContext(), "Failed to start Bluetooth discovery", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when starting Bluetooth discovery: " + e.getMessage());
            Toast.makeText(requireContext(), "Bluetooth permission error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            requestBluetoothPermissions();
        } catch (Exception e) {
            Log.e(TAG, "Exception in startDeviceDiscovery: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error starting Bluetooth discovery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        
        // Also check paired devices
        checkPairedDevices();
    }
    
    /**
     * Check for already paired devices that match our target device
     */
    private void checkPairedDevices() {
        try {
            // Version-specific permission check
            boolean hasPermission = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (API 31+)
                hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
                if (!hasPermission) {
                    Log.d(TAG, "BLUETOOTH_CONNECT permission not granted for Android 12+");
                    requestBluetoothPermissions();
                    return;
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6-11 (API 23-30)
                hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                        Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
                if (!hasPermission) {
                    Log.d(TAG, "BLUETOOTH permission not granted for Android 6-11");
                    requestBluetoothPermissions();
                    return;
                }
            } else { // Below Android 6 (API < 23)
                hasPermission = true; // Permissions granted at install time
            }
            
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            Log.d(TAG, "Found " + pairedDevices.size() + " paired devices");
            
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    if (deviceName != null && deviceName.equals(TARGET_DEVICE_NAME)) {
                        // Found our target device in paired devices
                        connectToDevice(device);
                        return;
                    }
                    
                    // Add to the list of devices
                    if (!discoveredDevices.contains(device)) {
                        discoveredDevices.add(device);
                        updateDeviceList();
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when checking paired devices: " + e.getMessage());
            Toast.makeText(requireContext(), "Bluetooth permission error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            requestBluetoothPermissions();
        }
    }
    
    /**
     * Set up the battery status button to show the current battery level
     */
    private void setupBatteryStatusButton() {
        batteryStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // For testing purposes, always show the dialog even if bluetoothManager is null
                    // In a real app, you would check if bluetoothManager != null
                    
                    // Get the battery level (0-100)
                    int batteryLevel = 75; // Default value for testing
                    if (bluetoothManager != null) {
                        batteryLevel = bluetoothManager.getBatteryLevel();
                    }
                    
                    // Create a dialog using our custom layout
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    
                    // Create a custom view for the dialog
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_battery_status, null);
                    
                    // Find views in the dialog layout
                    TextView batteryLevelText = dialogView.findViewById(R.id.text_battery_level);
                    TextView batteryStatusText = dialogView.findViewById(R.id.text_battery_status);
                    ProgressBar batteryProgress = dialogView.findViewById(R.id.battery_progress);
                    Button closeButton = dialogView.findViewById(R.id.button_close);
                    
                    // Set the battery level text and progress
                    batteryLevelText.setText(batteryLevel + "%");
                    batteryProgress.setProgress(batteryLevel);
                    
                    // Set battery status text based on level
                    if (batteryLevel > 80) {
                        batteryStatusText.setText("Excellent - Full day of use remaining");
                        batteryStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    } else if (batteryLevel > 50) {
                        batteryStatusText.setText("Good - Several hours remaining");
                        batteryStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                    } else if (batteryLevel > 20) {
                        batteryStatusText.setText("Low - Consider charging soon");
                        batteryStatusText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                    } else {
                        batteryStatusText.setText("Critical - Charge immediately");
                        batteryStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    }
                    
                    // Create and show the dialog
                    AlertDialog dialog = builder.setView(dialogView).create();
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }
                    dialog.show();
                    
                    // Set up the close button
                    closeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error showing battery dialog: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error showing battery status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // For testing purposes, enable the button even if not connected
        batteryStatusButton.setEnabled(true);
    }
    
    /**
     * Set up the sensor readings button to show EMG, temperature, and pressure values
     */
    private void setupSensorReadingsButton() {
        sensorReadingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Default values - using sample values for testing
                    float emgValue = 125.75f;
                    float temperatureValue = 36.8f;
                    float pressureValue = 12.45f;
                    
                    // Get the actual sensor values if available
                    if (bluetoothManager != null) {
                        try {
                            emgValue = bluetoothManager.getEmgValue();
                            temperatureValue = bluetoothManager.getTemperatureValue();
                            pressureValue = bluetoothManager.getPressureValue();
                        } catch (Exception e) {
                            Log.e(TAG, "Error getting sensor values: " + e.getMessage());
                        }
                    }
                    
                    // Create a dialog using our custom layout
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    
                    // Create a custom view for the dialog
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_sensor_readings, null);
                    
                    // Find views in the dialog layout
                    TextView emgText = dialogView.findViewById(R.id.text_emg_value);
                    TextView tempText = dialogView.findViewById(R.id.text_temperature_value);
                    TextView pressureText = dialogView.findViewById(R.id.text_pressure_value);
                    TextView lastUpdatedText = dialogView.findViewById(R.id.text_last_updated);
                    Button closeButton = dialogView.findViewById(R.id.button_close);
                    
                    // Set the sensor values
                    emgText.setText(String.format("%.2f μV", emgValue));
                    tempText.setText(String.format("%.1f °C", temperatureValue));
                    pressureText.setText(String.format("%.2f kPa", pressureValue));
                    
                    // Set the last updated text with current time
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    String currentTime = sdf.format(new Date());
                    lastUpdatedText.setText("Last updated: " + currentTime);
                    
                    // Create and show the dialog
                    AlertDialog dialog = builder.setView(dialogView).create();
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }
                    dialog.show();
                    
                    // Set up the close button
                    closeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error showing sensor readings dialog: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error showing sensor readings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // For testing purposes, enable the button even if not connected
        sensorReadingsButton.setEnabled(true);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        requireActivity().registerReceiver(discoveryReceiver, filter);
        
        // Update language text when fragment resumes
        updateCurrentLanguageText();
    }
    
    @Override
    public void onConnectionStateChanged(boolean isConnected) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isConnected) {
                    deviceStatusText.setText(R.string.device_connected);
                    bluetoothIcon.setImageResource(R.drawable.ic_bluetooth_connected);
                    batteryStatusButton.setEnabled(true);
                    sensorReadingsButton.setEnabled(true);
                } else {
                    deviceStatusText.setText(R.string.device_disconnected);
                    bluetoothIcon.setImageResource(android.R.drawable.stat_sys_data_bluetooth);
                    batteryStatusButton.setEnabled(false);
                    sensorReadingsButton.setEnabled(false);
                }
            });
        }
    }
    
    /**
     * Connect to the specified Bluetooth device
     */
    private void connectToDevice(BluetoothDevice device) {
        try {
            // Version-specific permission check
            boolean hasPermission = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (API 31+)
                hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
                if (!hasPermission) {
                    Log.d(TAG, "BLUETOOTH_CONNECT permission not granted for Android 12+");
                    Toast.makeText(requireContext(), "Bluetooth connect permission is required", Toast.LENGTH_SHORT).show();
                    requestBluetoothPermissions();
                    return;
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6-11 (API 23-30)
                hasPermission = ActivityCompat.checkSelfPermission(requireContext(), 
                        Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
                if (!hasPermission) {
                    Log.d(TAG, "BLUETOOTH permission not granted for Android 6-11");
                    requestBluetoothPermissions();
                    return;
                }
            } else { // Below Android 6 (API < 23)
                hasPermission = true; // Permissions granted at install time
            }
            
            // Cancel discovery before connecting to improve connection success rate
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(requireContext(), 
                            Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                } else {
                    bluetoothAdapter.cancelDiscovery();
                }
            }
            
            // Update UI to show connecting state
            deviceStatusText.setText(R.string.connecting);
            
            // Show toast message
            Toast.makeText(requireContext(), R.string.connecting_to_device, Toast.LENGTH_SHORT).show();
            
            // Log connection attempt
            Log.d(TAG, "Attempting to connect to device: " + device.getName() + " [" + device.getAddress() + "]");
            
            // Connect to the device using our BluetoothManager
            bluetoothManager.connectToDevice(device.getName(), new Runnable() {
                @Override
                public void run() {
                    // Connection successful
                    Toast.makeText(requireContext(), R.string.connection_successful, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Successfully connected to device: " + device.getName());
                }
            }, new Runnable() {
                @Override
                public void run() {
                    // Connection failed
                    Toast.makeText(requireContext(), R.string.connection_failed, Toast.LENGTH_SHORT).show();
                    deviceStatusText.setText(R.string.device_disconnected);
                    Log.e(TAG, "Failed to connect to device: " + device.getName());
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when connecting to device: " + e.getMessage());
            Toast.makeText(requireContext(), "Bluetooth permission error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            requestBluetoothPermissions();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error when connecting to device: " + e.getMessage());
            Toast.makeText(requireContext(), "Connection error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Update the list of discovered devices in the scan dialog
     */
    private void updateDeviceList() {
        if (deviceListAdapter != null && scanDialog != null && scanDialog.isShowing()) {
            List<String> deviceNames = new ArrayList<>();
            for (BluetoothDevice device : discoveredDevices) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                String name = device.getName();
                if (name == null || name.isEmpty()) {
                    name = device.getAddress();
                }
                deviceNames.add(name);
            }
            
            deviceListAdapter.clear();
            deviceListAdapter.addAll(deviceNames);
            deviceListAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Close Billing Manager
        if (billingManager != null) {
            billingManager.close();
        }
        
        // Unregister the broadcast receiver
        try {
            requireActivity().unregisterReceiver(discoveryReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered, ignore
        }
        
        // Cancel any ongoing discovery
        if (bluetoothAdapter != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        if (bluetoothAdapter.isDiscovering()) {
                            bluetoothAdapter.cancelDiscovery();
                        }
                    } else {
                        Log.w(TAG, "Cannot check/cancel Bluetooth discovery: BLUETOOTH_SCAN permission not granted");
                    }
                } else {
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when canceling Bluetooth discovery: " + e.getMessage());
            }
        }
    }

    private void showBaridiMobInfoDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Paiement via BaridiMob")
            .setMessage("Voulez-vous procéder au paiement via BaridiMob (carte EDAHABIA) ?\n\nMontant: 1500 DZD/mois")
            .setPositiveButton("Payer maintenant", (dialog, which) -> {
                startBaridiMobPayment();
            })
            .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
            .show();
    }

    private void startBaridiMobPayment() {
        try {
            Intent intent = new Intent(requireActivity(), DemoPaymentActivity.class);
            startActivityForResult(intent, REQUEST_BARIDIMOB_PAYMENT);
        } catch (Exception e) {
            Log.e("SettingsFragment", "Erreur lors du lancement du paiement BaridiMob", e);
            Toast.makeText(requireContext(), "Erreur lors du lancement du paiement", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPremiumDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_premium_features, null);

        TextView featuresText = dialogView.findViewById(R.id.premium_features_text);
        TextView priceText = dialogView.findViewById(R.id.premium_price_text);
        Button purchaseButton = dialogView.findViewById(R.id.premium_purchase_button);
        Button baridiMobButton = dialogView.findViewById(R.id.baridimob_purchase_button);
        Button googlePlayTestButton = dialogView.findViewById(R.id.googleplay_test_button);

        featuresText.setText(R.string.premium_features_list);
        
        // Afficher le prix si disponible, sinon prix par défaut
        if (premiumProductDetails != null) {
            String price = premiumProductDetails.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
            priceText.setText(String.format("Seulement %s/mois", price));
            purchaseButton.setVisibility(View.VISIBLE);
        } else {
            priceText.setText("Seulement 9,99€/mois");
            purchaseButton.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Mode test activé - Aucun paiement réel", Toast.LENGTH_SHORT).show();
        }

        builder.setView(dialogView);
        premiumDialog = builder.create();

        purchaseButton.setOnClickListener(v -> {
            billingManager.launchPurchaseFlow(requireActivity(), premiumProductDetails);
        });

        baridiMobButton.setOnClickListener(v -> {
            showBaridiMobInfoDialog();
        });

        googlePlayTestButton.setOnClickListener(v -> {
            startGooglePlayTest();
        });

        premiumDialog.show();
    }

    private void startGooglePlayTest() {
        try {
            Intent intent = new Intent(requireActivity(), DemoPaymentActivity.class);
            intent.putExtra("isGooglePlayTest", true);
            startActivityForResult(intent, REQUEST_GOOGLEPLAY_TEST);
        } catch (Exception e) {
            Log.e("SettingsFragment", "Erreur lors du lancement du test Google Play Billing", e);
            Toast.makeText(requireContext(), "Erreur lors du test Google Play", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePremiumStatus() {
        if (premiumManager != null && premiumStatusText != null) {
            if (premiumManager.isPremium()) {
                premiumStatusText.setText(R.string.premium_user);
                if (upgradeButton != null) {
                    upgradeButton.setVisibility(View.GONE);
                }
            } else {
                premiumStatusText.setText(R.string.free_user);
                if (upgradeButton != null) {
                    upgradeButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    // BillingManager callback methods
    @Override
    public void onPurchaseSuccess() {
        Toast.makeText(getContext(), "Achat réussi ! Vous êtes maintenant un utilisateur Premium.", Toast.LENGTH_LONG).show();
        if (premiumDialog != null && premiumDialog.isShowing()) {
            premiumDialog.dismiss();
        }
        updatePremiumStatus();
    }

    @Override
    public void onPurchaseFailed(String error) {
        Toast.makeText(getContext(), "L'achat a échoué: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProductsFetched(List<ProductDetails> productDetailsList) {
        if (productDetailsList != null && !productDetailsList.isEmpty()) {
            for (ProductDetails product : productDetailsList) {
                if (product.getProductId().equals(BillingManager.PREMIUM_MONTHLY_SKU)) {
                    this.premiumProductDetails = product;
                    break;
                }
            }
        } else {
            Log.d(TAG, "Aucun produit trouvé.");
        }
    }

    /**
     * Configure le switch pour l'effet de frappe du chatbot
     */
    private void setupTypingEffectSwitch() {
        // Charger l'état actuel depuis les préférences
        SharedPreferences prefs = requireContext().getSharedPreferences("chatbot_preferences", Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean("typing_effect_enabled", true);
        typingEffectSwitch.setChecked(isEnabled);
        
        // Configurer le listener
        typingEffectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Sauvegarder la préférence
            prefs.edit().putBoolean("typing_effect_enabled", isChecked).apply();
            
            // Notifier le ChatbotFragment si il est actif
            if (getActivity() != null) {
                ChatbotFragment chatbotFragment = (ChatbotFragment) getActivity()
                    .getSupportFragmentManager()
                    .findFragmentByTag("chatbot_fragment");
                
                if (chatbotFragment != null) {
                    chatbotFragment.setTypingEffectEnabled(isChecked);
                }
            }
            
            // Afficher un message de confirmation
            String message = isChecked ? "Effet de frappe activé" : "Effet de frappe désactivé";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }
}
