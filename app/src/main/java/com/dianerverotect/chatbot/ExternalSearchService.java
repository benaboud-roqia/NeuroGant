package com.dianerverotect.chatbot;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONArray;
import com.dianerverotect.chatbot.MedicalKnowledgeBase;
import com.dianerverotect.chatbot.ApiConfig;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Service pour rechercher des informations depuis des sources externes
 */
public class ExternalSearchService {
    private static final String TAG = "ExternalSearchService";
    private static final String DEBUG_TAG = "CHATBOT_DEBUG"; // Même tag pour le débogage
    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    
    public ExternalSearchService(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient();
        this.executor = Executors.newFixedThreadPool(3);
    }
    
    /**
     * Recherche une réponse depuis plusieurs sources
     * @param query La question de l'utilisateur
     * @param languageCode The language code ("fr" or "en").
     * @param callback Callback pour retourner la réponse
     */
    public void searchAnswer(String query, String languageCode, SearchCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(DEBUG_TAG, "ExternalSearch: Searching for: '" + query + "' in language: " + languageCode);

                // 1. Search local knowledge base based on language
                String localResponse = ("fr".equals(languageCode))
                        ? MedicalKnowledgeBase.searchKnowledge(query)
                        : MedicalKnowledgeBase_EN.searchKnowledge(query);

                if (localResponse != null && !localResponse.isEmpty()) {
                    Log.d(DEBUG_TAG, "ExternalSearch: Found in local knowledge base.");
                    callback.onSuccess(localResponse);
                    return;
                }

                // 2. Search Wikipedia based on language
                String wikiResponse = searchWikipedia(query, languageCode);
                if (wikiResponse != null && !wikiResponse.isEmpty()) {
                    Log.d(DEBUG_TAG, "ExternalSearch: Found on Wikipedia.");
                    callback.onSuccess(wikiResponse);
                    return;
                }

                // 3. Search OpenAI if key is available
                if (ApiConfig.hasOpenAIKey()) {
                    String openaiResponse = searchOpenAI(query, languageCode);
                    if (openaiResponse != null && !openaiResponse.isEmpty()) {
                        Log.d(DEBUG_TAG, "ExternalSearch: Found on OpenAI.");
                        callback.onSuccess(openaiResponse);
                        return;
                    }
                }

                // 4. Default response if no other result is found
                Log.d(DEBUG_TAG, "ExternalSearch: No result found, providing default response.");
                callback.onSuccess(generateDefaultResponse(query, languageCode));

            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Error during external search", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Recherche avec OpenAI
     */
    private String searchOpenAI(String query, String languageCode) {
        if (!ApiConfig.hasOpenAIKey()) {
            return null;
        }
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "gpt-3.5-turbo");
            
            JSONArray messages = new JSONArray();
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            String systemPrompt = ("fr".equals(languageCode))
                    ? "Tu es un assistant médical spécialisé dans les maladies neurologiques comme la neuropathie et la SLA. Fournis des réponses informatives, concises et rassurantes. Réponds uniquement en français."
                    : "You are a medical assistant specializing in neurological diseases like neuropathy and ALS. Provide informative, concise, and reassuring answers. Respond only in English.";
            systemMessage.put("content", systemPrompt);
            messages.put(systemMessage);
            
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", query);
            messages.put(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);
            
            // Créer la requête HTTP
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            
            Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + ApiConfig.OPENAI_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    JSONObject json = new JSONObject(jsonResponse);
                    
                    if (json.has("choices")) {
                        JSONArray choices = json.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            if (choice.has("message")) {
                                JSONObject message = choice.getJSONObject("message");
                                if (message.has("content")) {
                                    return message.getString("content");
                                }
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Erreur API OpenAI: " + response.code() + " - " + response.message());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur OpenAI: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Recherche avec Wikipédia
     */
    private String searchWikipedia(String query, String languageCode) {
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
            String urlString = "https://" + languageCode + ".wikipedia.org/api/rest_v1/page/summary/" + encodedQuery;
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            try (Response response = httpClient.newCall(new Request.Builder()
                .url(urlString)
                .build())
                .execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    JSONObject json = new JSONObject(jsonResponse);
                    
                    if (json.has("extract")) {
                        String extract = json.getString("extract");
                        if (extract.length() > 200) {
                            extract = extract.substring(0, 200) + "...";
                        }
                        return "Selon Wikipédia : " + extract;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur Wikipédia: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Recherche avec Google (simulation)
     */
    private String searchGoogle(String query) {
        // Note: L'API Google Search nécessite une clé API et des quotas
        // Pour l'instant, on simule une recherche
        return null;
    }
    
    /**
     * Génère une réponse par défaut basée sur la base de connaissances locale
     */
    private String generateDefaultResponse(String query, String languageCode) {
        if ("fr".equals(languageCode)) {
            return "Je suis désolé, je n'ai pas trouvé d'informations précises sur '" + query + "'. Pourrais-tu reformuler ta question ou essayer un autre terme ? Pour des questions médicales complexes, il est toujours préférable de consulter un professionnel de la santé.";
        } else {
            return "I'm sorry, I couldn't find specific information about '" + query + "'. Could you please rephrase your question or try another term? For complex medical questions, it is always best to consult a healthcare professional.";
        }
    }
    
    /**
     * Interface pour les callbacks de recherche
     */
    public interface SearchCallback {
        void onSuccess(String response);
        void onError(Exception e);
    }
    
    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        executor.shutdown();
    }
} 