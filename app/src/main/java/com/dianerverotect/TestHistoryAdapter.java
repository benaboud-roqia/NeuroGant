package com.dianerverotect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TestHistoryAdapter extends RecyclerView.Adapter<TestHistoryAdapter.TestHistoryViewHolder> {

    private final List<TestHistoryItem> testHistoryItems;
    private final Context context;

    public TestHistoryAdapter(Context context, List<TestHistoryItem> testHistoryItems) {
        this.context = context;
        this.testHistoryItems = testHistoryItems;
    }

    @NonNull
    @Override
    public TestHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_test_history, parent, false);
        return new TestHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TestHistoryViewHolder holder, int position) {
        TestHistoryItem item = testHistoryItems.get(position);
        
        // Format the timestamp to a readable date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(item.getTimestamp()));
        
        holder.testDateText.setText(formattedDate);
        
        // Set temperature sensation
        String temperatureResponse = item.isTemperatureSensation() ? 
                context.getString(R.string.yes_response) : 
                context.getString(R.string.no_response);
        holder.temperatureSensationText.setText(temperatureResponse);
        
        // Set pressure sensation
        String pressureResponse = item.isPressureSensation() ? 
                context.getString(R.string.yes_response) : 
                context.getString(R.string.no_response);
        holder.pressureSensationText.setText(pressureResponse);
    }

    @Override
    public int getItemCount() {
        return testHistoryItems.size();
    }

    public void updateData(List<TestHistoryItem> newItems) {
        testHistoryItems.clear();
        testHistoryItems.addAll(newItems);
        notifyDataSetChanged();
    }

    static class TestHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView testDateText;
        TextView temperatureSensationText;
        TextView pressureSensationText;

        TestHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            testDateText = itemView.findViewById(R.id.text_test_date);
            temperatureSensationText = itemView.findViewById(R.id.text_temperature_sensation);
            pressureSensationText = itemView.findViewById(R.id.text_pressure_sensation);
        }
    }

    // Model class for test history items
    public static class TestHistoryItem {
        private final long timestamp;
        private final boolean temperatureSensation;
        private final boolean pressureSensation;

        public TestHistoryItem(long timestamp, boolean temperatureSensation, boolean pressureSensation) {
            this.timestamp = timestamp;
            this.temperatureSensation = temperatureSensation;
            this.pressureSensation = pressureSensation;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isTemperatureSensation() {
            return temperatureSensation;
        }

        public boolean isPressureSensation() {
            return pressureSensation;
        }
    }
}
