package com.asgerjon.comp1337

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.components.Service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
class LocalModelService {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()

    suspend fun complete(prefix: String, suffix: String, settings: Comp1337Settings.State): String? {
        if (prefix.isBlank() && suffix.isBlank()) return null
        return withTimeoutOrNull(settings.timeoutMs.toLong()) {
            runCatching {
                withContext(Dispatchers.IO) {
                    when (settings.backend) {
                        "llamacpp" -> completeLlamaCpp(prefix, suffix, settings)
                        else -> completeOpenAiCompatible(prefix, suffix, settings)
                    }
                }
            }.getOrNull()
        }
    }

    suspend fun warmup(settings: Comp1337Settings.State) {
        complete("fun warmup(", "):\n    pass\n", settings)
    }

    private suspend fun completeLlamaCpp(prefix: String, suffix: String, s: Comp1337Settings.State): String {
        val body = JsonObject().apply {
            addProperty("input_prefix", prefix)
            addProperty("input_suffix", suffix)
            addProperty("n_predict", s.maxTokens)
            addProperty("temperature", 0.0)
            addProperty("cache_prompt", true)
            add("stop", JsonArray().apply { add("\n\n\n") })
        }
        val json = post("${s.endpoint.trimEnd('/')}/infill", body)
        return JsonParser.parseString(json).asJsonObject.get("content").asString
    }

    private suspend fun completeOpenAiCompatible(prefix: String, suffix: String, s: Comp1337Settings.State): String {
        val prompt = "<|fim_prefix|>$prefix<|fim_suffix|>$suffix<|fim_middle|>"
        val body = JsonObject().apply {
            addProperty("model", s.model)
            addProperty("prompt", prompt)
            addProperty("max_tokens", s.maxTokens)
            addProperty("temperature", 0.0)
            addProperty("stream", false)
            add("stop", JsonArray().apply {
                add("<|fim_suffix|>")
                add("<|fim_middle|>")
                add("<|fim_prefix|>")
                add("\n\n\n")
            })
        }
        val json = post("${s.endpoint.trimEnd('/')}/completions", body)
        return JsonParser.parseString(json).asJsonObject
            .getAsJsonArray("choices").first().asJsonObject.get("text").asString
    }

    private suspend fun post(url: String, body: JsonObject): String {
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()
        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        check(response.statusCode() in 200..299) { "comp1337: HTTP ${response.statusCode()} from $url" }
        return response.body()
    }
}
