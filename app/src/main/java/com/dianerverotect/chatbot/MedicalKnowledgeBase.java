package com.dianerverotect.chatbot;

import java.util.HashMap;
import java.util.Map;

/**
 * Base de connaissances médicale pour le chatbot
 */
public class MedicalKnowledgeBase {
    private static final Map<String, String> knowledgeBase = new HashMap<>();
    
    static {
        // Neuropathie
        knowledgeBase.put("neuropathie", "La neuropathie est une affection des nerfs périphériques qui peut causer des douleurs, des picotements, des engourdissements et une faiblesse musculaire. Les causes courantes incluent le diabète, l'alcoolisme, les carences vitaminiques et certaines infections.");
        
        knowledgeBase.put("neuropathie diabétique", "La neuropathie diabétique est une complication du diabète qui affecte les nerfs. Elle peut causer des douleurs dans les pieds et les mains, des problèmes digestifs et des changements dans la fréquence cardiaque. Un bon contrôle de la glycémie est essentiel.");
        
        knowledgeBase.put("neuropathie périphérique", "La neuropathie périphérique affecte les nerfs qui transmettent les signaux entre le système nerveux central et le reste du corps. Les symptômes incluent des douleurs, des picotements et une perte de sensation.");
        
        // ALS
        knowledgeBase.put("als", "La sclérose latérale amyotrophique (ALS) est une maladie neurologique progressive qui affecte les motoneurones. Elle cause une faiblesse musculaire progressive, une atrophie et finalement une paralysie. Le diagnostic et le traitement précoces sont cruciaux.");
        
        knowledgeBase.put("sclérose latérale amyotrophique", "La sclérose latérale amyotrophique (ALS) est une maladie dégénérative qui affecte les cellules nerveuses du cerveau et de la moelle épinière. Elle progresse rapidement et affecte la capacité de bouger, parler, manger et respirer.");
        
        knowledgeBase.put("maladie de lou gehrig", "La maladie de Lou Gehrig est un autre nom pour la sclérose latérale amyotrophique (ALS). Elle a été nommée d'après le joueur de baseball américain qui en était atteint.");
        
        // Symptômes
        knowledgeBase.put("symptômes neuropathie", "Les symptômes de la neuropathie incluent : douleurs aiguës ou lancinantes, picotements et engourdissements, sensibilité au toucher, faiblesse musculaire, perte de coordination, problèmes digestifs et changements dans la tension artérielle.");
        
        knowledgeBase.put("symptômes als", "Les symptômes de l'ALS incluent : faiblesse musculaire progressive, atrophie musculaire, crampes et fasciculations, difficultés à parler et à avaler, problèmes respiratoires et changements cognitifs dans certains cas.");
        
        knowledgeBase.put("douleur nerveuse", "La douleur nerveuse (névralgie) peut être décrite comme une sensation de brûlure, de picotement, de choc électrique ou de douleur lancinante. Elle peut être constante ou intermittente et s'aggraver la nuit.");
        
        // Diagnostic
        knowledgeBase.put("diagnostic neuropathie", "Le diagnostic de la neuropathie implique un examen physique, des tests neurologiques, des analyses sanguines, des études de conduction nerveuse et parfois une biopsie nerveuse. Un historique médical complet est essentiel.");
        
        knowledgeBase.put("diagnostic als", "Le diagnostic de l'ALS est complexe et nécessite l'exclusion d'autres conditions. Il inclut des examens neurologiques, des tests électromyographiques, des IRM et des analyses sanguines. Un diagnostic définitif peut prendre du temps.");
        
        // Traitements
        knowledgeBase.put("traitement neuropathie", "Le traitement de la neuropathie dépend de la cause sous-jacente. Il peut inclure des médicaments pour la douleur, des suppléments vitaminiques, la physiothérapie, des changements de mode de vie et parfois la chirurgie.");
        
        knowledgeBase.put("traitement als", "Le traitement de l'ALS est principalement symptomatique et vise à améliorer la qualité de vie. Il inclut des médicaments pour ralentir la progression, la physiothérapie, l'orthophonie, la nutrition et le support respiratoire.");
        
        knowledgeBase.put("médicaments neuropathie", "Les médicaments pour la neuropathie incluent : antidépresseurs tricycliques, anticonvulsivants, opioïdes, crèmes topiques et suppléments vitaminiques (B12, acide alpha-lipoïque).");
        
        // Prévention
        knowledgeBase.put("prévention neuropathie", "La prévention de la neuropathie inclut : un bon contrôle du diabète, une alimentation équilibrée, l'évitement de l'alcool excessif, l'exercice régulier et la protection contre les toxines.");
        
        knowledgeBase.put("prévention als", "La prévention de l'ALS est difficile car les causes exactes ne sont pas connues. Cependant, éviter l'exposition aux toxines, maintenir une alimentation saine et faire de l'exercice régulièrement peuvent aider.");
        
        // Mode de vie
        knowledgeBase.put("exercice neuropathie", "L'exercice peut aider à améliorer la circulation, renforcer les muscles et réduire la douleur dans la neuropathie. Les exercices à faible impact comme la marche, la natation et le yoga sont recommandés.");
        
        knowledgeBase.put("alimentation neuropathie", "Une alimentation riche en vitamines B, en antioxydants et en acides gras oméga-3 peut aider à soutenir la santé nerveuse. Éviter l'alcool excessif et maintenir un poids santé sont importants.");
        
        // Urgences
        knowledgeBase.put("urgence neuropathie", "Consultez immédiatement un médecin si vous ressentez : une douleur soudaine et intense, une faiblesse musculaire soudaine, des problèmes respiratoires ou des changements dans la conscience.");
        
        knowledgeBase.put("urgence als", "Consultez immédiatement un médecin si vous ressentez : des difficultés respiratoires soudaines, des problèmes de déglutition sévères, une faiblesse musculaire rapide ou des changements cognitifs soudains.");
        
        knowledgeBase.put("neuropathy", "La neuropathie est une affection des nerfs périphériques qui peut causer des douleurs, des picotements, des engourdissements et une faiblesse musculaire. Les causes courantes incluent le diabète, l'alcoolisme, les carences vitaminiques et certaines infections.");
    }
    
