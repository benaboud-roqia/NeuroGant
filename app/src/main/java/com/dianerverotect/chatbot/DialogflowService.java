package com.dianerverotect.chatbot;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Service class for interacting with Dialogflow using direct HTTP requests
 * This implementation avoids using the Dialogflow client libraries
 */
public class DialogflowService {
    private static final String TAG = "DialogflowService";
    
    // Dialogflow API endpoint
    private static final String DIALOGFLOW_API_ENDPOINT = "https://dialogflow.googleapis.com/v2/projects/%s/agent/sessions/%s:detectIntent";
    
    // Dialogflow project ID
    private static final String PROJECT_ID = "dianerveprotect-chatbot";
    
    // Session ID for the current conversation
    private String sessionId;
    
    // API key (will be loaded from credentials file)
    private String apiKey;
    
    // Context
    private final Context context;
    
    // Callback interface
    public interface DialogflowCallback {
        void onResult(String response);
        void onError(String error);
    }
    
    /**
     * Constructor
     * @param context Application context
     */
    public DialogflowService(Context context) {
        this.context = context;
        this.sessionId = UUID.randomUUID().toString();
    }
    
    /**
     * Initialize the Dialogflow service
     */
    public void initialize() {
        try {
            // Load credentials from JSON file
            InputStream stream = context.getAssets().open("dialogflow-credentials.json");
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            reader.close();
            stream.close();
            
            // Parse JSON to get API key
            JSONObject jsonObject = new JSONObject(stringBuilder.toString());
            apiKey = jsonObject.getString("api_key"); // Assuming the JSON has an api_key field
            
            Log.d(TAG, "Dialogflow service initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Dialogflow service: " + e.getMessage());
        }
    }
    
    /**
     * Send a text query to Dialogflow
     * @param query Text query
     * @param callback Callback for handling the response
     */
    public void sendQuery(String query, DialogflowCallback callback) {
        if (apiKey == null) {
            callback.onError("Dialogflow service not initialized");
            return;
        }
        
        // Run the query in a background thread
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                HttpURLConnection connection = null;
                try {
                    // Create the API URL
                    String apiUrl = String.format(DIALOGFLOW_API_ENDPOINT, PROJECT_ID, sessionId);
                    URL url = new URL(apiUrl);
                    
                    // Open connection
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                    connection.setDoOutput(true);
                    
                    // Create request JSON
                    JSONObject requestBody = new JSONObject();
                    JSONObject queryInput = new JSONObject();
                    JSONObject textInput = new JSONObject();
                    textInput.put("text", query);
                    textInput.put("languageCode", "en-US");
                    queryInput.put("text", textInput);
                    requestBody.put("queryInput", queryInput);
                    
                    // Write request body
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                    
                    // Get response
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Read response
                        try (BufferedReader br = new BufferedReader(
                                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                            StringBuilder response = new StringBuilder();
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                            return response.toString();
                        }
                    } else {
                        Log.e(TAG, "HTTP error code: " + responseCode);
                        return null;
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error sending query to Dialogflow: " + e.getMessage());
                    return null;
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
            
            @Override
            protected void onPostExecute(String response) {
                if (response != null) {
                    try {
                        // Parse the response JSON
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONObject queryResult = jsonResponse.getJSONObject("queryResult");
                        String fulfillmentText = queryResult.getString("fulfillmentText");
                        
                        if (!fulfillmentText.isEmpty()) {
                            callback.onResult(fulfillmentText);
                        } else {
                            callback.onError("No response from Dialogflow");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing Dialogflow response: " + e.getMessage());
                        callback.onError("Error parsing Dialogflow response");
                    }
                } else {
                    callback.onError("Error getting response from Dialogflow");
                }
            }
        }.execute();
    }
    
    /**
     * Close the Dialogflow service
     */
    public void close() {
        // Nothing to close in this implementation
        apiKey = null;
    }
}
