package com.dianerverotect.chatbot;

import java.util.Date;

/**
 * Data model for a chat message
 */
public class ChatMessage {
    private final String message;
    private final boolean isUser;
    private final int imageResourceId;
    private final long timestamp;
    private boolean isTyping;
    private boolean isTypingComplete;

    // Constructor for user messages
    public ChatMessage(String message, boolean isUser) {
        this(message, isUser, 0, !isUser); // Typing effect is enabled for bot messages
    }
    
    // Constructor for bot messages with typing effect toggle
    public ChatMessage(String message, boolean isUser, boolean enableTypingEffect) {
        this(message, isUser, 0, enableTypingEffect);
    }

    // Master constructor
    public ChatMessage(String message, boolean isUser, int imageResourceId, boolean enableTypingEffect) {
        this.message = message;
        this.isUser = isUser;
        this.imageResourceId = imageResourceId;
        this.isTyping = enableTypingEffect;
        this.timestamp = new Date().getTime();
        this.isTypingComplete = !enableTypingEffect;
    }

    public String getMessage() { return message; }
    public boolean isUser() { return isUser; }
    public long getTimestamp() { return timestamp; }
    public int getImageResourceId() { return imageResourceId; }
    public boolean isTyping() { return isTyping; }
    public void setTyping(boolean typing) { isTyping = typing; }
    public boolean isTypingComplete() { return isTypingComplete; }
    public void setTypingComplete(boolean typingComplete) { isTypingComplete = typingComplete; }
}
