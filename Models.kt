package com.humaira.app

import java.util.UUID

data class Message(
    val role: String,            // "user" or "assistant"
    val content: String,
    val time: Long = System.currentTimeMillis()
)

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New chat",
    var updated: Long = System.currentTimeMillis(),
    val messages: MutableList<Message> = mutableListOf()
)
