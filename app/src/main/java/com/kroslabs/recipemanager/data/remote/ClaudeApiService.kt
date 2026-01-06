package com.kroslabs.recipemanager.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ClaudeApiService {
    @POST("messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: ClaudeRequest
    ): ClaudeResponse
}

@Serializable
data class ClaudeRequest(
    val model: String = "claude-sonnet-4-5-20250929",
    @SerialName("max_tokens")
    val maxTokens: Int = 4096,
    val messages: List<ClaudeMessage>
)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: List<ClaudeContent>
)

@Serializable
sealed class ClaudeContent {
    @Serializable
    @SerialName("text")
    data class Text(
        val type: String = "text",
        val text: String
    ) : ClaudeContent()

    @Serializable
    @SerialName("image")
    data class Image(
        val type: String = "image",
        val source: ImageSource
    ) : ClaudeContent()
}

@Serializable
data class ImageSource(
    val type: String = "base64",
    @SerialName("media_type")
    val mediaType: String,
    val data: String
)

@Serializable
data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ResponseContent>,
    val model: String,
    @SerialName("stop_reason")
    val stopReason: String? = null
)

@Serializable
data class ResponseContent(
    val type: String,
    val text: String? = null
)
