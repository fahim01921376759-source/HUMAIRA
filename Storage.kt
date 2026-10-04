package com.humaira.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Saves settings and conversation history on the phone. */
class Storage(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("humaira_settings", Context.MODE_PRIVATE)
    private val file = File(context.applicationContext.filesDir, "conversations.json")

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) {
            prefs.edit().putString("api_key", value.trim()).apply()
        }

    var model: String
        get() = prefs.getString("model", MODEL_FAST) ?: MODEL_FAST
        set(value) {
            prefs.edit().putString("model", value).apply()
        }

    var speakReplies: Boolean
        get() = prefs.getBoolean("speak_replies", true)
        set(value) {
            prefs.edit().putBoolean("speak_replies", value).apply()
        }

    var speechRate: Float
        get() = prefs.getFloat("speech_rate", 1.0f)
        set(value) {
            prefs.edit().putFloat("speech_rate", value).apply()
        }

    var instructions: String
        get() = prefs.getString("instructions", DEFAULT_INSTRUCTIONS) ?: DEFAULT_INSTRUCTIONS
        set(value) {
            prefs.edit().putString("instructions", value).apply()
        }

    /** The conversation currently open on the main screen. */
    var currentId: String
        get() = prefs.getString("current_id", "") ?: ""
        set(value) {
            prefs.edit().putString("current_id", value).apply()
        }

    fun loadAll(): MutableList<Conversation> {
        if (!file.exists()) return mutableListOf()
        return try {
            val arr = JSONArray(file.readText())
            val list = mutableListOf<Conversation>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val msgs = mutableListOf<Message>()
                val ma = o.getJSONArray("messages")
                for (j in 0 until ma.length()) {
                    val m = ma.getJSONObject(j)
                    msgs.add(Message(m.getString("role"), m.getString("content"), m.optLong("time")))
                }
                list.add(
                    Conversation(
                        id = o.getString("id"),
                        title = o.optString("title", "Chat"),
                        updated = o.optLong("updated"),
                        messages = msgs
                    )
                )
            }
            list
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveAll(list: List<Conversation>) {
        val arr = JSONArray()
        for (c in list) {
            val ma = JSONArray()
            for (m in c.messages) {
                ma.put(
                    JSONObject()
                        .put("role", m.role)
                        .put("content", m.content)
                        .put("time", m.time)
                )
            }
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("title", c.title)
                    .put("updated", c.updated)
                    .put("messages", ma)
            )
        }
        file.writeText(arr.toString())
    }

    fun get(id: String): Conversation? = loadAll().firstOrNull { it.id == id }

    fun upsert(c: Conversation) {
        val list = loadAll()
        val index = list.indexOfFirst { it.id == c.id }
        if (index >= 0) list[index] = c else list.add(c)
        saveAll(list)
    }

    fun delete(id: String) {
        saveAll(loadAll().filter { it.id != id })
    }

    fun deleteAll() {
        file.delete()
    }

    companion object {
        const val MODEL_FAST = "claude-haiku-4-5-20251001"
        const val MODEL_SMART = "claude-sonnet-5-5"
        const val MODEL_BEST = "claude-opus-5-5"

        const val DEFAULT_INSTRUCTIONS =
            "You are HUMAIRA, a warm, smart and concise personal AI assistant. " +
                "Always reply in the same language the user writes in. " +
                "Your replies may be read aloud, so write plain conversational text: " +
                "no markdown, no bullet symbols, no emojis. " +
                "Keep answers short unless the user asks for detail."
    }
}
