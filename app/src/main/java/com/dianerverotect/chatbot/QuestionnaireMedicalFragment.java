package com.dianerverotect.chatbot;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.dianerverotect.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.widget.ScrollView;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.widget.ImageButton;
import android.content.SharedPreferences;

public class QuestionnaireMedicalFragment extends DialogFragment {
    private LinearLayout questionsContainer;
    private Button validateButton;
    private Button printButton;
    private Spinner diseaseSelector;
    private List<EditText> answerFields = new ArrayList<>();
    private List<String> questions = new ArrayList<>();

    private List<String> recommandationsNeuropathieOui = new ArrayList<String>() {{
        add("Ces sensations indiquent une atteinte sensitive. Une neuropathie périphérique est possible.");
        add("Ce type de douleur est fréquent dans les neuropathies douloureuses. Consultez un neurologue.");
        add("Cela peut refléter une atteinte de la sensibilité proprioceptive.");
        add("La perte de perception thermique est un signe classique de neuropathie.");
        add("L'allodynie est un symptôme courant dans certaines neuropathies chroniques.");
        add("La faiblesse peut survenir dans les neuropathies avancées. Une évaluation EMG est recommandée.");
        add("Cela peut être dû à une perte de la proprioception ou une faiblesse motrice périphérique.");
        add("Les crampes peuvent être liées à une neuropathie.");
        add("La neuropathie autonome peut entraîner une sécheresse inhabituelle.");
        add("Cela peut indiquer une atteinte des nerfs autonomes périphériques.");
        add("Une atteinte autonome peut aussi affecter le système digestif ou urinaire.");
        add("Le diabète est la première cause de neuropathie. Un suivi régulier est essentiel.");
        add("La chronicité augmente le risque de neuropathie installée.");
        add("L'apparition progressive est typique d'une neuropathie d'évolution lente (ex. diabétique).");
    }};
    private List<String> recommandationsNeuropathieNon = new ArrayList<String>() {{
        add("L'absence de paresthésies réduit la probabilité d'une atteinte sensitive périphérique.");
        add("L'absence de douleurs neuropathiques indique que l'atteinte sensorielle est moins probable.");
        add("Le maintien d'une bonne perception plantaire est un bon signe neurologique.");
        add("La perception thermique conservée indique une fonction nerveuse sensorielle correcte.");
        add("L'absence d'allodynie oriente vers une forme non douloureuse ou fonction nerveuse normale.");
        add("L'absence de faiblesse indique que les nerfs moteurs ne semblent pas affectés actuellement.");
        add("L'équilibre préservé réduit la probabilité d'une neuropathie proprioceptive.");
        add("L'absence de crampes ne signifie pas l'absence de neuropathie, mais c'est un signe de bon tonus musculaire.");
        add("La peau hydratée suggère une fonction autonome intacte.");
        add("L'absence de trouble vasomoteur est rassurante.");
        add("L'absence de troubles digestifs ou urinaires est un bon signe végétatif.");
        add("L'absence de maladies chroniques réduit le risque de neuropathie secondaire.");
        add("Symptômes récents ou inexistants : surveiller leur apparition dans le futur.");
        add("Une absence de progression ou de symptôme est rassurante.");
    }};
    private List<String> recommandationsSlaOui = new ArrayList<String>() {{
        add("La faiblesse musculaire progressive est un signe d'alerte pour une maladie neuromusculaire.");
        add("Cela indique une atteinte motrice distale. Une évaluation neurologique est urgente.");
        add("La perte de force et la maladresse sont préoccupantes. Un EMG est recommandé.");
        add("L'atrophie musculaire est un symptôme d'atteinte du motoneurone inférieur.");
        add("Les fasciculations sont un signe fréquent de SLA. Un neurologue doit être consulté rapidement.");
        add("L'atteinte bulbaire (troubles de la parole) est caractéristique des formes bulbaires de SLA.");
        add("Une altération vocale peut être un signe précoce de SLA bulbaire.");
        add("Les troubles de la déglutition peuvent compromettre la respiration. Évaluation urgente recommandée.");
        add("Risque d'aspiration pulmonaire élevé. Il faut consulter rapidement un neurologue.");
        add("La dénutrition est un signe d'évolution. Une prise en charge nutritionnelle est souvent nécessaire.");
        add("Une évolution rapide est caractéristique de la SLA.");
        add("Ces symptômes sont liés à l'atteinte du motoneurone supérieur.");
        add("Si un examen a été fait, il faut le communiquer au médecin pour suivi ou confirmation.");
    }};
    private List<String> recommandationsSlaNon = new ArrayList<String>() {{
        add("L'absence de faiblesse est rassurante vis-à-vis d'une maladie motoneuronale.");
        add("Les gestes fins sont bien conservés, aucun déficit moteur distal détecté.");
        add("Une bonne motricité des membres inférieurs est un signe neurologique normal.");
        add("Pas d'atrophie observée = intégrité musculaire apparente.");
        add("L'absence de fasciculations indique qu'il n'y a pas de décharge motoneuronale anormale visible.");
        add("Une articulation claire écarte une atteinte bulbaire à ce stade.");
        add("Une voix normale est un bon signe fonctionnel de la zone bulbaire.");
        add("L'absence de dysphagie est rassurante quant à l'intégrité des nerfs crâniens impliqués.");
        add("L'absence de fausses routes diminue le risque de complication respiratoire neurologique.");
        add("L'absence de perte de poids indique un bon état général et une alimentation correcte.");
        add("Aucun symptôme évolutif rapide détecté — surveillance clinique standard.");
        add("L'absence de spasticité musculaire est rassurante pour l'axe central.");
        add("En l'absence de bilan, il est conseillé d'envisager un suivi uniquement si des signes apparaissent.");
    }};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaire_medical, container, false);
        questionsContainer = view.findViewById(R.id.questions_container);
        validateButton = view.findViewById(R.id.validate_button);
        printButton = view.findViewById(R.id.print_button);
        Context context = getContext();

        // Ajout du sélecteur de maladie
        diseaseSelector = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new String[]{"ALS", "Neuropathie"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        diseaseSelector.setAdapter(adapter);
        questionsContainer.addView(diseaseSelector, 0);

        // Chargement initial des questions pour ALS
        loadQuestionsFromAsset("als_info.json");

        // Changement de maladie => recharge les questions
        diseaseSelector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    loadQuestionsFromAsset("als_info.json");
                } else {
                    loadQuestionsFromAsset("neuropathy_info.json");
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        validateButton.setOnClickListener(v -> {
            List<String> answers = new ArrayList<>();
            for (EditText field : answerFields) {
                answers.add(field.getText().toString());
            }
            Toast.makeText(context, "Réponses enregistrées !", Toast.LENGTH_SHORT).show();
            // Sauvegarder le questionnaire
            saveQuestionnaireToHistory(questions, answers, diseaseSelector.getSelectedItem().toString());

            // Ajout de points de gamification
            SharedPreferences prefs = requireContext().getSharedPreferences("gamification", Context.MODE_PRIVATE);
            int points = prefs.getInt("points", 0);
            points += 10; // 10 points pour un questionnaire rempli
            prefs.edit().putInt("points", points).apply();
        });

        printButton.setOnClickListener(v -> {
            String rapport = genererRapportComplet(questions, answerFields, diseaseSelector.getSelectedItem().toString());
            
            // Sauvegarder le questionnaire avant de l'afficher
            List<String> answers = new ArrayList<>();
            for (EditText field : answerFields) {
                answers.add(field.getText().toString());
            }
            saveQuestionnaireToHistory(questions, answers, diseaseSelector.getSelectedItem().toString());

            // Affiche le rapport dans un dialogue scrollable
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Rapport médical");
            TextView rapportView = new TextView(getContext());
            rapportView.setText(rapport);
            rapportView.setPadding(32, 32, 32, 32);
            rapportView.setTextSize(16);
            ScrollView scrollView = new ScrollView(getContext());
            scrollView.addView(rapportView);
            builder.setView(scrollView);
            builder.setPositiveButton("Fermer", null);
            builder.setNeutralButton("Exporter en PDF", (dialog, which) -> {
                exportRapportToPdfAndShare(rapport);
            });
            builder.show();
        });

        // Bouton fermer
        ImageButton btnClose = view.findViewById(R.id.btn_close_questionnaire);
        btnClose.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private void loadQuestionsFromAsset(String fileName) {
        questionsContainer.removeViews(1, Math.max(0, questionsContainer.getChildCount() - 1)); // Garde le Spinner en haut
        answerFields.clear();
        questions.clear();
        try {
            Context context = getContext();
            InputStream is = context.getAssets().open("dialogflow-intents/" + fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(json);
            JSONArray trainingPhrases = obj.getJSONArray("trainingPhrases");
            for (int i = 0; i < trainingPhrases.length(); i++) {
                JSONObject phraseObj = trainingPhrases.getJSONObject(i);
                JSONArray parts = phraseObj.getJSONArray("parts");
                if (parts.length() > 0) {
                    String question = parts.getJSONObject(0).getString("text");
                    questions.add(question);
                }
            }
            // Affichage dynamique des questions
            for (String question : questions) {
                TextView qView = new TextView(context);
                qView.setText(question);
                qView.setTextSize(16);
                qView.setTextColor(getResources().getColor(R.color.aubergine));
                qView.setPadding(0, 16, 0, 4);
                questionsContainer.addView(qView);
                EditText answer = new EditText(context);
                answer.setHint("Votre réponse...");
                answer.setBackgroundResource(R.drawable.rounded_card_background);
                answer.setPadding(16, 8, 16, 8);
                questionsContainer.addView(answer);
                answerFields.add(answer);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void exportRapportToPdfAndShare(String rapportText) {
        Context context = getContext();
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        int pageWidth = 595; // A4
        int pageHeight = 842;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        int y = 40;
        // Logo
        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo_app);
        if (logo != null) {
            int logoWidth = 80, logoHeight = 80;
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoWidth, logoHeight, false);
            canvas.drawBitmap(scaledLogo, (pageWidth - logoWidth) / 2f, y, null);
            y += logoHeight + 10;
        }
        // Titre
        paint.setColor(getResources().getColor(R.color.aubergine));
        paint.setTextSize(22);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String titre = "Rapport médical";
        float titreWidth = paint.measureText(titre);
        canvas.drawText(titre, (pageWidth - titreWidth) / 2f, y + 30, paint);
        y += 50;
        // Ligne de séparation
        paint.setStrokeWidth(3);
        canvas.drawLine(40, y, pageWidth - 40, y, paint);
        y += 20;
        // Tableau questions/réponses/reco
        paint.setColor(0xFF333333);
        paint.setTextSize(13);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Question", 40, y, paint);
        canvas.drawText("Réponse", 250, y, paint);
        canvas.drawText("Recommandation", 400, y, paint);
        y += 18;
        paint.setStrokeWidth(1);
        canvas.drawLine(40, y, pageWidth - 40, y, paint);
        y += 10;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        List<String> questions = this.questions;
        List<String> answers = new ArrayList<>();
        for (EditText field : answerFields) answers.add(field.getText().toString());
        List<String> recos = genererRecommandationsParQuestion(questions, answers, diseaseSelector.getSelectedItem().toString());
        for (int i = 0; i < questions.size(); i++) {
            String q = questions.get(i);
            String a = answers.get(i);
            String r = recos.get(i);
            // Découpage pour éviter dépassement largeur
            String qShort = q.length() > 30 ? q.substring(0, 30) + "..." : q;
            String aShort = a.length() > 18 ? a.substring(0, 18) + "..." : a;
            String rShort = r.length() > 22 ? r.substring(0, 22) + "..." : r;
            canvas.drawText(qShort, 40, y, paint);
            canvas.drawText(aShort, 250, y, paint);
            canvas.drawText(rShort, 400, y, paint);
            y += 18;
            if (y > pageHeight - 120) break;
        }
        y += 20;
        // Synthèse IA
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(15);
        canvas.drawText("Avis de l'IA :", 40, y, paint);
        y += 18;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(13);
        String[] synthese = genererRecommandations(questions, answers, diseaseSelector.getSelectedItem().toString()).split("\\n");
        for (String line : synthese) {
            canvas.drawText(line, 40, y, paint);
            y += 16;
            if (y > pageHeight - 60) break;
        }
        y += 18;
        // Avis du médecin généré automatiquement
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
        paint.setTextSize(14);
        canvas.drawText("Avis du médecin :", 40, y, paint);
        y += 18;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(13);
        String[] avisMedecin = genererAvisMedecinAuto(diseaseSelector.getSelectedItem().toString()).split("\\n");
        for (String line : avisMedecin) {
            if (y > pageHeight - 40) break;
            canvas.drawText(line, 40, y, paint);
            y += 16;
        }
        document.finishPage(page);
        try {
            String maladie = diseaseSelector.getSelectedItem().toString().toLowerCase();
            String dateStr = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(new Date());
            String fileName = "rapport_" + maladie + "_" + dateStr + ".pdf";
            File pdfDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "rapports");
            if (!pdfDir.exists()) pdfDir.mkdirs();
            File pdfFile = new File(pdfDir, fileName);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", pdfFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Partager le rapport PDF"));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Erreur lors de la création du PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> genererRecommandationsParQuestion(List<String> questions, List<String> answers, String maladie) {
        List<String> recos = new ArrayList<>();
        boolean isNeuro = maladie.toLowerCase().contains("neuropath");
        boolean isSla = maladie.toLowerCase().contains("sla");
        for (int i = 0; i < questions.size(); i++) {
            String a = answers.get(i).trim().toLowerCase();
            String reco = "-";
            if (isNeuro && i < recommandationsNeuropathieOui.size()) {
                if (a.equals("oui")) reco = recommandationsNeuropathieOui.get(i);
                else if (a.equals("non")) reco = recommandationsNeuropathieNon.get(i);
            } else if (isSla && i < recommandationsSlaOui.size()) {
                if (a.equals("oui")) reco = recommandationsSlaOui.get(i);
                else if (a.equals("non")) reco = recommandationsSlaNon.get(i);
            }
            recos.add(reco);
        }
        return recos;
    }

    private String genererRecommandations(List<String> questions, List<String> answers, String maladie) {
        StringBuilder reco = new StringBuilder();
        reco.append("\n\nAvis de l'IA et recommandations :\n");
        if (maladie.toLowerCase().contains("neuropath")) {
            boolean douleur = false, faiblesse = false, allodynie = false, diabete = false;
            for (int i = 0; i < questions.size(); i++) {
                String q = questions.get(i).toLowerCase();
                String a = answers.get(i).toLowerCase();
                if (q.contains("douleur") && (a.contains("oui") || a.contains("brûlure") || a.contains("élancements"))) douleur = true;
                if (q.contains("faiblesse") && a.contains("oui")) faiblesse = true;
                if (q.contains("allodynie") && a.contains("oui")) allodynie = true;
                if (q.contains("diabète") && a.contains("oui")) diabete = true;
            }
            if (douleur) reco.append("- Présence de douleurs neuropathiques : un avis médical est conseillé.\n");
            if (faiblesse) reco.append("- Faiblesse musculaire : surveillez l'évolution, consultez si aggravation.\n");
            if (allodynie) reco.append("- Allodynie : ce symptôme est typique d'une neuropathie, parlez-en à votre médecin.\n");
            if (diabete) reco.append("- Diabète : un bon contrôle glycémique est essentiel pour limiter l'évolution de la neuropathie.\n");
            reco.append("- Pensez à protéger vos pieds/mains et à surveiller l'apparition de plaies.\n");
        } else if (maladie.toLowerCase().contains("sla")) {
            boolean faiblesse = false, fonte = false, troublesBulbaires = false;
            for (int i = 0; i < questions.size(); i++) {
                String q = questions.get(i).toLowerCase();
                String a = answers.get(i).toLowerCase();
                if (q.contains("faiblesse") && a.contains("oui")) faiblesse = true;
                if (q.contains("fonte") && a.contains("oui")) fonte = true;
                if ((q.contains("articuler") || q.contains("voix") || q.contains("avaler")) && a.contains("oui")) troublesBulbaires = true;
            }
            if (faiblesse) reco.append("- Faiblesse musculaire progressive : consultez rapidement un neurologue.\n");
            if (fonte) reco.append("- Fonte musculaire : ce signe nécessite un avis médical spécialisé.\n");
            if (troublesBulbaires) reco.append("- Troubles de la parole ou de la déglutition : signalez-les à votre médecin.\n");
            reco.append("- Un électromyogramme (EMG) peut être utile pour préciser le diagnostic.\n");
        }
        reco.append("\n⚠️ Ce rapport ne remplace pas un avis médical. Consultez un professionnel de santé pour interpréter ces résultats.\n");
        return reco.toString();
    }

    private String genererAvisMedecinAuto(String maladie) {
        return "\uD83E\uDDE0 Avis du médecin (généré automatiquement)\n\n" +
                "En se basant sur les réponses fournies, ce chatbot médical estime qu'il existe :\n\n" +
                "\uD83D\uDD39 Soit une suspicion de neuropathie périphérique\n" +
                "\uD83D\uDD39 Soit des signes pouvant évoquer une atteinte motoneuronale comme la SLA\n" +
                "\uD83D\uDD39 Soit aucune alerte majeure à ce stade\n\n" +
                "▶️ Ce résultat ne constitue pas un diagnostic médical. Il s'agit uniquement d'une aide à la détection précoce.\n\n" +
                "\uD83D\uDCCC Il est recommandé de :\n" +
                "- Réaliser un examen clinique approfondi\n" +
                "- Prescrire un électromyogramme (EMG) si nécessaire\n" +
                "- Rechercher des antécédents (diabète, maladies neurologiques, carences…)\n" +
                "- Orienter vers un spécialiste en neurologie en cas de doute\n\n" +
                "\uD83D\uDD8B️ Signé par :\nDr. [ ]\nSpécialiste en neurologie / médecin interne\n";
    }

    private String genererRapportComplet(List<String> questions, List<EditText> answerFields, String maladie) {
        StringBuilder rapport = new StringBuilder();
        rapport.append("Rapport médical\n\n");
        rapport.append("Maladie : ").append(maladie).append("\n\n");
        List<String> answers = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            String rep = (i < answerFields.size()) ? answerFields.get(i).getText().toString() : "";
            answers.add(rep);
        }
        // Tableau
        rapport.append("| Question | Réponse | Recommandation |\n");
        rapport.append("|----------|---------|----------------|\n");
        List<String> recos = genererRecommandationsParQuestion(questions, answers, maladie);
        for (int i = 0; i < questions.size(); i++) {
            rapport.append("- ").append(questions.get(i).replace("|", "/")).append(" | ")
                .append(answers.get(i).replace("|", "/")).append(" | ")
                .append(recos.get(i).replace("|", "/")).append(" |\n");
        }
        // Synthèse IA
        rapport.append("\nAvis de l'IA :\n");
        rapport.append(genererRecommandations(questions, answers, maladie));
        // Champ avis médecin
        rapport.append("\n").append(genererAvisMedecinAuto(maladie)).append("\n");
        return rapport.toString();
    }

    private void saveQuestionnaireToHistory(List<String> questions, List<String> answers, String maladie) {
        SharedPreferences prefs = requireContext().getSharedPreferences("questionnaire_history", Context.MODE_PRIVATE);
        String historyJson = prefs.getString("history_json", "[]");
        try {
            JSONArray historyArray = new JSONArray(historyJson);
            JSONObject newEntry = new JSONObject();
            
            // Créer un résumé du questionnaire
            StringBuilder resumeBuilder = new StringBuilder();
            for (int i = 0; i < questions.size() && i < 2; i++) { // Résumé des 2 premières questions
                resumeBuilder.append(questions.get(i)).append(": ").append(answers.get(i)).append("\n");
            }

            newEntry.put("patientId", "Utilisateur"); // À remplacer par un vrai ID si disponible
            newEntry.put("dateHeure", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(new Date()));
            newEntry.put("typeEchange", "Questionnaire " + maladie);
            newEntry.put("resume", resumeBuilder.toString().trim());
            newEntry.put("analyseIA", genererRecommandations(questions, answers, maladie));
            newEntry.put("lienRapport", "N/A"); // Le rapport PDF est généré à la volée

            // Stocker les questions et réponses complètes
            JSONArray qaArray = new JSONArray();
            for (int i = 0; i < questions.size(); i++) {
                JSONObject qaPair = new JSONObject();
                qaPair.put("question", questions.get(i));
                qaPair.put("answer", answers.get(i));
                qaArray.put(qaPair);
            }
            newEntry.put("conversationComplete", qaArray.toString());

            historyArray.put(newEntry);
            prefs.edit().putString("history_json", historyArray.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Erreur lors de la sauvegarde de l'historique.", Toast.LENGTH_SHORT).show();
        }
    }
} 