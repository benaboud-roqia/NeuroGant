package com.dianerverotect;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ProgressBar progressBar;
    private ConstraintLayout emptyStateContainer;

    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tab_layout);
        progressBar = view.findViewById(R.id.progress_loading);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            showEmptyState();
            return view;
        }

        // Set up ViewPager and TabLayout
        setupViewPager();

        return view;
    }

    private void setupViewPager() {
        try {
            // Create fragment list
            List<Fragment> fragments = new ArrayList<>();
            fragments.add(new NeuropathyHistoryFragment());
            fragments.add(new ALSHistoryFragment());

            // Create adapter
            HistoryPagerAdapter pagerAdapter = new HistoryPagerAdapter(this, fragments);
            viewPager.setAdapter(pagerAdapter);

            // Connect TabLayout with ViewPager
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText("Neuropathy");
                        break;
                    case 1:
                        tab.setText("ALS");
                        break;
                }
            }).attach();

            // Add sample data for testing if needed
            if (mAuth.getCurrentUser() != null) {
                insertSampleData();
            }

            // Show content
            showContent();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up ViewPager: " + e.getMessage());
            showEmptyState();
        }
    }
    
    /**
     * Insert sample test data for demonstration purposes
     */
    private void insertSampleData() {
        try {
            String userId = mAuth.getCurrentUser().getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(userId);
            
            // Check if data already exists
            userRef.child("testResults").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Only add sample data if no data exists
                    if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                        Log.d(TAG, "No test data found, adding sample data");
                        
                        // Add sample neuropathy test (yesterday)
                        long yesterdayTimestamp = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
                        DatabaseReference neuropathyRef = userRef.child("testResults").child(String.valueOf(yesterdayTimestamp));
                        
                        neuropathyRef.child("emgScore").setValue(78);
                        neuropathyRef.child("temperatureSensation").setValue(true);
                        neuropathyRef.child("pressureSensation").setValue(false);
                        neuropathyRef.child("recommendation").setValue("Based on your test results, we recommend monitoring your symptoms. Your EMG readings show mild abnormalities that could indicate early peripheral neuropathy. Consider following up with a neurologist.");
                        
                        // Add questionnaire responses
                        DatabaseReference questionnaireRef = neuropathyRef.child("questionnaire");
                        questionnaireRef.child("Do you experience numbness or tingling in your feet?").setValue(true);
                        questionnaireRef.child("Do you have burning pain in your feet?").setValue(false);
                        questionnaireRef.child("Are your symptoms worse at night?").setValue(true);
                        questionnaireRef.child("Do you have difficulty feeling temperature changes?").setValue(false);
                        
                        // Add sample ALS test (today)
                        long todayTimestamp = System.currentTimeMillis();
                        DatabaseReference alsRef = userRef.child("testResults").child(String.valueOf(todayTimestamp));
                        
                        alsRef.child("emgOverallScore").setValue(92);
                        alsRef.child("emgAmplitude").setValue(85);
                        alsRef.child("emgFrequency").setValue(120);
                        alsRef.child("emgDuration").setValue(5);
                        alsRef.child("predictionResult").setValue("Low Risk");
                        alsRef.child("predictionDescription").setValue("Your EMG readings show normal motor unit potentials with no signs of denervation. The AI model predicts a low risk of ALS based on your current readings.");
                        alsRef.child("predictionConfidence").setValue(95);
                        alsRef.child("clinicalNotes").setValue("Normal EMG findings with no evidence of fasciculations or fibrillations. Motor unit morphology and recruitment patterns are within normal limits.");
                        
                        Log.d(TAG, "Sample data added successfully");
                    } else {
                        Log.d(TAG, "Test data already exists, skipping sample data insertion");
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error checking for existing data: " + databaseError.getMessage());
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error inserting sample data: " + e.getMessage());
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.GONE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
        emptyStateContainer.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.VISIBLE);
    }

    /**
     * ViewPager adapter for the history tabs
     */
    private static class HistoryPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {

        private final List<Fragment> fragments;

        public HistoryPagerAdapter(@NonNull Fragment fragment, List<Fragment> fragments) {
            super(fragment);
            this.fragments = fragments;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }
}
