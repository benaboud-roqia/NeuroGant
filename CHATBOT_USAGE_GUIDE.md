Here’s your updated `README.md` with a **new section** at the end titled:

> **✅ Questions Implemented in the Knowledge Base**

This shows **who added the questions**, what type they are, and aligns with your original content and structure:

---

# NerveBot Usage and Testing Guide

## Overview

NerveBot is a medical chatbot integrated into the NerveProtect app that provides information about diabetic neuropathy, ALS, and EMG tests. This guide explains how to use and test the chatbot.

## Using the Chatbot

### Basic Usage

1. Launch the DiaNerveProtect app
2. Tap on the "NerveBot" tab in the bottom navigation
3. Type your medical question in the text input field
4. Tap the send button to submit your question
5. The chatbot will respond with relevant medical information

### Example Questions

**ALS-related:**

* What is ALS?
* What are the symptoms of ALS?
* How is ALS diagnosed?
* What treatments are available for ALS?
* Is ALS genetic?

**Neuropathy-related:**

* What is diabetic neuropathy?
* What causes peripheral neuropathy?
* What are the symptoms of neuropathy?
* How is neuropathy diagnosed?
* Can neuropathy be reversed?

**EMG-related:**

* What is an EMG test?
* How is an EMG performed?
* Is an EMG test painful?
* What conditions can an EMG diagnose?
* How should I prepare for an EMG?

---

## How It Works

The chatbot uses a two-tier approach to generate responses:

1. **Primary: Google Dialogflow**

    * Handles natural language understanding
    * Processes conversational context
    * Provides rich, detailed responses

2. **Fallback: TensorFlow Lite Models**

    * Two specialized AI models:

        * `alsnet3.tflite` for ALS-related queries
        * `neuropathy_model.tflite` for neuropathy-related queries
    * Provides responses based on a medical knowledge base
    * Used when Dialogflow is unavailable or fails

3. **Language Identification**

    * Detects the language of user input
    * Currently optimized for English

---

## Testing the Chatbot

### Manual Testing

* Ask questions about ALS and diabetic neuropathy
* Try rephrasing and shortened forms
* Disconnect the internet to test fallback model behavior

### Automated Testing

Test classes included:

* `MedicalAIModelTest`
* `DialogflowServiceTest`
* `ChatAdapterTest`
* `ChatMessageTest`
* `ChatbotFragmentTest`

To run:

1. Open project in Android Studio
2. Right-click the `test/` folder → **Run tests**

---

## Troubleshooting

**Dialogflow Not Responding**

* Check internet and credentials
* Inspect Logcat with tag `ChatbotFragment`

**Model Loading Fails**

* Ensure `.tflite` models are in `assets/`
* Check for missing permissions or asset loading issues

**UI Issues**

* Verify RecyclerView adapter
* Check send button listener

---

## Extending the Chatbot

**Dialogflow:**

* Add intents via the Dialogflow console
* Train with new examples, test live
* Deploy updates automatically

**AI Models:**

* Replace `.tflite` files with new trained models
* Update I/O tensor handling in `MedicalAIModel` if needed

**Feature Ideas:**

* Voice assistant
* Symptom photo analysis
* User profile–based personalization
* French/Arabic multilingual support

---

## Security and Privacy

* On-device TFLite processing (private)
* Dialogflow uses Google Cloud securely
* No personal health info stored locally
* You can extend with biometric login for privacy

---

## ✅ Questions Implemented in the Knowledge Base

This chatbot includes **200+ curated questions and answers** added by the **project author (you)** across these domains:

| Category              | Topics Covered                                                                  |
| --------------------- | ------------------------------------------------------------------------------- |
| **ALS**               | Definition, symptoms, diagnosis, prognosis, treatment, muscle weakness, fatigue |
| **Neuropathy**        | Causes, symptoms, prevention, sleep, walking, pain, autonomic impact            |
| **EMG**               | Testing procedure, pain, differences with MRI, usage in ALS and neuropathy      |
| **Daily Scenarios**   | Burning feet, numbness, cramping, shortness of breath, foot drop                |
| **User Monitoring**   | What to check daily, how to rest, report symptoms, foot care routines           |
| **Alerts & Triggers** | Low glucose after walking, stress worsening symptoms, exercise limits           |
| **Lifestyle & Diet**  | Foods for nerve health, sleep tips, vitamin support, diabetic care              |

> 🛠 These questions were manually researched, categorized, and formatted in the Java code base using:

