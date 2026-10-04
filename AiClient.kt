package com.humaira.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiException(val code: Int, message: String) : Exception(message)

/** Talks to the Anthropic Messages API. No extra libraries needed. */
object AiClient {

    fun chat(apiKey: String, model: String, system: String, history: List<Message>): String {
        val messages = JSONArray()
        // The API requires the first message to come from the user.
        for (m in history.dropWhile { it.role != "user" }) {
            messages.put(JSONObject().put("role", m.role).put("content", m.content))
        }

        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 1024)
            .put("system", system)
            .put("messages", messages)

        val conn = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20000
            conn.readTimeout = 90000
            conn.doOutput = true
            conn.setRequestProperty("content-type", "application/json")
            conn.setRequestProperty("x-api-key", apiKey)
            conn.setRequestProperty("anthropic-version", "2023-06-01")

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

            if (code !in 200..299) throw ApiException(code, extractError(text))

            val content = JSONObject(text).getJSONArray("content")
            val sb = StringBuilder()
            for (i in 0 until content.length()) {
                val block = content.getJSONObject(i)
                if (block.optString("type") == "text") sb.append(block.optString("text"))
            }
            val reply = sb.toString().trim()
            if (reply.isEmpty()) throw IOException("The AI sent an empty reply.")
            return reply
        } finally {
            conn.disconnect()
        }
    }

    private fun extractError(body: String): String =
        try {
            JSONObject(body).getJSONObject("error").getString("message")
        } catch (e: Exception) {
            body.take(200)
        }
}
