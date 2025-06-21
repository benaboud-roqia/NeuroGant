# NerveBot Setup Guide

This document provides instructions for setting up the Dialogflow integration for NerveBot, the medical assistant chatbot in DiaNerveProtect.

## Dialogflow Setup

1. **Create a Dialogflow Project**:
   - Go to [Dialogflow Console](https://dialogflow.cloud.google.com/)
   - Click "Create Agent" and name it "DiaNerveProtect-Chatbot"
   - Select your Google Cloud project or create a new one

2. **Create Intents**:
   - Create intents for common medical queries about ALS and neuropathy
   - Examples:
     - `als_info`: What is ALS?
     - `als_symptoms`: What are the symptoms of ALS?
     - `als_treatment`: How is ALS treated?
     - `neuropathy_info`: What is neuropathy?
     - `neuropathy_symptoms`: What are the symptoms of neuropathy?
     - `neuropathy_treatment`: How is neuropathy treated?
     - `emg_info`: What is an EMG test?

3. **Train the Agent**:
   - Add training phrases for each intent
   - Create responses with accurate medical information
   - Use contexts to create conversational flows

4. **Get Service Account Credentials**:
   - In Google Cloud Console, go to IAM & Admin > Service Accounts
   - Create a new service account with Dialogflow API Client role
   - Create a key for this service account (JSON format)
   - Replace the placeholder in `assets/dialogflow-credentials.json` with your actual credentials

## TensorFlow Lite Models

The app uses two TensorFlow Lite models:
- `alsnet3.tflite`: For ALS-related queries
- `neuropathy_model.tflite`: For neuropathy-related queries

These models are already included in the assets folder and don't require additional setup.

## Testing the Chatbot

1. Build and run the app
2. Navigate to the NerveBot tab in the bottom navigation
3. Test with various medical queries about ALS and neuropathy
4. Verify that responses are medically accurate and helpful

## Troubleshooting

- If you see "Dialogflow client not initialized" errors, check your credentials file
- If language identification fails, ensure the ML Kit dependencies are correctly included
- For model loading issues, verify that the TensorFlow Lite models are in the assets folder

## Security Note

Never commit real service account credentials to version control. The included credentials file is a placeholder and must be replaced with your actual credentials before deployment.
