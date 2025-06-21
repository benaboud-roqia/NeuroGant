package com.dianerverotect.chatbot;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.appbar.MaterialToolbar;
import android.view.Gravity;

import com.dianerverotect.R;
import com.dianerverotect.chatbot.ChatAdapter;
import com.dianerverotect.chatbot.ChatMessage;
import com.dianerverotect.chatbot.DialogflowService;
import com.dianerverotect.chatbot.MedicalAIModel;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.dianerverotect.chatbot.QuestionnaireMedicalFragment;
import com.dianerverotect.chatbot.ChatbotHistoryFragment;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatbotFragment extends Fragment {

    private static final String TAG = "ChatbotFragment";
    private static final String DEBUG_TAG = "CHATBOT_DEBUG";
    
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private com.google.android.material.floatingactionbutton.FloatingActionButton sendButton;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private MedicalAIModel medicalAIModel;
    private DialogflowService dialogflowService;
    private ExternalSearchService externalSearchService;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    private static final String PREFS_HISTORY = "chatbot_history";
    private static final String KEY_HISTORY = "history_json";
    private static final int MAX_HISTORY = 50;
    private static final String PREFS_CHATBOT = "chatbot_preferences";
    private static final String KEY_TYPING_EFFECT = "typing_effect_enabled";
    
    private boolean typingEffectEnabled = true;
    private boolean isProcessingMessage = false;
    private String currentLanguage = "en";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chatbot, container, false);
        
        SharedPreferences mainPrefs = requireActivity().getSharedPreferences("DiaNerverotectPrefs", Context.MODE_PRIVATE);
        currentLanguage = mainPrefs.getString("language", "en"); 
        
        drawerLayout = view.findViewById(R.id.drawer_layout);
        navigationView = view.findViewById(R.id.navigation_view);
        toolbar = view.findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_questionnaire) {
                DialogFragment dialog = new QuestionnaireMedicalFragment();
                dialog.show(requireActivity().getSupportFragmentManager(), "QuestionnaireDialog");
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_history) {
                DialogFragment dialog = new ChatbotHistoryFragment();
                dialog.show(requireActivity().getSupportFragmentManager(), "HistoryDialog");
                drawerLayout.closeDrawers();
                return true;
            }
            return false;
        });

        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button_fab);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(requireContext(), chatMessages);
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setItemAnimator(new DefaultItemAnimator());

        medicalAIModel = new MedicalAIModel(requireContext());
        dialogflowService = new DialogflowService(requireContext());
        externalSearchService = new ExternalSearchService(requireContext());
        
        loadChatbotPreferences();

        if (chatMessages.isEmpty()) {
            addBotMessage("Hello! I'm NerveBot, your personal medical assistant. How can I help you?");
        }

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText("");
            }
        });

        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText("");
                return true;
            }
            return false;
        });

        chatRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollToBottom();
                chatRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        return view;
    }

    private void loadChatbotPreferences() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_CHATBOT, Context.MODE_PRIVATE);
        typingEffectEnabled = prefs.getBoolean(KEY_TYPING_EFFECT, true);
    }

    public void setTypingEffectEnabled(boolean enabled) {
        typingEffectEnabled = enabled;
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_CHATBOT, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_TYPING_EFFECT, enabled).apply();
    }

    public boolean isTypingEffectEnabled() {
        return typingEffectEnabled;
    }

    private void sendMessage(String message) {
        if (message.isEmpty() || isProcessingMessage) {
            return;
        }

        isProcessingMessage = true;
        addUserMessage(message);

        if (isGreeting(message)) {
            addBotMessage("Hello! How can I help you today?");
            isProcessingMessage = false;
            return;
        }

        externalSearchService.searchAnswer(message, currentLanguage, new ExternalSearchService.SearchCallback() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (response != null && !response.trim().isEmpty()) {
                        addBotMessage(response);
                    } else {
                        addBotMessage("I'm sorry, I couldn't find information on that.");
                    }
                    isProcessingMessage = false;
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    addBotMessage("Sorry, there was an error. Please check your connection.");
                    isProcessingMessage = false;
                });
            }
        });
    }

    private void addUserMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, true);
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        saveMessageToHistory(message, false);
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, false);
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        saveMessageToHistory(message, true);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (chatMessages.size() > 0) {
            new Handler().postDelayed(() -> {
                if (chatRecyclerView != null) {
                    chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                }
            }, 100);
        }
    }

    private void saveMessageToHistory(String message, boolean isBot) {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE);
            String historyJson = prefs.getString(KEY_HISTORY, "[]");
            JSONArray history = new JSONArray(historyJson);

            JSONObject messageObj = new JSONObject();
            messageObj.put("message", message);
            messageObj.put("isBot", isBot);
            messageObj.put("timestamp", System.currentTimeMillis());

            history.put(messageObj);

            if (history.length() > MAX_HISTORY) {
                JSONArray newHistory = new JSONArray();
                for (int i = history.length() - MAX_HISTORY; i < history.length(); i++) {
                    newHistory.put(history.get(i));
                }
                history = newHistory;
            }

            prefs.edit().putString(KEY_HISTORY, history.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving message to history", e);
        }
    }

    private boolean isGreeting(String message) {
        String lowerMessage = message.toLowerCase();
        if ("fr".equals(currentLanguage)) {
            return lowerMessage.contains("bonjour") || lowerMessage.contains("salut") || 
                   lowerMessage.contains("coucou") || lowerMessage.contains("hello") || 
                   lowerMessage.contains("hi") || lowerMessage.contains("hey");
        } else {
            return lowerMessage.contains("hello") || lowerMessage.contains("hi") || 
                   lowerMessage.contains("hey") || lowerMessage.contains("good morning") || 
                   lowerMessage.contains("good afternoon") || lowerMessage.contains("good evening");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (medicalAIModel != null) {
            medicalAIModel.close();
        }
    }
} 