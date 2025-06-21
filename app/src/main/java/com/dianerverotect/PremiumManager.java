package com.dianerverotect;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestionnaire des fonctionnalités premium de l'application
 */
public class PremiumManager {
    private static final String PREF_NAME = "premium_preferences";
    private static final String KEY_IS_PREMIUM = "is_premium";
    
    private final SharedPreferences preferences;
    private static PremiumManager instance;
    
    private PremiumManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized PremiumManager getInstance(Context context) {
        if (instance == null) {
            instance = new PremiumManager(context.getApplicationContext());
        }
        return instance;
    }
    
    public boolean isPremium() {
        return preferences.getBoolean(KEY_IS_PREMIUM, false);
    }
    
    public void setPremium(boolean isPremium) {
        preferences.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply();
    }
    
    // Limites pour les utilisateurs gratuits
    public static final int FREE_HISTORY_DAYS_LIMIT = 30;
    public static final int FREE_EXPORT_LIMIT = 5;
    
    // Fonctionnalités spécifiques
    public boolean hasUnlimitedHistory() {
        return isPremium();
    }
    
    public boolean hasAdvancedAIAnalysis() {
        return isPremium();
    }
    
    public boolean hasAdvancedPDFExport() {
        return isPremium();
    }
    
    public boolean hasExcelExport() {
        return isPremium();
    }
    
    public int getHistoryDaysLimit() {
        return isPremium() ? Integer.MAX_VALUE : FREE_HISTORY_DAYS_LIMIT;
    }
    
    public boolean canExportPDF() {
        if (isPremium()) return true;
        int exportsThisMonth = getExportsThisMonth();
        return exportsThisMonth < FREE_EXPORT_LIMIT;
    }
    
    private int getExportsThisMonth() {
        return preferences.getInt("exports_this_month", 0);
    }
    
    public void incrementExportsCount() {
        int currentCount = getExportsThisMonth();
        preferences.edit().putInt("exports_this_month", currentCount + 1).apply();
    }
    
    public void resetMonthlyExports() {
        preferences.edit().putInt("exports_this_month", 0).apply();
    }
} 