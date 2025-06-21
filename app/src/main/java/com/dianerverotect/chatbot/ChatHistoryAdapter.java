package com.dianerverotect.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dianerverotect.R;
import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {
    private List<ChatHistoryItem> historyList;

    public ChatHistoryAdapter(List<ChatHistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatHistoryItem item = historyList.get(position);
        holder.textDate.setText(item.getDateHeure());
        holder.textType.setText(item.getTypeEchange());
        holder.textMessage.setText(item.getResume());
        
        // Configuration du clic sur l'item
        holder.itemView.setOnClickListener(v -> {
            // TODO: Implémenter l'action au clic (afficher les détails)
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textDate;
        TextView textType;
        TextView textMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.text_date);
            textType = itemView.findViewById(R.id.text_type);
            textMessage = itemView.findViewById(R.id.text_message);
        }
    }
} 