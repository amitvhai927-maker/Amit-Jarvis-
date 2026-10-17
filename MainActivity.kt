package com.amit.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

private val Navy = Color(0xFF020817)
private val Panel = Color(0xFF07182D)
private val Cyan = Color(0xFF31C8FF)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var recognized: ((String) -> Unit)? = null
    private val io = Executors.newSingleThreadExecutor()
    private var backendUrl by mutableStateOf("https://YOUR-BACKEND-DOMAIN.example/api/chat")

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) startListening() else speak("Microphone permission is needed for voice commands.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { recognized?.invoke("I couldn't hear that clearly. Please try again.") }
                override fun onResults(results: Bundle?) {
                    val s = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    recognized?.invoke(s.ifBlank { "I didn't catch that. Please try again." })
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        setContent {
            var input by remember { mutableStateOf("") }
            var answer by remember { mutableStateOf("Namaste! I am AMIT, your personal assistant. Tap the microphone or type a command.") }
            var listening by remember { mutableStateOf(false) }
            var busy by remember { mutableStateOf(false) }
            var endpoint by remember { mutableStateOf(backendUrl) }
            val history = remember { mutableStateListOf("AMIT: Ready to help you.") }
            val reminders = remember { mutableStateListOf<String>() }

            MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Navy, surface = Panel,
                onPrimary = Navy, onBackground = Color.White, onSurface = Color.White)) {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF061A35), Navy)))
                        .verticalScroll(rememberScrollState()).padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(18.dp))
                    Text("YOUR PERSONAL AI ASSISTANT", color = Cyan, fontSize = 11.sp, letterSpacing = 3.sp)
                    Text("AMIT", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
                    Text("JARVIS • AI ASSISTANT", color = Cyan, fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.size(172.dp).background(
                        Brush.radialGradient(listOf(Color(0xFF0E5B91), Color(0xFF06172E), Navy)), CircleShape
                    ), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = {
                                recognized = { spoken ->
                                    listening = false
                                    input = spoken
                                    history.add("You: $spoken")
                                    busy = true
                                    runCommand(spoken, endpoint, reminders) { result ->
                                        answer = result
                                        history.add("AMIT: $result")
                                        speak(result)
                                        busy = false
                                    }
                                }
                                listening = true
                                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                                    == PackageManager.PERMISSION_GRANTED) {
                                    startListening()
                                } else {
                                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.size(116.dp), shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B3154))
                        ) { Text(if (listening) "LISTENING…" else "🎙", fontSize = if (listening) 12.sp else 36.sp) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(if (listening) "Listening… speak now" else if (busy) "Thinking…" else "Tap to speak",
                        color = Cyan)
                    Spacer(Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(15.dp)) {
                            Text("AMIT SAYS", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(Modifier.height(7.dp))
                            Text(answer, color = Color.White, fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(input, { input = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Type a command or ask a question") }, singleLine = false)
                    Button(onClick = {
                        if (input.isNotBlank()) {
                            history.add("You: $input")
                            busy = true
                            runCommand(input, endpoint, reminders) { result ->
                                answer = result; history.add("AMIT: $result"); speak(result); busy = false
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !busy) { Text(if (busy) "Working…" else "Ask AMIT") }

                    Spacer(Modifier.height(12.dp))
                    Text("AI BACKEND URL", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(endpoint, { endpoint = it; backendUrl = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Paste your deployed backend /api/chat URL") },
                        singleLine = true)
                    Text("Keep AI provider keys on the backend, never inside the app.", color = Color.LightGray, fontSize = 11.sp)

                    Spacer(Modifier.height(16.dp))
                    Text("QUICK ACTIONS", color = Cyan, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        QuickButton("YouTube", Modifier.weight(1f)) { openWeb("https://www.youtube.com") }
                        QuickButton("Google", Modifier.weight(1f)) { openWeb("https://www.google.com") }
                        QuickButton("Help", Modifier.weight(1f)) {
                            answer = "Try: open YouTube, open Google, search cats, call 98XXXXXXXX, add reminder study at 6, or ask a question after connecting the AI backend."
                            speak(answer)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("REMINDERS (LOCAL LIST)", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (reminders.isEmpty()) Text("No reminders added yet.", color = Color.LightGray, fontSize = 12.sp)
                    reminders.takeLast(5).forEach { Text("• $it", color = Color.White, modifier = Modifier.fillMaxWidth().padding(3.dp)) }
                    Spacer(Modifier.height(14.dp))
                    Text("RECENT CHAT", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    history.takeLast(8).forEach { Text(it, color = Color(0xFFD9E9F7), fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) }
                    Spacer(Modifier.height(18.dp))
                    Text("Starter features work locally; background wake-word listening is not enabled.", color = Color.LightGray, fontSize = 10.sp)
                }
            }
        }
    }

    @Composable
    private fun QuickButton(label: String, modifier: Modifier, action: () -> Unit) {
        OutlinedButton(onClick = action, modifier = modifier) { Text(label, fontSize = 11.sp) }
    }

    private fun startListening() {
        val r = recognizer ?: run { recognized?.invoke("Speech recognition is not available on this device."); return }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO); return
        }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to AMIT")
        }
        r.startListening(i)
    }

    private fun runCommand(raw: String, endpoint: String, reminders: MutableList<String>, done: (String) -> Unit) {
        val c = raw.lowercase(Locale.ROOT).trim()
        when {
            c.contains("open youtube") || c == "youtube" -> { openWeb("https://www.youtube.com"); done("Opening YouTube.") }
            c.contains("open google") || c == "google" -> { openWeb("https://www.google.com"); done("Opening Google.") }
            c.startsWith("search ") -> {
                val q = raw.substringAfter("search ").trim()
                openWeb("https://www.google.com/search?q=" + Uri.encode(q)); done("Searching for $q.")
            }
            c.startsWith("call ") -> {
                val n = raw.substringAfter("call ").trim()
                if (n.isNotBlank()) { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(n)))); done("Opening the dialer. Review the number and tap call.") }
                else done("Say call followed by a phone number.")
            }
            c.startsWith("add reminder ") -> {
                val r = raw.substringAfter("add reminder ").trim()
                if (r.isNotBlank()) { reminders.add(r); done("Added to your local reminder list: $r. This starter does not send a timed notification yet.") }
                else done("Tell me what reminder to add.")
            }
            c.contains("time") -> done("The current time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()))
            c.contains("your name") || c.contains("who are you") -> done("I am AMIT, your personal assistant inspired by JARVIS.")
            c.contains("hello") || c.contains("namaste") || c.contains("नमस्ते") -> done("Namaste! I am AMIT. How can I help?")
            c == "help" -> done("Try open YouTube, open Google, search followed by a topic, call followed by a number, add reminder followed by a task, or ask a question after connecting the AI backend.")
            else -> askBackend(raw, endpoint, done)
        }
    }

    private fun askBackend(message: String, endpoint: String, done: (String) -> Unit) {
        if (endpoint.contains("YOUR-BACKEND-DOMAIN")) {
            done("Your question is ready, but AI is not connected yet. Deploy the backend included in this ZIP, then paste its /api/chat URL above.")
            return
        }
        io.execute {
            var connection: HttpURLConnection? = null
            val result = try {
                connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                val body = JSONObject().put("message", message).toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                json.optString("reply", json.optString("error", "The AI server returned an unexpected response."))
            } catch (e: Exception) {
                "Could not reach the AI backend. Check the URL and internet connection, then try again."
            } finally { connection?.disconnect() }
            runOnUiThread { done(result) }
        }
    }

    private fun openWeb(url: String) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    private fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "amit-reply") }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() }
    override fun onDestroy() { recognizer?.destroy(); tts?.shutdown(); io.shutdown(); super.onDestroy() }
}
