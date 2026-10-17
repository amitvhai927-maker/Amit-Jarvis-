# AMIT JARVIS — Complete Starter Bundle

This bundle contains an Android app starter plus a separate Node.js AI backend.

## Features included in this starter
- Dark navy and electric-blue JARVIS-inspired interface branded AMIT
- Speech input through Android SpeechRecognizer (tap microphone; not always listening)
- Spoken replies through Android TextToSpeech
- Typed chat
- Basic local commands: open YouTube, open Google, web search, open phone dialer, tell time, local reminder list
- AI chat through a configurable backend URL
- Separate Node.js backend that keeps provider API keys off the APK
- GitHub Actions workflow to build a debug APK

## Important limitations
This is a starter project, not a fully production-ready assistant. It does **not** yet implement a working always-on “Hey Amit” wake word, reliable background microphone listening, scheduled notification reminders, screen reading, automatic WhatsApp sending, or arbitrary control of every phone feature. Android restricts background microphone use and privileged device actions. Those need separate, permission-based implementations and device testing.

## Build APK with GitHub Actions
1. Download and extract this ZIP.
2. Create a GitHub repository and upload all files, including hidden `.github` folder.
3. Open Actions and run “Build AMIT JARVIS APK”, or push to `main`.
4. Download the `amit-jarvis-debug-apk` artifact from the successful workflow run.
5. Install the APK on your Android device and grant microphone permission.

The workflow installs Gradle 8.9 and builds the debug APK.

## Connect AI
1. Deploy the `backend` folder to a Node.js hosting service.
2. Configure server environment variables using `backend/.env.example` as a guide.
3. Use a provider API key with an available free tier if eligible. Check current pricing, region availability, model names, and quotas on the provider's official dashboard.
4. Paste your deployed HTTPS endpoint ending in `/api/chat` into the app's AI Backend URL field.
5. Never put your private API key in Android code or a public GitHub repository.

## Test locally
Open the project in Android Studio with JDK 17 and Android SDK 35. Let Gradle sync, connect your phone with USB debugging or use an emulator, then Run.
