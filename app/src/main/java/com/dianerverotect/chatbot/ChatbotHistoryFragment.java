package com.dianerverotect.chatbot;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dianerverotect.R;
import com.itextpdf.text.DocumentException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatbotHistoryFragment extends DialogFragment {
    private RecyclerView rvMessages;
    private RecyclerView rvQuestionnaires;
    private ChatHistoryAdapter messagesAdapter;
    private ChatHistoryAdapter questionnairesAdapter;
    private List<ChatHistoryItem> messagesList = new ArrayList<>();
    private List<ChatHistoryItem> questionnairesList = new ArrayList<>();
    private List<ChatHistoryItem> filteredMessagesList = new ArrayList<>();
    private List<ChatHistoryItem> filteredQuestionnairesList = new ArrayList<>();
    private EditText searchEditText;
    private ImageButton btnFilter;
    private TextView btnExportMessages;
    private TextView btnExportQuestionnaires;
    private ChatHistoryFilter.SortOrder currentSortOrder = ChatHistoryFilter.SortOrder.NEWEST_FIRST;
    private ChatHistoryFilter.FilterType currentFilterType = ChatHistoryFilter.FilterType.ALL;
    private static final String PREFS_HISTORY = "chatbot_history";
    private static final String KEY_HISTORY = "history_json";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_chatbot_history, null);
        initializeViews(view);
        AlertDialog dialog = builder.setView(view).create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    private void initializeViews(View view) {
        // RecyclerViews
        rvMessages = view.findViewById(R.id.rv_messages);
        rvQuestionnaires = view.findViewById(R.id.rv_questionnaires);
        rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        rvQuestionnaires.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesAdapter = new ChatHistoryAdapter(filteredMessagesList);
        questionnairesAdapter = new ChatHistoryAdapter(filteredQuestionnairesList);
        rvMessages.setAdapter(messagesAdapter);
        rvQuestionnaires.setAdapter(questionnairesAdapter);

        // Boutons et textes
        TextView tvNbMessages = view.findViewById(R.id.tv_nb_messages);
        TextView tvNbQuestionnaires = view.findViewById(R.id.tv_nb_questionnaires);
        ImageButton btnClose = view.findViewById(R.id.btn_close_history);
        btnFilter = view.findViewById(R.id.btn_filter);
        searchEditText = view.findViewById(R.id.search_edit_text);
        btnExportMessages = view.findViewById(R.id.btn_export_messages);
        btnExportQuestionnaires = view.findViewById(R.id.btn_export_questionnaires);

        btnClose.setOnClickListener(v -> dismiss());

        // Chargement des données
        loadMessagesHistory(tvNbMessages);
        loadQuestionnairesHistory(tvNbQuestionnaires);

        // Configuration des listeners
        setupListeners();
    }

    private void setupListeners() {
        // Recherche
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                applyFiltersAndSearch();
            }
        });

        // Filtrage
        btnFilter.setOnClickListener(v -> showFilterDialog());

        // Export
        btnExportMessages.setOnClickListener(v -> exportHistory(messagesList, "Historique des Messages"));
        btnExportQuestionnaires.setOnClickListener(v -> exportHistory(questionnairesList, "Historique des Questionnaires"));
    }

    private void applyFiltersAndSearch() {
        String searchQuery = searchEditText.getText().toString();
        
        // Appliquer les filtres aux messages
        ChatHistoryFilter.applySearch(messagesList, filteredMessagesList, searchQuery);
        ChatHistoryFilter.applyFilter(filteredMessagesList, currentFilterType, currentSortOrder);
        
        // Appliquer les filtres aux questionnaires
        ChatHistoryFilter.applySearch(questionnairesList, filteredQuestionnairesList, searchQuery);
        ChatHistoryFilter.applyFilter(filteredQuestionnairesList, currentFilterType, currentSortOrder);
        
        // Mettre à jour les adaptateurs
        messagesAdapter.notifyDataSetChanged();
        questionnairesAdapter.notifyDataSetChanged();
    }

    private void showFilterDialog() {
        String[] options = {"Plus récent d'abord", "Plus ancien d'abord", "Messages uniquement", "Questionnaires uniquement", "Tout afficher"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Filtrer par")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentSortOrder = ChatHistoryFilter.SortOrder.NEWEST_FIRST;
                            break;
                        case 1:
                            currentSortOrder = ChatHistoryFilter.SortOrder.OLDEST_FIRST;
                            break;
                        case 2:
                            currentFilterType = ChatHistoryFilter.FilterType.MESSAGES_ONLY;
                            break;
                        case 3:
                            currentFilterType = ChatHistoryFilter.FilterType.QUESTIONNAIRES_ONLY;
                            break;
                        case 4:
                            currentFilterType = ChatHistoryFilter.FilterType.ALL;
                            break;
                    }
                    applyFiltersAndSearch();
                })
                .show();
    }

    private void exportHistory(List<ChatHistoryItem> items, String title) {
        try {
            File pdfFile = ChatHistoryExporter.exportToPdf(requireContext(), items, title);
            sharePdfFile(pdfFile);
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Erreur lors de l'export : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sharePdfFile(File pdfFile) {
        Uri pdfUri = FileProvider.getUriForFile(requireContext(), 
            requireContext().getPackageName() + ".provider", pdfFile);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Partager l'historique"));
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            int height = (int)(getResources().getDisplayMetrics().heightPixels * 0.80);
            dialog.getWindow().setLayout(width, height);
        }
    }

    private void loadMessagesHistory(TextView tvNbMessages) {
        messagesList.clear();
        filteredMessagesList.clear();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE);
        String historyJson = prefs.getString(KEY_HISTORY, "[]");
        int count = 0;
        try {
            JSONArray array = new JSONArray(historyJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String message = obj.getString("message");
                boolean isBot = obj.getBoolean("isBot");
                String date = obj.has("date") ? obj.getString("date") : "";
                if (!isBot) {
                    ChatHistoryItem item = new ChatHistoryItem(
                        "Utilisateur", date, "Message libre",
                        message.length() > 40 ? message.substring(0, 40) + "..." : message,
                        "-", "-", message
                    );
                    messagesList.add(item);
                    filteredMessagesList.add(item);
                    count++;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        tvNbMessages.setText(count + (count > 1 ? " messages" : " message"));
        messagesAdapter.notifyDataSetChanged();
    }

    private void loadQuestionnairesHistory(TextView tvNbQuestionnaires) {
        questionnairesList.clear();
        filteredQuestionnairesList.clear();
        
        SharedPreferences prefs = requireContext().getSharedPreferences("questionnaire_history", Context.MODE_PRIVATE);
        String historyJson = prefs.getString("history_json", "[]");
        int count = 0;
        try {
            JSONArray array = new JSONArray(historyJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ChatHistoryItem item = new ChatHistoryItem(
                    obj.getString("patientId"),
                    obj.getString("dateHeure"),
                    obj.getString("typeEchange"),
                    obj.getString("resume"),
                    obj.getString("analyseIA"),
                    obj.getString("lienRapport"),
                    obj.getString("conversationComplete")
                );
                questionnairesList.add(item);
                filteredQuestionnairesList.add(item);
                count++;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        tvNbQuestionnaires.setText(count + (count > 1 ? " questionnaires" : " questionnaire"));
        questionnairesAdapter.notifyDataSetChanged();
    }
} 