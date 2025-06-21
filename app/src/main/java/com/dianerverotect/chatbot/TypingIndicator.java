package com.dianerverotect.chatbot;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

/**
 * Classe pour gérer l'animation des points de frappe
 */
public class TypingIndicator {
    private final TextView dot1;
    private final TextView dot2;
    private final TextView dot3;
    private final Handler handler;
    private boolean isAnimating = false;
    
    public TypingIndicator(TextView dot1, TextView dot2, TextView dot3) {
        this.dot1 = dot1;
        this.dot2 = dot2;
        this.dot3 = dot3;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Démarre l'animation des points
     */
    public void startAnimation() {
        if (isAnimating) {
            return;
        }
        
        isAnimating = true;
        animateDots();
    }
    
    /**
     * Arrête l'animation des points
     */
    public void stopAnimation() {
        isAnimating = false;
        handler.removeCallbacksAndMessages(null);
        
        // Réinitialiser l'opacité des points
        dot1.setAlpha(1.0f);
        dot2.setAlpha(1.0f);
        dot3.setAlpha(1.0f);
    }
    
    /**
     * Anime les points de frappe
     */
    private void animateDots() {
        if (!isAnimating) {
            return;
        }
        
        // Animation du premier point
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(dot1, "alpha", 1.0f, 0.3f, 1.0f);
        anim1.setDuration(600);
        
        // Animation du deuxième point
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(dot2, "alpha", 1.0f, 0.3f, 1.0f);
        anim2.setDuration(600);
        anim2.setStartDelay(200);
        
        // Animation du troisième point
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(dot3, "alpha", 1.0f, 0.3f, 1.0f);
        anim3.setDuration(600);
        anim3.setStartDelay(400);
        
        // Créer un ensemble d'animations
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(anim1, anim2, anim3);
        
        animatorSet.start();
        
        // Répéter l'animation après 1.2 secondes
        handler.postDelayed(this::animateDots, 1200);
    }
    
    /**
     * Vérifie si l'animation est en cours
     * @return true si l'animation est en cours, false sinon
     */
    public boolean isAnimating() {
        return isAnimating;
    }
    
    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        stopAnimation();
        handler.removeCallbacksAndMessages(null);
    }
} 