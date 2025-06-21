package com.dianerverotect.chatbot;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

/**
 * Classe pour créer un effet de frappe lettre par lettre comme ChatGPT
 */
public class TypewriterEffect {
    private static final int DEFAULT_DELAY = 25; // Délai en millisecondes entre chaque lettre
    private static final int FAST_DELAY = 15; // Délai rapide pour les messages courts
    private static final int SLOW_DELAY = 40; // Délai lent pour les messages longs
    private static final int PAUSE_DELAY = 200; // Délai pour les pauses naturelles
    
    private final TextView textView;
    private final Handler handler;
    private boolean isTyping = false;
    private OnTypingCompleteListener onTypingCompleteListener;
    
    public interface OnTypingCompleteListener {
        void onTypingComplete();
    }
    
    public TypewriterEffect(TextView textView) {
        this.textView = textView;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Démarre l'effet de frappe avec le texte complet
     * @param fullText Le texte complet à afficher lettre par lettre
     */
    public void startTyping(String fullText) {
        if (isTyping) {
            stopTyping();
        }
        
        isTyping = true;
        textView.setText("");
        
        // Détermine le délai en fonction de la longueur du texte
        int baseDelay = getDelayForTextLength(fullText.length());
        
        final int[] currentIndex = {0};
        
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (currentIndex[0] < fullText.length() && isTyping) {
                    char currentChar = fullText.charAt(currentIndex[0]);
                    textView.setText(fullText.substring(0, currentIndex[0] + 1));
                    currentIndex[0]++;
                    
                    // Ajouter des pauses naturelles
                    int delay = baseDelay;
                    if (currentChar == '.' || currentChar == '!' || currentChar == '?') {
                        delay = PAUSE_DELAY; // Pause plus longue après la ponctuation
                    } else if (currentChar == ',' || currentChar == ';' || currentChar == ':') {
                        delay = baseDelay * 2; // Pause moyenne après la virgule
                    } else if (currentChar == ' ') {
                        delay = baseDelay / 2; // Frappe plus rapide pour les espaces
                    }
                    
                    handler.postDelayed(this, delay);
                } else {
                    isTyping = false;
                    if (onTypingCompleteListener != null) {
                        onTypingCompleteListener.onTypingComplete();
                    }
                }
            }
        };
        
        handler.post(runnable);
    }
    
    /**
     * Démarre l'effet de frappe avec un délai personnalisé
     * @param fullText Le texte complet à afficher
     * @param delay Le délai en millisecondes entre chaque lettre
     */
    public void startTyping(String fullText, int delay) {
        if (isTyping) {
            stopTyping();
        }
        
        isTyping = true;
        textView.setText("");
        
        final int[] currentIndex = {0};
        
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (currentIndex[0] < fullText.length() && isTyping) {
                    char currentChar = fullText.charAt(currentIndex[0]);
                    textView.setText(fullText.substring(0, currentIndex[0] + 1));
                    currentIndex[0]++;
                    
                    // Ajouter des pauses naturelles
                    int currentDelay = delay;
                    if (currentChar == '.' || currentChar == '!' || currentChar == '?') {
                        currentDelay = PAUSE_DELAY;
                    } else if (currentChar == ',' || currentChar == ';' || currentChar == ':') {
                        currentDelay = delay * 2;
                    } else if (currentChar == ' ') {
                        currentDelay = delay / 2;
                    }
                    
                    handler.postDelayed(this, currentDelay);
                } else {
                    isTyping = false;
                    if (onTypingCompleteListener != null) {
                        onTypingCompleteListener.onTypingComplete();
                    }
                }
            }
        };
        
        handler.post(runnable);
    }
    
    /**
     * Arrête l'effet de frappe et affiche le texte complet
     */
    public void stopTyping() {
        isTyping = false;
        handler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Vérifie si l'effet de frappe est en cours
     * @return true si l'effet est en cours, false sinon
     */
    public boolean isTyping() {
        return isTyping;
    }
    
    /**
     * Définit le listener pour être notifié quand la frappe est terminée
     * @param listener Le listener
     */
    public void setOnTypingCompleteListener(OnTypingCompleteListener listener) {
        this.onTypingCompleteListener = listener;
    }
    
    /**
     * Détermine le délai approprié en fonction de la longueur du texte
     * @param textLength La longueur du texte
     * @return Le délai en millisecondes
     */
    private int getDelayForTextLength(int textLength) {
        if (textLength < 50) {
            return FAST_DELAY;
        } else if (textLength > 200) {
            return SLOW_DELAY;
        } else {
            return DEFAULT_DELAY;
        }
    }
    
    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        stopTyping();
        handler.removeCallbacksAndMessages(null);
    }
} 