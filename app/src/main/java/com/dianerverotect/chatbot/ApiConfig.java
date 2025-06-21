package com.dianerverotect.chatbot;

/**
 * Configuration des clés API pour les services externes
 * Note: Ces clés doivent être configurées par l'utilisateur
 */
public class ApiConfig {
    
    // Clé API OpenAI (optionnelle)
    // Pour obtenir une clé: https://platform.openai.com/api-keys
    public static final String OPENAI_API_KEY = "your_openai_api_key_here";
    
    // Clé API Google Search (optionnelle)
    // Pour obtenir une clé: https://developers.google.com/custom-search/v1/overview
    public static final String GOOGLE_API_KEY = "your_google_api_key_here";
    
    // ID du moteur de recherche Google (optionnel)
    public static final String GOOGLE_SEARCH_ENGINE_ID = "your_search_engine_id_here";
    
    /**
     * Vérifie si les clés API sont configurées
     * @return true si au moins une clé est configurée
     */
    public static boolean hasApiKeys() {
        return !OPENAI_API_KEY.equals("your_openai_api_key_here") ||
               !GOOGLE_API_KEY.equals("your_google_api_key_here");
    }
    
    /**
     * Vérifie si la clé OpenAI est configurée
     * @return true si la clé OpenAI est configurée
     */
    public static boolean hasOpenAIKey() {
        return !OPENAI_API_KEY.equals("your_openai_api_key_here");
    }
    
    /**
     * Vérifie si la clé Google est configurée
     * @return true si la clé Google est configurée
     */
    public static boolean hasGoogleKey() {
        return !GOOGLE_API_KEY.equals("your_google_api_key_here");
    }
} 