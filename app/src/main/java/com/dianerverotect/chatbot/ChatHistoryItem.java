package com.dianerverotect.chatbot;

public class ChatHistoryItem {
    private String message;
    private boolean isBot;
    private String patientId;
    private String dateHeure;
    private String typeEchange;
    private String resume;
    private String analyseIA;
    private String lienRapport;
    private String conversationComplete;

    public ChatHistoryItem(String message, boolean isBot) {
        this.message = message;
        this.isBot = isBot;
    }

    public ChatHistoryItem(String patientId, String dateHeure, String typeEchange, String resume, String analyseIA, String lienRapport, String conversationComplete) {
        this.patientId = patientId;
        this.dateHeure = dateHeure;
        this.typeEchange = typeEchange;
        this.resume = resume;
        this.analyseIA = analyseIA;
        this.lienRapport = lienRapport;
        this.conversationComplete = conversationComplete;
    }

    public String getMessage() {
        return message;
    }

    public boolean isBot() {
        return isBot;
    }

    public String getPatientId() { return patientId; }
    public String getDateHeure() { return dateHeure; }
    public String getTypeEchange() { return typeEchange; }
    public String getResume() { return resume; }
    public String getAnalyseIA() { return analyseIA; }
    public String getLienRapport() { return lienRapport; }
    public String getConversationComplete() { return conversationComplete; }
} 