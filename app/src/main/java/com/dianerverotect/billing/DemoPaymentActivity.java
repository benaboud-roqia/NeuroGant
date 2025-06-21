package com.dianerverotect.billing;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.dianerverotect.PremiumManager;
import com.dianerverotect.R;

public class DemoPaymentActivity extends AppCompatActivity {
    private TextView statusText;
    private Button simulateSuccessButton;
    private Button simulateFailureButton;
    private Button simulateCancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_payment);

        statusText = findViewById(R.id.status_text);
        simulateSuccessButton = findViewById(R.id.simulate_success_button);
        simulateFailureButton = findViewById(R.id.simulate_failure_button);
        simulateCancelButton = findViewById(R.id.simulate_cancel_button);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Démonstration Paiement");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        boolean isGooglePlayTest = getIntent().getBooleanExtra("isGooglePlayTest", false);
        if (isGooglePlayTest) {
            setTitle("Test Google Play Billing");
            statusText.setText("Statut actuel: Utilisateur Gratuit\nMode: Test Google Play Billing");
        }

        setupButtons();
        updateStatus();
    }

    private void setupButtons() {
        simulateSuccessButton.setOnClickListener(v -> {
            PremiumManager.getInstance(this).setPremium(true);
            Toast.makeText(this, "Paiement simulé réussi ! Vous êtes maintenant Premium.", Toast.LENGTH_LONG).show();
            updateStatus();
            setResult(RESULT_OK);
            finish();
        });
        simulateFailureButton.setOnClickListener(v -> {
            Toast.makeText(this, "Paiement simulé échoué.", Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
        });
        simulateCancelButton.setOnClickListener(v -> {
            Toast.makeText(this, "Paiement simulé annulé.", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void updateStatus() {
        boolean isPremium = PremiumManager.getInstance(this).isPremium();
        String status = isPremium ? "Utilisateur Premium" : "Utilisateur Gratuit";
        statusText.setText("Statut actuel: " + status);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 