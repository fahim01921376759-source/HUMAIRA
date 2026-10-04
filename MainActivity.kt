package com.humaira.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.humaira.app.databinding.ActivityMainBinding
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var storage: Storage
    private lateinit var adapter: MessageAdapter

    private var conversation = Conversation()
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var busy = false
    private val executor = Executors.newSingleThreadExecutor()

    // Opens the phone's built-in speech recognition screen.
    private val voiceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val spoken = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                if (!spoken.isNullOrBlank()) sendMessage(spoken)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        storage = Storage(this)
        adapter = MessageAdapter(conversation.messages)
        b.messages.layoutManager = LinearLayoutManager(this)
        b.messages.adapter = adapter

        setupVoiceOutput()

        b.btnSend.setOnClickListener { sendMessage(b.input.text.toString()) }
        b.btnMic.setOnClickListener { startListening() }
        b.btnNew.setOnClickListener { startNewChat() }
        b.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        b.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        b.btnStop.setOnClickListener {
            tts?.stop()
            b.btnStop.visibility = View.GONE
        }

        loadCurrent()
    }

    override fun onResume() {
        super.onResume()
        // History screen may have switched or deleted the open conversation.
        if (storage.currentId != conversation.id) loadCurrent()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        executor.shutdown()
    }

    // ---------- conversations ----------

    private fun loadCurrent() {
        conversation = storage.get(storage.currentId) ?: Conversation()
        storage.currentId = conversation.id
        adapter.setItems(conversation.messages)
        refreshEmptyState()
        scrollToEnd()
    }

    private fun startNewChat() {
        tts?.stop()
        conversation = Conversation()
        storage.currentId = conversation.id
        adapter.setItems(conversation.messages)
        refreshEmptyState()
    }

    private fun refreshEmptyState() {
        b.emptyState.visibility = if (conversation.messages.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun scrollToEnd() {
        if (adapter.itemCount > 0) b.messages.scrollToPosition(adapter.itemCount - 1)
    }

    // ---------- sending / receiving ----------

    private fun sendMessage(text: String) {
        val msg = text.trim()
        if (msg.isEmpty()) return
        if (busy) {
            toast("HUMAIRA is still answering...")
            return
        }
        if (storage.apiKey.isBlank()) {
            toast("Please add your API key in Settings first.")
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        tts?.stop()
        val target = conversation
        if (target.messages.isEmpty()) target.title = msg.replace("\n", " ").take(40)
        target.messages.add(Message("user", msg))
        target.updated = System.currentTimeMillis()
        storage.upsert(target)
        storage.currentId = target.id

        adapter.notifyItemInserted(target.messages.size - 1)
        b.input.setText("")
        refreshEmptyState()
        scrollToEnd()
        setBusy(true)

        val key = storage.apiKey
        val model = storage.model
        val system = storage.instructions
        val history = target.messages.takeLast(20).toList()

        executor.execute {
            val result = runCatching { AiClient.chat(key, model, system, history) }
            runOnUiThread {
                setBusy(false)
                result
                    .onSuccess { reply -> onReply(target, reply) }
                    .onFailure { e ->
                        Snackbar.make(b.root, friendlyError(e), Snackbar.LENGTH_LONG)
                            .setTextMaxLines(6)
                            .show()
                    }
            }
        }
    }

    private fun onReply(target: Conversation, reply: String) {
        val c = if (conversation.id == target.id) conversation
        else (storage.get(target.id) ?: target)

        c.messages.add(Message("assistant", reply))
        c.updated = System.currentTimeMillis()
        storage.upsert(c)

        if (c === conversation) {
            adapter.notifyItemInserted(c.messages.size - 1)
            refreshEmptyState()
            scrollToEnd()
            if (storage.speakReplies) speak(reply)
        }
    }

    private fun setBusy(value: Boolean) {
        busy = value
        b.thinking.visibility = if (value) View.VISIBLE else View.GONE
        b.btnSend.isEnabled = !value
        b.btnMic.isEnabled = !value
        val alpha = if (value) 0.4f else 1f
        b.btnSend.alpha = alpha
        b.btnMic.alpha = alpha
    }

    private fun friendlyError(e: Throwable): String = when {
        e is ApiException && (e.code == 401 || e.code == 403) ->
            "Your API key was not accepted. Please check it in Settings."
        e is ApiException && e.code == 404 ->
            "That AI model is not available for your account. Choose another one in Settings."
        e is ApiException && e.code == 429 ->
            "Too many requests right now. Wait a moment and try again."
        e is ApiException -> "Error ${e.code}: ${e.message}"
        e is UnknownHostException || e is ConnectException || e is SocketTimeoutException ->
            "Could not reach the AI. Please check your internet connection."
        else -> "Something went wrong: ${e.message}"
    }

    // ---------- voice input ----------

    private fun startListening() {
        if (busy) return
        tts?.stop()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to HUMAIRA")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            toast("Voice input is not available on this phone. Please install or enable the Google app.")
        }
    }

    // ---------- voice output ----------

    private fun setupVoiceOutput() {
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val r = engine.setLanguage(Locale.getDefault())
                    if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.setLanguage(Locale.US)
                    }
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            runOnUiThread { b.btnStop.visibility = View.VISIBLE }
                        }

                        override fun onDone(utteranceId: String?) {
                            runOnUiThread { b.btnStop.visibility = View.GONE }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            runOnUiThread { b.btnStop.visibility = View.GONE }
                        }
                    })
                    ttsReady = true
                }
            }
        }
    }

    private fun speak(text: String) {
        if (!ttsReady) return
        val clean = text
            .replace(Regex("[*#`_>~]"), "")
            .replace(Regex("\\s+"), " ")
            .take(3900)
        tts?.setSpeechRate(storage.speechRate)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "humaira")
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
