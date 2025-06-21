package com.dianerverotect.chatbot;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the MedicalAIModel class
 */
@RunWith(MockitoJUnitRunner.class)
public class MedicalAIModelTest {

    @Mock
    private Context mockContext;

    private MedicalAIModel medicalAIModel;

    @Before
    public void setUp() {
        // Create a new instance of MedicalAIModel with the mock context
        medicalAIModel = new MedicalAIModel(mockContext);
    }

    @Test
    public void testGetResponseForALS() {
        // Test ALS-related queries
        String response = medicalAIModel.getResponse("What is ALS?", MedicalAIModel.MODEL_ALS);
        assertNotNull("Response should not be null", response);
        assertFalse("Response should not be empty", response.isEmpty());
        
        response = medicalAIModel.getResponse("What are the symptoms of ALS?", MedicalAIModel.MODEL_ALS);
        assertNotNull("Response should not be null", response);
        assertFalse("Response should not be empty", response.isEmpty());
        
        response = medicalAIModel.getResponse("How is ALS diagnosed?", MedicalAIModel.MODEL_ALS);
        assertNotNull("Response should not be null", response);
        assertFalse("Response should not be empty", response.isEmpty());
    }

    @Test
    public void testGetResponseForNeuropathy() {
        // Test neuropathy-related queries
        String response = medicalAIModel.getResponse("What is neuropathy?", MedicalAIModel.MODEL_NEUROPATHY);
        assertNotNull("Response should not be null", response);
        assertFalse("Response should not be empty", response.isEmpty());
        
        response = medicalAIModel.getResponse("What are the symptoms of diabetic neuropathy?", MedicalAIModel.MODEL_NEUROPATHY);
        assertNotNull("Response should not be null", response);
        assertFalse("Response should not be empty", response.isEmpty());
        
        response = medicalAIModel.getResponse("How is neuropathy treated?", MedicalAIModel.MODEL_NEUROPATHY);
        assertNotNull("Response should not be null", response);
        assertFalse("Response should not be empty", response.isEmpty());
    }

    @Test
    public void testGetResponseForEMG() {
        // Test EMG-related queries
        String response = medicalAIModel.getResponse("What is an EMG test?", MedicalAIModel.MODEL_ALS);
        assertNotNull("Response should not be null", response);
        assertFalse("Response should not be empty", response.isEmpty());
        
        response = medicalAIModel.getResponse("How is an EMG performed?", MedicalAIModel.MODEL_NEUROPATHY);
        assertNotNull("Response should not be null", response);
        assertFalse("Response should not be empty", response.isEmpty());
    }
}