    /**
     * Recherche une réponse dans la base de connaissances
     * @param query La question de l'utilisateur
     * @return La réponse trouvée ou null
     */
    public static String searchKnowledge(String query) {
        String lowerQuery = query.toLowerCase().trim();
        
        // Nettoyer la requête utilisateur pour une meilleure correspondance
        String cleanedQuery = lowerQuery.replace("diabitique", "diabétique");

        // 1. Recherche de correspondance exacte (la plus fiable)
        if (knowledgeBase.containsKey(cleanedQuery)) {
            return knowledgeBase.get(cleanedQuery);
        }

        // 2. Recherche par mots-clés principaux (du plus long au plus court)
        String[] keywords = {
            "neuropathie diabétique", "sclérose latérale amyotrophique", 
            "neuropathie périphérique", "maladie de lou gehrig",
            "neuropathie", "als"
        };
        for (String keyword : keywords) {
            if (cleanedQuery.contains(keyword)) {
                return knowledgeBase.get(keyword);
            }
        }
        
        return null; // Aucune correspondance trouvée
    }
    
    /**
     * Ajoute une nouvelle entrée à la base de connaissances
     * @param keyword Le mot-clé
     * @param response La réponse
     */
    public static void addKnowledge(String keyword, String response) {
        knowledgeBase.put(keyword.toLowerCase(), response);
    }
    
    /**
     * Obtient le nombre d'entrées dans la base de connaissances
     * @return Le nombre d'entrées
     */
    public static int getKnowledgeBaseSize() {
        return knowledgeBase.size();
    }
} 