package com.dianerverotect;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Random;

public class RewardsFragment extends Fragment {
    private static final String PREFS_NAME = "gamification";
    private static final String KEY_POINTS = "points";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rewards, container, false);

        TextView pointsText = view.findViewById(R.id.points_text);
        LinearLayout badgesContainer = view.findViewById(R.id.badges_container);
        LinearLayout challengesContainer = view.findViewById(R.id.challenges_container);

        // Streak (jours consécutifs)
        TextView streakText = view.findViewById(R.id.streak_text);
        int streak = updateStreak();
        streakText.setText("Série de jours actifs : " + streak + (streak > 1 ? " jours" : " jour"));

        // Citation inspirante du jour
        TextView quoteText = view.findViewById(R.id.quote_text);
        quoteText.setText(getQuoteOfTheDay());

        // Ajout d'une barre de progression vers le prochain badge
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressDrawable(getResources().getDrawable(android.R.drawable.progress_horizontal));
        progressBar.setPadding(0, 0, 0, 24);
        ((LinearLayout) view.findViewById(R.id.badges_container)).addView(progressBar);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int points = prefs.getInt(KEY_POINTS, 0);
        pointsText.setText("Points : " + points);

        // Mise à jour de la barre de progression
        int nextBadge = 10;
        if (points < 10) nextBadge = 10;
        else if (points < 50) nextBadge = 50;
        else if (points < 100) nextBadge = 100;
        else nextBadge = 100;
        int progress = Math.min(100, (int) (100.0 * points / nextBadge));
        progressBar.setProgress(progress);
        progressBar.setContentDescription("Progression vers le prochain badge : " + progress + "%");

        // Affichage des badges (exemple simple)
        badgesContainer.removeAllViews();
        if (points >= 10) {
            ImageView badge1 = new ImageView(getContext());
            badge1.setImageResource(R.drawable.ic_check_circle_green);
            badge1.setContentDescription("Badge 10 points");
            badgesContainer.addView(badge1);
        }
        if (points >= 50) {
            ImageView badge2 = new ImageView(getContext());
            badge2.setImageResource(R.drawable.ic_medical_pulse);
            badge2.setContentDescription("Badge 50 points");
            badgesContainer.addView(badge2);
        }
        if (points >= 100) {
            ImageView badge3 = new ImageView(getContext());
            badge3.setImageResource(R.drawable.ic_check_circle);
            badge3.setContentDescription("Badge 100 points");
            badgesContainer.addView(badge3);
        }

        // Badge spécial pour 7 jours d'affilée
        if (streak >= 7) {
            ImageView badgeStreak = new ImageView(getContext());
            badgeStreak.setImageResource(R.drawable.ic_check_circle_green);
            badgeStreak.setContentDescription("Badge 7 jours d'affilée");
            badgesContainer.addView(badgeStreak);
        }

        // Affichage des défis à venir
        challengesContainer.removeAllViews();
        if (points < 10) {
            addChallenge(challengesContainer, "Atteindre 10 points pour débloquer le premier badge !");
        } else if (points < 50) {
            addChallenge(challengesContainer, "Atteindre 50 points pour débloquer un nouveau badge !");
        } else if (points < 100) {
            addChallenge(challengesContainer, "Atteindre 100 points pour devenir un expert !");
        } else {
            addChallenge(challengesContainer, "Bravo, tu as débloqué tous les badges !");
        }

        // --- SUIVI SANTÉ PAR CALENDRIER ---
        android.widget.CalendarView calendarView = view.findViewById(R.id.calendar_view);
        TextView healthStatusText = view.findViewById(R.id.health_status_text);
        android.widget.Button btnToggleHealth = view.findViewById(R.id.btn_toggle_health);

        // Date sélectionnée (par défaut aujourd'hui)
        final long[] selectedDate = {calendarView.getDate()};
        updateHealthUI(selectedDate[0], prefs, healthStatusText, btnToggleHealth);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            selectedDate[0] = cal.getTimeInMillis();
            updateHealthUI(selectedDate[0], prefs, healthStatusText, btnToggleHealth);
        });

        btnToggleHealth.setOnClickListener(v -> {
            String key = "health_" + getDayKey(selectedDate[0]);
            boolean isSick = prefs.getBoolean(key, false);
            prefs.edit().putBoolean(key, !isSick).apply();
            updateHealthUI(selectedDate[0], prefs, healthStatusText, btnToggleHealth);
            // Afficher un Toast avec l'état et la date
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            String dateStr = sdf.format(new java.util.Date(selectedDate[0]));
            String etat = !isSick ? "malade" : "parfait";
            android.widget.Toast.makeText(getContext(), "État pour le " + dateStr + " : " + etat, android.widget.Toast.LENGTH_SHORT).show();
        });

        // --- DÉTECTION D'UN NOUVEAU BADGE ---
        int lastBadgeLevel = prefs.getInt("last_badge_level", 0);
        int newBadgeLevel = 0;
        if (points >= 100 && lastBadgeLevel < 100) newBadgeLevel = 100;
        else if (points >= 50 && lastBadgeLevel < 50) newBadgeLevel = 50;
        else if (points >= 10 && lastBadgeLevel < 10) newBadgeLevel = 10;
        else if (streak >= 7 && lastBadgeLevel < 7) newBadgeLevel = 7;
        if (newBadgeLevel > 0) {
            prefs.edit().putInt("last_badge_level", newBadgeLevel).apply();
            showBadgeCongratsDialog(newBadgeLevel);
        }

        // Gestion du bouton "Voir mes bonus"
        Button btnShowBonus = view.findViewById(R.id.btn_show_bonus);
        TextView bonusTip = view.findViewById(R.id.bonus_health_tip);
        TextView specialContent = view.findViewById(R.id.special_content_text);
        bonusTip.setVisibility(View.GONE);
        specialContent.setVisibility(View.GONE);
        TextView emojiText = new TextView(getContext());
        emojiText.setTextSize(32);
        emojiText.setPadding(0, 16, 0, 0);
        emojiText.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        emojiText.setVisibility(View.GONE);
        // Ajout du TextView emoji dans le layout principal, juste après le bouton bonus
        ((ViewGroup) btnShowBonus.getParent()).addView(emojiText, ((ViewGroup) btnShowBonus.getParent()).indexOfChild(btnShowBonus) + 1);

        btnShowBonus.setOnClickListener(v -> {
            SharedPreferences prefsBonus = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int pointsBonus = prefsBonus.getInt(KEY_POINTS, 0);
            int streakBonus = updateStreak();
            StringBuilder conseils = new StringBuilder();
            StringBuilder contenus = new StringBuilder();
            boolean hasBonus = false;
            StringBuilder emojis = new StringBuilder();
            if (pointsBonus >= 100) {
                conseils.append(getBonusHealthTip()).append("\n");
                contenus.append("- Mini-jeu santé débloqué !\n");
                emojis.append("🎉🏆😃💪👏");
                hasBonus = true;
            }
            if (pointsBonus >= 50) {
                conseils.append(getBonusHealthTip()).append("\n");
                contenus.append("- Article exclusif sur la santé nerveuse.\n");
                emojis.append("💪😃👏✨");
                hasBonus = true;
            }
            if (pointsBonus >= 10) {
                conseils.append(getBonusHealthTip()).append("\n");
                contenus.append("- Exercice de relaxation spécial débutant.\n");
                emojis.append("👏🙂🌟");
                hasBonus = true;
            }
            if (streakBonus >= 7) {
                conseils.append(getBonusHealthTip()).append("\n");
                contenus.append("- Vidéo d'exercices pour la motivation quotidienne.\n");
                emojis.append("🔥🏅");
                hasBonus = true;
            }
            if (hasBonus) {
                bonusTip.setText(conseils.toString().trim());
                bonusTip.setVisibility(View.VISIBLE);
                specialContent.setText(contenus.toString().trim());
                specialContent.setVisibility(View.VISIBLE);
                emojiText.setText(emojis.toString());
                emojiText.setVisibility(View.VISIBLE);
                // Animation de rebond
                emojiText.setScaleX(0.7f);
                emojiText.setScaleY(0.7f);
                emojiText.animate().scaleX(1.1f).scaleY(1.1f).setDuration(400).setStartDelay(100)
                        .withEndAction(() -> emojiText.animate().scaleX(1f).scaleY(1f).setDuration(200));
            } else {
                bonusTip.setText("Débloque un badge pour recevoir un bonus santé !");
                bonusTip.setVisibility(View.VISIBLE);
                specialContent.setVisibility(View.GONE);
                emojiText.setVisibility(View.GONE);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRewardsUI();
    }

    private void updateRewardsUI() {
        if (getView() == null) return;
        TextView pointsText = getView().findViewById(R.id.points_text);
        LinearLayout badgesContainer = getView().findViewById(R.id.badges_container);
        LinearLayout challengesContainer = getView().findViewById(R.id.challenges_container);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int points = prefs.getInt(KEY_POINTS, 0);
        pointsText.setText("Points : " + points);

        badgesContainer.removeAllViews();
        if (points >= 10) {
            ImageView badge1 = new ImageView(getContext());
            badge1.setImageResource(R.drawable.ic_check_circle_green);
            badge1.setContentDescription("Badge 10 points");
            badgesContainer.addView(badge1);
        }
        if (points >= 50) {
            ImageView badge2 = new ImageView(getContext());
            badge2.setImageResource(R.drawable.ic_medical_pulse);
            badge2.setContentDescription("Badge 50 points");
            badgesContainer.addView(badge2);
        }
        if (points >= 100) {
            ImageView badge3 = new ImageView(getContext());
            badge3.setImageResource(R.drawable.ic_check_circle);
            badge3.setContentDescription("Badge 100 points");
            badgesContainer.addView(badge3);
        }

        challengesContainer.removeAllViews();
        if (points < 10) {
            addChallenge(challengesContainer, "Atteindre 10 points pour débloquer le premier badge !");
        } else if (points < 50) {
            addChallenge(challengesContainer, "Atteindre 50 points pour débloquer un nouveau badge !");
        } else if (points < 100) {
            addChallenge(challengesContainer, "Atteindre 100 points pour devenir un expert !");
        } else {
            addChallenge(challengesContainer, "Bravo, tu as débloqué tous les badges !");
        }
    }

    private void addChallenge(LinearLayout container, String text) {
        TextView challenge = new TextView(getContext());
        challenge.setText(text);
        challenge.setTextSize(15);
        challenge.setTextColor(0xFF3F51B5);
        challenge.setPadding(0, 8, 0, 8);
        container.addView(challenge);
    }

    private String getQuoteOfTheDay() {
        String[] quotes = {
            "La santé, c'est le plus grand des biens.",
            "Un petit pas chaque jour mène à de grands résultats.",
            "Le courage, c'est de persévérer quand on a envie d'abandonner.",
            "Prends soin de ton corps, c'est le seul endroit où tu es obligé de vivre.",
            "Chaque effort compte, même les plus petits.",
            "Le sourire est le meilleur remède.",
            "Aujourd'hui est un nouveau départ !"
        };
        // Utilise le jour de l'année pour changer la citation chaque jour
        int dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR);
        return "\u201C" + quotes[dayOfYear % quotes.length] + "\u201D";
    }

    private int updateStreak() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastDay = prefs.getLong("last_streak_day", 0);
        int streak = prefs.getInt("streak", 0);
        java.util.Calendar today = java.util.Calendar.getInstance();
        int currentDay = today.get(java.util.Calendar.DAY_OF_YEAR);
        int currentYear = today.get(java.util.Calendar.YEAR);
        long todayKey = currentYear * 1000L + currentDay;
        if (lastDay == todayKey) {
            // Déjà compté aujourd'hui
            return streak;
        } else if (lastDay == todayKey - 1) {
            // Jour consécutif
            streak++;
        } else {
            // Série interrompue
            streak = 1;
        }
        prefs.edit().putLong("last_streak_day", todayKey).putInt("streak", streak).apply();
        return streak;
    }

    private void updateHealthUI(long date, SharedPreferences prefs, TextView healthStatusText, android.widget.Button btnToggleHealth) {
        String key = "health_" + getDayKey(date);
        boolean isSick = prefs.getBoolean(key, false);
        if (isSick) {
            healthStatusText.setText("Vous avez indiqué être malade ce jour-là.");
            btnToggleHealth.setText("Je suis malade");
            btnToggleHealth.setBackgroundResource(R.drawable.btn_health_status_red);
        } else {
            healthStatusText.setText("Vous avez indiqué être en forme ce jour-là.");
            btnToggleHealth.setText("Je suis parfait");
            btnToggleHealth.setBackgroundResource(R.drawable.btn_health_status_bg);
        }
    }

    private String getDayKey(long millis) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(millis);
        int year = cal.get(java.util.Calendar.YEAR);
        int day = cal.get(java.util.Calendar.DAY_OF_YEAR);
        return year + "_" + day;
    }

    private void showBadgeCongratsDialog(int badgeLevel) {
        String titre = "Félicitations !";
        String message;
        String conseil = getBonusHealthTip();
        String contenuSpecial = "";
        if (badgeLevel == 10) {
            message = "Tu as débloqué ton premier badge !";
            contenuSpecial = "Découvre un exercice de relaxation spécial débutant.";
        } else if (badgeLevel == 50) {
            message = "Bravo, tu as atteint 50 points !";
            contenuSpecial = "Accède à un article exclusif sur la santé nerveuse.";
        } else if (badgeLevel == 100) {
            message = "Incroyable, tu es un expert !";
            contenuSpecial = "Mini-jeu santé débloqué !";
        } else if (badgeLevel == 7) {
            message = "Série de 7 jours consécutifs !";
            contenuSpecial = "Vidéo d'exercices pour la motivation quotidienne.";
        } else {
            message = "Nouveau badge débloqué !";
        }
        message += "\n\n" + conseil + "\n\n" + contenuSpecial;

        // Affichage dans la section bonus du fragment
        if (getView() != null) {
            TextView bonusTip = getView().findViewById(R.id.bonus_health_tip);
            TextView specialContent = getView().findViewById(R.id.special_content_text);
            bonusTip.setText(conseil);
            bonusTip.setVisibility(View.VISIBLE);
            specialContent.setText(contenuSpecial);
            specialContent.setVisibility(View.VISIBLE);
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle(titre)
                .setMessage(message)
                .setPositiveButton("Partager", (dialog, which) -> shareBadge(badgeLevel))
                .setNegativeButton("Fermer", null)
                .setNeutralButton("Voir le contenu spécial", (dialog, which) -> openSpecialContent(badgeLevel))
                .show();
    }

    private String getBonusHealthTip() {
        String[] tips = {
                "Bois un verre d'eau pour bien commencer la journée !",
                "Prends 5 minutes pour respirer profondément.",
                "Fais une courte marche pour activer la circulation.",
                "Pense à t'étirer doucement.",
                "Un sourire améliore la santé !"
        };
        int idx = new java.util.Random().nextInt(tips.length);
        return "Conseil santé bonus : " + tips[idx];
    }

    private void shareBadge(int badgeLevel) {
        String text = "Je viens de débloquer un badge sur Nerve Protect ! #Santé #Motivation";
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
        startActivity(android.content.Intent.createChooser(shareIntent, "Partager via"));
    }

    private void openSpecialContent(int badgeLevel) {
        // Ici tu peux lancer une activité, ouvrir un lien, ou afficher un contenu spécial selon le badge
        // Exemple simple : Toast
        String msg;
        if (badgeLevel == 10) msg = "Exercice de relaxation : inspire, expire, relâche !";
        else if (badgeLevel == 50) msg = "Article : Comment prendre soin de ses nerfs ?";
        else if (badgeLevel == 100) msg = "Mini-jeu : Teste ta mémoire !";
        else if (badgeLevel == 7) msg = "Vidéo : Routine motivation quotidienne.";
        else msg = "Contenu spécial !";
        android.widget.Toast.makeText(getContext(), msg, android.widget.Toast.LENGTH_LONG).show();
    }
} 