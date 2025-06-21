package com.dianerverotect.billing;

/**
 * Configuration centralisée pour les paiements
 */
public class PaymentConfig {
    
    // Configuration Google Play Billing
    public static final String PREMIUM_MONTHLY_SKU = "premium_monthly_subscription";
    public static final double GOOGLE_PLAY_PRICE = 9.99; // USD
    
    // Configuration BaridiMob
    public static final double BARIDIMOB_PRICE = 1500.0; // DZD
    public static final String BARIDIMOB_CURRENCY = "DZD";
    public static final String BARIDIMOB_PAYMENT_METHOD = "edahabia";
    
    // Configuration Chargily (à remplacer par vos vraies clés)
    public static final String CHARGILY_API_KEY = "votre_cle_api_chargily";
    public static final String CHARGILY_SECRET_KEY = "votre_cle_secrete_chargily";
    public static final String CHARGILY_BASE_URL = "https://epay.chargily.com/api/v1";
    
    // URLs de callback (à configurer selon votre serveur)
    public static final String SUCCESS_URL = "https://votre-domaine.com/success";
    public static final String FAILURE_URL = "https://votre-domaine.com/failure";
    public static final String CANCEL_URL = "https://votre-domaine.com/cancel";
    
    // Mode de développement
    public static final boolean IS_DEVELOPMENT_MODE = true;
    
    // Description des produits
    public static final String PRODUCT_DESCRIPTION = "Abonnement Premium DiaNerve Protect";
    
    /**
     * Vérifie si l'application est en mode développement
     */
    public static boolean isDevelopmentMode() {
        return IS_DEVELOPMENT_MODE || CHARGILY_API_KEY.equals("votre_cle_api_chargily");
    }
    
    /**
     * Retourne le prix formaté selon la devise
     */
    public static String getFormattedPrice(double amount, String currency) {
        switch (currency.toUpperCase()) {
            case "DZD":
                return String.format("%.0f DZD", amount);
            case "USD":
                return String.format("$%.2f", amount);
            case "EUR":
                return String.format("€%.2f", amount);
            default:
                return String.format("%.2f %s", amount, currency);
        }
    }
} 