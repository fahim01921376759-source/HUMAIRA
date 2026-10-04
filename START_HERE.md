# HUMAIRA - Start Here

HUMAIRA is a personal AI assistant app for Android: text chat, microphone input,
spoken replies, conversation history, a settings page and a dark premium design.

You do NOT need to write or edit any code, and you do NOT need an emulator.
The easiest way to get the APK (the installable app file) is to let GitHub build it for free.

---------------------------------------------------------------
## PART 1 - Get the APK (about 10 minutes, no software to install)
---------------------------------------------------------------

1. Create a free account at https://github.com (skip if you have one).
2. Click "+" (top right) -> "New repository". Name it `humaira`, click "Create repository".
3. Unzip HUMAIRA-fixed.zip. Open the unzipped folder: you should see
   `app`, `gradle`, `.github`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`,
   `gradlew`, `gradlew.bat`, `build-apk.yml`, `START_HERE.md`.
4. On the repository page click "uploading an existing file". Select EVERYTHING inside that
   folder (not the folder itself) and drag it into the browser. Wait for the upload to finish,
   then click "Commit changes".
   The `app` folder MUST be uploaded with everything inside it
   (it contains `build.gradle.kts` and `src`). The build tells you by name if a file is missing.

   If `.github` (hidden on some computers) did not upload:
   "Add file" -> "Create new file" -> type exactly `.github/workflows/build-apk.yml`
   -> paste the contents of `build-apk.yml` -> "Commit changes".

5. Click the "Actions" tab. "Build HUMAIRA APK" starts by itself (3-6 minutes).
   If it does not: click it on the left -> "Run workflow".
6. When it is green, open it -> "Artifacts" -> download `HUMAIRA-apk`, unzip -> `app-debug.apk`.

If it fails, open the failed step, copy the red text and send it to Claude.

---------------------------------------------------------------
## PART 2 - Install on your phone
---------------------------------------------------------------

1. Send `app-debug.apk` to your phone (USB cable, Google Drive, WhatsApp to yourself, email...).
2. On the phone tap the file. Android will ask to allow installs from this source - allow it
   (Settings -> "Install unknown apps" -> turn on for the app you opened the file from).
3. Tap Install, then Open. If Play Protect warns about an unknown app, choose "Install anyway"
   (it is your own app).

---------------------------------------------------------------
## PART 3 - Connect the AI (one time)
---------------------------------------------------------------

HUMAIRA's brain is Claude, through Anthropic's API.

1. Go to https://console.anthropic.com on any device, sign in, add a few dollars of credit
   (API credit is separate from a Claude.ai chat subscription), and create an API key.
2. In HUMAIRA tap the gear icon -> paste the key into "Anthropic API key" -> Save.
3. Chat! Tap the microphone to speak. Replies are read aloud (you can turn that off in Settings,
   and tap the square "stop" icon at the top while it is speaking).

Tips
- Haiku 4.5 is the default: fast and cheapest. Switch model in Settings if you want smarter answers.
- Voice input uses your phone's built-in Google speech recognition. If the mic says it is
  unavailable, install/enable the "Google" app.
- Voice replies use your phone's text-to-speech. For other languages, install the voice in
  Settings -> System -> Languages -> Text-to-speech output.
- Your API key and chats stay on your phone.

---------------------------------------------------------------
## Alternative: Android Studio (only if you prefer a desktop tool)
---------------------------------------------------------------
Install Android Studio, choose "Open", pick the HUMAIRA folder, wait for sync, then
Build -> Build Bundle(s)/APK(s) -> Build APK(s). You can also plug in your phone with USB debugging
on and press Run. (The GitHub method above needs no installation, so it is recommended.)

---------------------------------------------------------------
## What is inside (for reference - you never need to edit this)
---------------------------------------------------------------
app/src/main/java/com/humaira/app/
  MainActivity.kt      chat screen, mic, spoken replies
  HistoryActivity.kt   conversation history
  SettingsActivity.kt  API key, model, voice, personality
  AiClient.kt          talks to the AI
  Storage.kt           saves settings + history on the phone
