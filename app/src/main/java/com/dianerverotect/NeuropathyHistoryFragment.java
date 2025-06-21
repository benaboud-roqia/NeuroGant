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

public class NeuropathyHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private TextView noDataText;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private List<NeuropathyTestResult> testResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_neuropathy_history, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_neuropathy_history);
        noDataText = view.findViewById(R.id.text_no_neuropathy_data);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        testResults = new ArrayList<>();

        // Use the proper NeuropathyHistoryAdapter
        adapter = new NeuropathyHistoryAdapter(getContext(), testResults);
        recyclerView.setAdapter(adapter);

        // Load test history data
        loadNeuropathyTestHistory();

        return view;
    }

    private void loadNeuropathyTestHistory() {
        if (mAuth.getCurrentUser() == null) {
            showNoData();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d("NeuropathyHistory", "Loading neuropathy test history for user: " + userId);

        // Use the existing testResults path instead of neuropathyResults
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

                        // Get EMG score
                        int emgScore = 0;
                        if (snapshot.hasChild("emgScore")) {
                            emgScore = snapshot.child("emgScore").getValue(Integer.class);
                        }

                        // Get temperature and pressure sensation values
                        boolean temperatureSensation = false;
                        boolean pressureSensation = false;

                        if (snapshot.hasChild("temperatureSensation")) {
                            Object tempValue = snapshot.child("temperatureSensation").getValue();
                            if (tempValue instanceof Boolean) {
                                temperatureSensation = (Boolean) tempValue;
                            } else if (tempValue instanceof String) {
                                temperatureSensation = Boolean.parseBoolean((String) tempValue);
                            }
                        }

                        if (snapshot.hasChild("pressureSensation")) {
                            Object pressValue = snapshot.child("pressureSensation").getValue();
                            if (pressValue instanceof Boolean) {
                                pressureSensation = (Boolean) pressValue;
                            } else if (pressValue instanceof String) {
                                pressureSensation = Boolean.parseBoolean((String) pressValue);
                            }
                        }

                        // Get recommendation
                        String recommendation = "";
                        if (snapshot.hasChild("recommendation")) {
                            recommendation = snapshot.child("recommendation").getValue(String.class);
                        }

                        // Get questionnaire responses
                        List<QuestionResponse> questionResponses = new ArrayList<>();
                        if (snapshot.hasChild("questionnaire")) {
                            DataSnapshot questionnaireSnapshot = snapshot.child("questionnaire");
                            for (DataSnapshot questionSnapshot : questionnaireSnapshot.getChildren()) {
                                String question = questionSnapshot.getKey();
                                boolean response = questionSnapshot.getValue(Boolean.class);
                                questionResponses.add(new QuestionResponse(question, response));
                            }
                        }

                        // Create test result item and add to list
                        NeuropathyTestResult result = new NeuropathyTestResult(
                                timestamp, emgScore, temperatureSensation, pressureSensation,
                                recommendation, questionResponses);
                        testResults.add(result);
                    } catch (Exception e) {
                        // Skip this item if there's an error
                    }
                }

                // Sort by timestamp (newest first)
                Collections.sort(testResults, new Comparator<NeuropathyTestResult>() {
                    @Override
                    public int compare(NeuropathyTestResult o1, NeuropathyTestResult o2) {
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
     * This will be replaced by the actual NeuropathyHistoryAdapter once it's properly compiled
     */
    private class TempNeuropathyHistoryAdapter extends RecyclerView.Adapter<TempNeuropathyHistoryAdapter.ViewHolder> {
        private final List<NeuropathyTestResult> items;

        TempNeuropathyHistoryAdapter(List<NeuropathyTestResult> items) {
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
            NeuropathyTestResult item = items.get(position);
            holder.textView.setText("Neuropathy Test: " + new Date(item.getTimestamp()).toString());
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

    // Model class for neuropathy test results
    public static class NeuropathyTestResult {
        private final long timestamp;
        private final int emgScore;
        private final boolean temperatureSensation;
        private final boolean pressureSensation;
        private final String recommendation;
        private final List<QuestionResponse> questionResponses;

        public NeuropathyTestResult(long timestamp, int emgScore, boolean temperatureSensation,
                                    boolean pressureSensation, String recommendation,
                                    List<QuestionResponse> questionResponses) {
            this.timestamp = timestamp;
            this.emgScore = emgScore;
            this.temperatureSensation = temperatureSensation;
            this.pressureSensation = pressureSensation;
            this.recommendation = recommendation;
            this.questionResponses = questionResponses;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getEmgScore() {
            return emgScore;
        }

        public boolean isTemperatureSensation() {
            return temperatureSensation;
        }

        public boolean isPressureSensation() {
            return pressureSensation;
        }

        public String getRecommendation() {
            return recommendation;
        }

        public List<QuestionResponse> getQuestionResponses() {
            return questionResponses;
        }
    }

    // Model class for question responses
    public static class QuestionResponse {
        private final String question;
        private final boolean response;

        public QuestionResponse(String question, boolean response) {
            this.question = question;
            this.response = response;
        }

        public String getQuestion() {
            return question;
        }

        public boolean isResponse() {
            return response;
        }
    }
}
