package com.claudeplugin.api

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class Message(val role: String, val content: String)

class ClaudeApiClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun sendMessage(
        messages: List<Message>,
        systemPrompt: String,
        onChunk: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val messagesJson = messages.map { mapOf("role" to it.role, "content" to it.content) }

        val body = mapOf(
            "model" to "claude-sonnet-4-20250514",
            "max_tokens" to 8000,
            "system" to systemPrompt,
            "stream" to true,
            "messages" to messagesJson
        )

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Ошибка соединения: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError("Ошибка API: ${response.code} ${response.message}")
                    return
                }

                val fullResponse = StringBuilder()
                response.body?.source()?.let { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") break
                            try {
                                val json = JsonParser.parseString(data).asJsonObject
                                val type = json.get("type")?.asString
                                if (type == "content_block_delta") {
                                    val delta = json.getAsJsonObject("delta")
                                    val text = delta?.get("text")?.asString ?: ""
                                    if (text.isNotEmpty()) {
                                        fullResponse.append(text)
                                        onChunk(text)
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
                onComplete(fullResponse.toString())
            }
        })
    }

    companion object {
        const val SYSTEM_ANDROID_GENERATOR = """Ты — эксперт Android разработчик. 
Ты генерируешь чистый, современный Kotlin код для Android.
Всегда используй:
- MVVM архитектуру
- ViewBinding
- Hilt для DI
- Coroutines + Flow
- Material Design 3 компоненты

Когда генерируешь файлы, ОБЯЗАТЕЛЬНО используй формат:
=== ФАЙЛ: путь/к/файлу.kt ===
(код файла)
=== КОНЕЦ ФАЙЛА ===

Это критически важно для автоматического создания файлов в проекте.
Генерируй полный рабочий код без placeholder'ов."""
    }
}
