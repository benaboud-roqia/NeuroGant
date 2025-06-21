package com.dianerverotect.chatbot;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the DialogflowService class
 */
@RunWith(MockitoJUnitRunner.class)
public class DialogflowServiceTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private DialogflowService.DialogflowCallback mockCallback;
    
    private DialogflowService dialogflowService;
    
    // Sample credentials JSON with API key format
    private static final String SAMPLE_CREDENTIALS = "{\"project_id\":\"test-project\",\"api_key\":\"test-api-key\"}";
    
    @Before
    public void setUp() throws Exception {
        // Mock the assets input stream
        InputStream mockStream = new ByteArrayInputStream(SAMPLE_CREDENTIALS.getBytes());
        when(mockContext.getAssets().open("dialogflow-credentials.json")).thenReturn(mockStream);
        
        // Create the service
        dialogflowService = new DialogflowService(mockContext);
    }
    
    @Test
    public void testInitialize() {
        // Initialize the service
        dialogflowService.initialize();
        
        // Verify that the service was initialized (indirectly by checking it doesn't throw exceptions)
        assertNotNull(dialogflowService);
    }
    
    @Test
    public void testSendQuery_serviceNotInitialized() {
        // Send a query without initializing
        dialogflowService.sendQuery("What is ALS?", mockCallback);
        
        // Verify that the error callback was called
        verify(mockCallback).onError("Dialogflow service not initialized");
    }
    
    @Test
    public void testClose() {
        // Initialize and then close
        dialogflowService.initialize();
        dialogflowService.close();
        
        // Send a query after closing
        dialogflowService.sendQuery("What is ALS?", mockCallback);
        
        // Verify that the error callback was called
        verify(mockCallback).onError("Dialogflow service not initialized");
        try {
            dialogflowService.close();
        } catch (Exception e) {
            // If an exception is thrown, the test will fail
            throw new AssertionError("close() threw an exception: " + e.getMessage(), e);
        }
    }
}
