package com.dianerverotect;

import android.content.Context;
import android.os.Bundle;
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
import android.content.SharedPreferences;
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
import com.dianerverotect.chatbot.ExternalSearchService;

public class ChatbotFragment extends Fragment {

    private static final String TAG = "ChatbotFragment";
    private static final String DEBUG_TAG = "CHATBOT_DEBUG"; // Tag pour le débogage
    
    // UI Components
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    
    // Chat messages
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    
    // Medical AI model for handling predictions
    private MedicalAIModel medicalAIModel;
    
    // Dialogflow service for natural language processing
    private DialogflowService dialogflowService;
    
    // External search service for comprehensive answers
    private ExternalSearchService externalSearchService;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    private static final String PREFS_HISTORY = "chatbot_history";
    private static final String KEY_HISTORY = "history_json";
    private static final int MAX_HISTORY = 50;
    private static final String PREFS_CHATBOT = "chatbot_preferences";
    private static final String KEY_TYPING_EFFECT = "typing_effect_enabled";
    
    // Variable pour contrôler l'effet de frappe
    private boolean typingEffectEnabled = true;
    
    // Flag pour éviter les réponses multiples
    private boolean isProcessingMessage = false;
    
    // Language preference
    private String currentLanguage = "en"; // Default to English

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chatbot, container, false);
        
        // Load language preference
        SharedPreferences mainPrefs = requireActivity().getSharedPreferences("DiaNerverotectPrefs", Context.MODE_PRIVATE);
        currentLanguage = mainPrefs.getString("language", "en"); // default to english
        Log.d(DEBUG_TAG, "Current chatbot language set to: " + currentLanguage);
        
        // Drawer et NavigationView
        drawerLayout = view.findViewById(R.id.drawer_layout);
        navigationView = view.findViewById(R.id.navigation_view);
        toolbar = view.findViewById(R.id.toolbar);

        // Icône hamburger ouvre le Drawer
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));

        // Gestion des clics sur le menu latéral
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
            } else if (id == R.id.nav_about) {
                // Dialogue professionnel "À propos du chatbot"
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(48, 32, 48, 32);
                layout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
                layout.setBackgroundResource(R.drawable.rounded_card_background);

                ImageView logo = new ImageView(getContext());
                logo.setImageResource(R.drawable.logo_app);
                LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(140, 140);
                logoParams.bottomMargin = 24;
                logo.setLayoutParams(logoParams);
                layout.addView(logo);

                TextView title = new TextView(getContext());
                title.setText("🤖 À propos du chatbot médical");
                title.setTextSize(22);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                title.setTextColor(getResources().getColor(R.color.aubergine));
                title.setPadding(0, 0, 0, 18);
                layout.addView(title);

                TextView message = new TextView(getContext());
                message.setText(
                    "Notre application embarque un chatbot intelligent conçu pour faciliter la détection précoce de certaines maladies neurologiques, en particulier la neuropathie périphérique et la sclérose latérale amyotrophique (SLA).\n\n"
                    + "🔬 Grâce à une série de questions simples mais cliniquement ciblées, ce chatbot :\n\n"
                    + "📋 Collecte les symptômes sensoriels, moteurs et autonomes ressentis par le patient\n\n"
                    + "🧠 Utilise deux modèles d'intelligence artificielle pour analyser les réponses et prédire l'orientation vers une neuropathie ou une SLA\n\n"
                    + "📄 Génère un rapport synthétique que le patient peut présenter à un professionnel de santé\n\n"
                    + "📈 Permet un suivi dans le temps en cas de réévaluation des symptômes\n\n"
                    + "\n⚠️ Ce chatbot ne remplace pas un diagnostic médical. Il est conçu comme un outil d'aide à la décision et d'orientation préclinique.\n\n"
                    + "💻 Développé par l'équipe projet :\n\n"
                    + "Benaboud Roqia\n"
                    + "Zouaoui Sirine\n"
                    + "Chebout Ibrahim Rassim\n"
                    + "Lameri Lokmane\n\n"
                    + "🧑‍🏫 Encadré par :\n\n"
                    + "Dr. Dehimi Nour El Houda\n"
                    + "Dr. Tolba Zakaria\n\n"
                    + "📬 Contactez-nous\n\n"
                    + "Pour toute suggestion, question ou collaboration, n'hésitez pas à nous contacter :\n\n"
                    + "📧 Email : benaboud.roqia@univ-oeb.dz et zououi.sirine@univ-oeb.dz"
                );
                message.setTextSize(16);
                message.setTextColor(0xFF333333);
                message.setPadding(0, 0, 0, 12);
                layout.addView(message);

                // Ajout des numéros de téléphone cliquables
                TextView phoneNumbers = new TextView(getContext());
                phoneNumbers.setText("Téléphone :\nRoqia : 0796446383\nSirine : 0659415646\nRassim : 0657162706\nLokmane : 0676313198");
                phoneNumbers.setTextSize(16);
                phoneNumbers.setTextColor(getResources().getColor(R.color.aubergine_light));
                phoneNumbers.setPadding(0, 16, 0, 0);
                phoneNumbers.setAutoLinkMask(0);
                phoneNumbers.setLinksClickable(true);
                phoneNumbers.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                String text = phoneNumbers.getText().toString();
                android.text.SpannableString spannable = new android.text.SpannableString(text);
                int startRoqia = text.indexOf("0796446383");
                int endRoqia = startRoqia + "0796446383".length();
                spannable.setSpan(new android.text.style.ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0796446383"));
                        startActivity(intent);
                    }
                }, startRoqia, endRoqia, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                int startSirine = text.indexOf("0659415646");
                int endSirine = startSirine + "0659415646".length();
                spannable.setSpan(new android.text.style.ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0659415646"));
                        startActivity(intent);
                    }
                }, startSirine, endSirine, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                int startRassim = text.indexOf("0657162706");
                int endRassim = startRassim + "0657162706".length();
                spannable.setSpan(new android.text.style.ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0657162706"));
                        startActivity(intent);
                    }
                }, startRassim, endRassim, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                int startLokmane = text.indexOf("0676313198");
                int endLokmane = startLokmane + "0676313198".length();
                spannable.setSpan(new android.text.style.ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0676313198"));
                        startActivity(intent);
                    }
                }, startLokmane, endLokmane, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                phoneNumbers.setText(spannable);
                layout.addView(phoneNumbers);

                ScrollView scrollView = new ScrollView(getContext());
                scrollView.addView(layout);

                new AlertDialog.Builder(getContext())
                        .setView(scrollView)
                        .setPositiveButton("Fermer", null)
                        .setNegativeButton("Contacter", (dialog, which) -> {
                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                            emailIntent.setData(Uri.parse("mailto:benaboud.roqia@univ-oeb.dz, zououi.sirine@univ-oeb.dz"));
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Contact - Chatbot médical");
                            startActivity(Intent.createChooser(emailIntent, "Envoyer un email"));
                        })
                        .show();
                drawerLayout.closeDrawers();
                return true;
            }
            return false;
        });
        
        // Initialize UI components
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button_fab);
        
        // Initialize chat messages and adapter
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(requireContext(), chatMessages);
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        chatRecyclerView.setItemAnimator(new DefaultItemAnimator());
        
        // Initialize services
        medicalAIModel = new MedicalAIModel(requireContext());
        dialogflowService = new DialogflowService(requireContext());
        externalSearchService = new ExternalSearchService(requireContext());
        
        // Charger les préférences du chatbot
        loadChatbotPreferences();
        
        // Add welcome message
        addBotMessage("Hello! I'm NerveBot. How can I help you today?");
        
        // Set up send button click listener
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText("");
            }
        });
        
        return view;
    }
    
    /**
     * Charge les préférences du chatbot
     */
    private void loadChatbotPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_CHATBOT, Context.MODE_PRIVATE);
        typingEffectEnabled = prefs.getBoolean(KEY_TYPING_EFFECT, true);
    }
    
    /**
     * Active ou désactive l'effet de frappe
     * @param enabled true pour activer, false pour désactiver
     */
    public void setTypingEffectEnabled(boolean enabled) {
        this.typingEffectEnabled = enabled;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_CHATBOT, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_TYPING_EFFECT, enabled).apply();
    }
    
    /**
     * Vérifie si l'effet de frappe est activé
     * @return true si activé, false sinon
     */
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
            String greetingResponse = "Hello! How can I help you today?";
            addBotMessage(greetingResponse);
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
                        String errorMsg = "I'm sorry, an error occurred.";
                        addBotMessage(errorMsg);
                    }
                    isProcessingMessage = false;
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    String errorMsg = "Sorry, I couldn't get a response. Please check your internet connection.";
                    addBotMessage(errorMsg);
                    isProcessingMessage = false;
                });
            }
        });
    }
    
    private boolean isGreeting(String message) {
        String lowerMessage = message.toLowerCase().trim();
        String[] greetings = {
            "hello", "hi", "hey", "greetings", "good morning", "good afternoon", "good evening",
            "bonjour", "salut", "coucou", "bonsoir", "bonne journée", "bonne soirée",
            "hola", "buenos dias", "buenas tardes", "buenas noches",
            "ciao", "buongiorno", "buonasera"
        };
        
        for (String greeting : greetings) {
            if (lowerMessage.contains(greeting)) {
                return true;
            }
        }
        
        // Vérifier aussi les messages très courts qui pourraient être des salutations
        if (lowerMessage.length() <= 10 && (lowerMessage.contains("yo") || lowerMessage.contains("hey") || lowerMessage.contains("hi"))) {
            return true;
        }
        
        return false;
    }
    
    private void addUserMessage(String message) {
        chatMessages.add(new ChatMessage(message, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
        saveMessageToHistory(message, false);
    }
    
    private void addBotMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        ChatMessage chatMessage = new ChatMessage(message, false, typingEffectEnabled);
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
        saveMessageToHistory(message, true);
    }
    
    private void scrollToBottom() {
        // Utilisation de postDelayed pour s'assurer que le défilement se produit après le rendu complet
        chatRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (chatMessages.size() > 0 && chatRecyclerView.getLayoutManager() != null) {
                    try {
                        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                    } catch (Exception e) {
                        // Fallback en cas d'erreur
                        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                    }
                }
            }
        }, 300); // Délai légèrement plus long pour s'assurer que la vue est complètement mise à jour
    }
    
    private void saveMessageToHistory(String message, boolean isBot) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE);
        String historyJson = prefs.getString(KEY_HISTORY, "[]");
        try {
            JSONArray array = new JSONArray(historyJson);
            JSONObject obj = new JSONObject();
            obj.put("message", message);
            obj.put("isBot", isBot);
            array.put(obj);
            // Limite à 50 derniers messages
            if (array.length() > MAX_HISTORY) {
                JSONArray newArray = new JSONArray();
                for (int i = array.length() - MAX_HISTORY; i < array.length(); i++) {
                    newArray.put(array.getJSONObject(i));
                }
                array = newArray;
            }
            prefs.edit().putString(KEY_HISTORY, array.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Clean up resources
        if (medicalAIModel != null) {
            medicalAIModel.close();
        }
        
        if (dialogflowService != null) {
            dialogflowService.close();
        }
        
        if (externalSearchService != null) {
            externalSearchService.cleanup();
        }
    }
}
