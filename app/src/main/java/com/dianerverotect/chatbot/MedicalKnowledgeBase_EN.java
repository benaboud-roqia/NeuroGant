package com.dianerverotect.chatbot;

import java.util.HashMap;
import java.util.Map;

/**
 * Medical knowledge base for the chatbot (English)
 */
public class MedicalKnowledgeBase_EN {
    private static final Map<String, String> knowledgeBase = new HashMap<>();
    
    static {
        // Neuropathy
        knowledgeBase.put("neuropathy", "Neuropathy is a condition affecting peripheral nerves, which can cause pain, tingling, numbness, and muscle weakness. Common causes include diabetes, alcoholism, vitamin deficiencies, and certain infections.");
        knowledgeBase.put("diabetic neuropathy", "Diabetic neuropathy is a complication of diabetes affecting the nerves. It can cause pain in the feet and hands, digestive issues, and changes in heart rate. Good blood sugar control is essential.");
        knowledgeBase.put("peripheral neuropathy", "Peripheral neuropathy affects the nerves that transmit signals between the central nervous system and the rest of the body. Symptoms include pain, tingling, and a loss of sensation.");

        // ALS
        knowledgeBase.put("als", "Amyotrophic lateral sclerosis (ALS) is a progressive neurological disease that affects motor neurons. It leads to progressive muscle weakness, atrophy, and eventually paralysis. Early diagnosis and treatment are crucial.");
        knowledgeBase.put("amyotrophic lateral sclerosis", "Amyotrophic lateral sclerosis (ALS) is a degenerative disease affecting nerve cells in the brain and spinal cord. It progresses rapidly and impacts the ability to move, speak, eat, and breathe.");
        knowledgeBase.put("lou gehrig's disease", "Lou Gehrig's disease is another name for amyotrophic lateral sclerosis (ALS), named after the American baseball player who was diagnosed with it.");

        // Symptoms
        knowledgeBase.put("neuropathy symptoms", "Symptoms of neuropathy include: sharp or shooting pains, tingling and numbness, sensitivity to touch, muscle weakness, loss of coordination, digestive problems, and changes in blood pressure.");
        knowledgeBase.put("als symptoms", "Symptoms of ALS include: progressive muscle weakness, muscle atrophy, cramps and fasciculations (twitches), difficulty speaking and swallowing, respiratory problems, and in some cases, cognitive changes.");
        knowledgeBase.put("nerve pain", "Nerve pain (neuralgia) can be described as a burning, tingling, electric shock-like, or stabbing sensation. It can be constant or intermittent and may worsen at night.");

        // Diagnosis
        knowledgeBase.put("neuropathy diagnosis", "Diagnosing neuropathy involves a physical exam, neurological tests, blood tests, nerve conduction studies, and sometimes a nerve biopsy. A complete medical history is essential.");
        knowledgeBase.put("als diagnosis", "Diagnosing ALS is complex and requires ruling out other conditions. It includes neurological exams, electromyography (EMG) tests, MRIs, and blood tests. A definitive diagnosis can take time.");

        // Treatments
        knowledgeBase.put("neuropathy treatment", "Treatment for neuropathy depends on the underlying cause. It may include pain medications, vitamin supplements, physical therapy, lifestyle changes, and sometimes surgery.");
        knowledgeBase.put("als treatment", "ALS treatment is primarily symptomatic and aims to improve quality of life. It includes medications to slow progression, physical therapy, speech therapy, nutritional support, and respiratory support.");
    }
    
    /**
     * Searches the knowledge base for a response.
     * @param query The user's query.
     * @return The found response or null.
     */
    public static String searchKnowledge(String query) {
        String lowerQuery = query.toLowerCase().trim();
        
        String[] keywords = {
            "diabetic neuropathy", "amyotrophic lateral sclerosis", "lou gehrig's disease",
            "peripheral neuropathy", "neuropathy", "als"
        };
        for (String keyword : keywords) {
            if (lowerQuery.contains(keyword)) {
                // Ensure the key exists before trying to access it
                if (knowledgeBase.containsKey(keyword)) {
                    return knowledgeBase.get(keyword);
                }
            }
        }
        
        return null;
    }
} 