package com.dianerverotect.chatbot;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the ChatMessage class
 */
public class ChatMessageTest {

    private static final String TEST_MESSAGE = "Test message";
    private static final int TEST_IMAGE_ID = 123;

    @Test
    public void testUserMessageConstructor() {
        // Create a user message
        ChatMessage userMessage = new ChatMessage(TEST_MESSAGE, true);
        
        // Test that the message text is set correctly
        assertEquals("Message text should match the constructor parameter", 
                TEST_MESSAGE, userMessage.getMessage());
        
        // Test that isUser returns true
        assertTrue("isUser should return true for a user message", userMessage.isUser());
        
        // Test that the image resource ID is 0 for a user message
        assertEquals("Image resource ID should be 0 for a user message", 
                0, userMessage.getImageResourceId());
    }
    
    @Test
    public void testBotMessageConstructor() {
        // Create a bot message without an image
        ChatMessage botMessage = new ChatMessage(TEST_MESSAGE, false);
        
        // Test that the message text is set correctly
        assertEquals("Message text should match the constructor parameter", 
                TEST_MESSAGE, botMessage.getMessage());
        
        // Test that isUser returns false
        assertFalse("isUser should return false for a bot message", botMessage.isUser());
        
        // Test that the image resource ID is 0 for a bot message without an image
        assertEquals("Image resource ID should be 0 for a bot message without an image", 
                0, botMessage.getImageResourceId());
    }
    
    @Test
    public void testBotMessageWithImageConstructor() {
        // Create a bot message with an image
        ChatMessage botMessageWithImage = new ChatMessage(TEST_MESSAGE, TEST_IMAGE_ID);
        
        // Test that the message text is set correctly
        assertEquals("Message text should match the constructor parameter", 
                TEST_MESSAGE, botMessageWithImage.getMessage());
        
        // Test that isUser returns false
        assertFalse("isUser should return false for a bot message with an image", 
                botMessageWithImage.isUser());
        
        // Test that the image resource ID is set correctly
        assertEquals("Image resource ID should match the constructor parameter", 
                TEST_IMAGE_ID, botMessageWithImage.getImageResourceId());
    }
    
    @Test
    public void testSetImageResourceId() {
        // Create a bot message without an image
        ChatMessage botMessage = new ChatMessage(TEST_MESSAGE, false);
        
        // Set the image resource ID
        botMessage.setImageResourceId(TEST_IMAGE_ID);
        
        // Test that the image resource ID is set correctly
        assertEquals("Image resource ID should match the value set", 
                TEST_IMAGE_ID, botMessage.getImageResourceId());
    }
}
