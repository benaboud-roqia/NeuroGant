package com.dianerverotect;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ALSHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private TextView noDataText;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private List<ALSTestResult> testResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_als_history, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_als_history);
        noDataText = view.findViewById(R.id.text_no_als_data);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        testResults = new ArrayList<>();
        
        // Use the proper ALSHistoryAdapter
        adapter = new ALSHistoryAdapter(getContext(), testResults);
        recyclerView.setAdapter(adapter);

        // Load test history data
        loadALSTestHistory();

        return view;
    }

    private void loadALSTestHistory() {
        if (mAuth.getCurrentUser() == null) {
            showNoData();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d("ALSHistory", "Loading ALS test history for user: " + userId);

        // Use the existing testResults path instead of alsResults
        usersRef.child(userId).child("testResults").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                testResults.clear();

                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    showNoData();
                    return;
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        // Get timestamp from the key
                        long timestamp = Long.parseLong(snapshot.getKey());

                        // Get EMG scores
                        int emgOverallScore = 0;
                        int emgAmplitude = 0;
                        int emgFrequency = 0;
                        int emgDuration = 0;

                        if (snapshot.hasChild("emgOverallScore")) {
                            emgOverallScore = snapshot.child("emgOverallScore").getValue(Integer.class);
                        }

                        if (snapshot.hasChild("emgAmplitude")) {
                            emgAmplitude = snapshot.child("emgAmplitude").getValue(Integer.class);
                        }

                        if (snapshot.hasChild("emgFrequency")) {
                            emgFrequency = snapshot.child("emgFrequency").getValue(Integer.class);
                        }

                        if (snapshot.hasChild("emgDuration")) {
                            emgDuration = snapshot.child("emgDuration").getValue(Integer.class);
                        }

                        // Get prediction result
                        String predictionResult = "";
                        String predictionDescription = "";
                        int predictionConfidence = 0;

                        if (snapshot.hasChild("predictionResult")) {
                            predictionResult = snapshot.child("predictionResult").getValue(String.class);
                        }

                        if (snapshot.hasChild("predictionDescription")) {
                            predictionDescription = snapshot.child("predictionDescription").getValue(String.class);
                        }

                        if (snapshot.hasChild("predictionConfidence")) {
                            predictionConfidence = snapshot.child("predictionConfidence").getValue(Integer.class);
                        }

                        // Get clinical notes
                        String clinicalNotes = "";
                        if (snapshot.hasChild("clinicalNotes")) {
                            clinicalNotes = snapshot.child("clinicalNotes").getValue(String.class);
                        }

                        // Create test result item and add to list
                        ALSTestResult result = new ALSTestResult(
                                timestamp, emgOverallScore, emgAmplitude, emgFrequency, emgDuration,
                                predictionResult, predictionDescription, predictionConfidence, clinicalNotes);
                        testResults.add(result);
                    } catch (Exception e) {
                        // Skip this item if there's an error
                    }
                }

                // Sort by timestamp (newest first)
                Collections.sort(testResults, new Comparator<ALSTestResult>() {
                    @Override
                    public int compare(ALSTestResult o1, ALSTestResult o2) {
                        return Long.compare(o2.getTimestamp(), o1.getTimestamp());
                    }
                });

                // Update UI
                if (testResults.isEmpty()) {
                    showNoData();
                } else {
                    showData();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showNoData();
            }
        });
    }

    private void showData() {
        recyclerView.setVisibility(View.VISIBLE);
        noDataText.setVisibility(View.GONE);
    }

    private void showNoData() {
        recyclerView.setVisibility(View.GONE);
        noDataText.setVisibility(View.VISIBLE);
    }

    /**
     * Temporary adapter class to resolve compilation errors
     * This will be replaced by the actual ALSHistoryAdapter once it's properly compiled
     */
    private class TempALSHistoryAdapter extends RecyclerView.Adapter<TempALSHistoryAdapter.ViewHolder> {
        private final List<ALSTestResult> items;

        TempALSHistoryAdapter(List<ALSTestResult> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ALSTestResult item = items.get(position);
            holder.textView.setText("ALS Test: " + new Date(item.getTimestamp()).toString());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(View view) {
                super(view);
                textView = view.findViewById(android.R.id.text1);
            }
        }
    }

    // Model class for ALS test results
    public static class ALSTestResult {
        private final long timestamp;
        private final int emgOverallScore;
        private final int emgAmplitude;
        private final int emgFrequency;
        private final int emgDuration;
        private final String predictionResult;
        private final String predictionDescription;
        private final int predictionConfidence;
        private final String clinicalNotes;

        public ALSTestResult(long timestamp, int emgOverallScore, int emgAmplitude, int emgFrequency,
                             int emgDuration, String predictionResult, String predictionDescription,
                             int predictionConfidence, String clinicalNotes) {
            this.timestamp = timestamp;
            this.emgOverallScore = emgOverallScore;
            this.emgAmplitude = emgAmplitude;
            this.emgFrequency = emgFrequency;
            this.emgDuration = emgDuration;
            this.predictionResult = predictionResult;
            this.predictionDescription = predictionDescription;
            this.predictionConfidence = predictionConfidence;
            this.clinicalNotes = clinicalNotes;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getEmgOverallScore() {
            return emgOverallScore;
        }

        public int getEmgAmplitude() {
            return emgAmplitude;
        }

        public int getEmgFrequency() {
            return emgFrequency;
        }

        public int getEmgDuration() {
            return emgDuration;
        }

        public String getPredictionResult() {
            return predictionResult;
        }

        public String getPredictionDescription() {
            return predictionDescription;
        }

        public int getPredictionConfidence() {
            return predictionConfidence;
        }

        public String getClinicalNotes() {
            return clinicalNotes;
        }
    }
}
