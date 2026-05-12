package com.example.appmovilstress.service

import android.util.Log
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AIRecommendationService(
    private val apiKey: String
) {

    suspend fun generarRecomendacion(
        puntuacion: Int,
        nivelEstres: String
    ): RecommendationResult {
        if (apiKey.isBlank()) {
            return RecommendationResult(
                texto = generarRecomendacionLocal(nivelEstres),
                generadaPorGemini = false,
                aviso = "No hay clave de Gemini configurada. Se usa la recomendacion local."
            )
        }

        repeat(MAX_RETRIES) { attempt ->
            try {
                val texto = generarConGemini(apiKey, puntuacion, nivelEstres)
                return RecommendationResult(
                    texto = texto,
                    generadaPorGemini = true,
                    aviso = null
                )
            } catch (exception: GeminiApiException) {
                Log.e(TAG, "Gemini error ${exception.code}: ${exception.message}")

                val retryable = exception.code == 503 || exception.code == 429
                if (retryable && attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAYS_MS[attempt])
                } else {
                    return RecommendationResult(
                        texto = generarRecomendacionLocal(nivelEstres),
                        generadaPorGemini = false,
                        aviso = if (exception.code in 200..299) {
                            "Gemini devolvio una respuesta incompleta. Se usa la recomendacion local."
                        } else {
                            "Gemini no disponible ahora mismo (${exception.code}). Se usa la recomendacion local."
                        }
                    )
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Gemini unexpected error", exception)
                return RecommendationResult(
                    texto = generarRecomendacionLocal(nivelEstres),
                    generadaPorGemini = false,
                    aviso = "No se pudo contactar con Gemini. Se usa la recomendacion local."
                )
            }
        }

        return RecommendationResult(
            texto = generarRecomendacionLocal(nivelEstres),
            generadaPorGemini = false,
            aviso = "Gemini no ha respondido. Se usa la recomendacion local."
        )
    }

    fun generarRecomendacionLocal(nivelEstres: String): String {
        return when (nivelEstres) {
            "Estr\u00e9s bajo" -> "Tu nivel de estr\u00e9s es bajo. Mant\u00e9n tus h\u00e1bitos saludables, duerme bien, realiza actividad f\u00edsica y reserva tiempo para actividades que te aporten bienestar."
            "Estr\u00e9s moderado" -> "Tu nivel de estr\u00e9s es moderado. Intenta introducir pausas activas, ejercicios de respiraci\u00f3n y una mejor organizaci\u00f3n del tiempo para reducir la carga diaria."
            else -> "Tu nivel de estr\u00e9s es alto. Prioriza el descanso, utiliza t\u00e9cnicas de relajaci\u00f3n como respiraci\u00f3n guiada o mindfulness y valora consultar con un profesional si esta situaci\u00f3n persiste."
        }
    }

    private fun generarConGemini(
        apiKey: String,
        puntuacion: Int,
        nivelEstres: String
    ): String {
        val url = URL("$BASE_URL?key=$apiKey")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        return try {
            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().put("text", buildPrompt(puntuacion, nivelEstres))
                                )
                            )
                        }
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", 0.4)
                        put("maxOutputTokens", 512)
                    }
                )
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: throw GeminiApiException(connection.responseCode, "Gemini request failed")
            }

            val body = BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }

            if (connection.responseCode !in 200..299) {
                val errorMessage = parseErrorMessage(body)
                throw GeminiApiException(connection.responseCode, errorMessage)
            }

            val recommendation = parseRecommendation(body)
            val finishReason = parseFinishReason(body)

            if (recommendation.isEmpty()) {
                throw GeminiApiException(connection.responseCode, "Empty Gemini response")
            }

            if (recommendation.length < MIN_RECOMMENDATION_LENGTH && finishReason == "MAX_TOKENS") {
                throw GeminiApiException(
                    connection.responseCode,
                    "Incomplete Gemini response: $recommendation"
                )
            }

            if (recommendation.length < MIN_RECOMMENDATION_LENGTH_HARD) {
                throw GeminiApiException(
                    connection.responseCode,
                    "Too short Gemini response: $recommendation"
                )
            }

            sanitizeRecommendation(recommendation)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRecommendation(responseBody: String): String {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""

        val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        if (parts.length() == 0) return ""

        val builder = StringBuilder()

        for (index in 0 until parts.length()) {
            val text = parts.optJSONObject(index)?.optString("text", "").orEmpty().trim()
            if (text.isNotEmpty()) {
                if (builder.isNotEmpty()) {
                    builder.append('\n')
                }
                builder.append(text)
            }
        }

        return builder.toString().trim()
    }

    private fun parseErrorMessage(responseBody: String): String {
        return runCatching {
            JSONObject(responseBody)
                .optJSONObject("error")
                ?.optString("message")
                .orEmpty()
        }.getOrDefault(responseBody)
    }

    private fun parseFinishReason(responseBody: String): String? {
        return runCatching {
            JSONObject(responseBody)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optString("finishReason")
        }.getOrNull()
    }

    private fun sanitizeRecommendation(text: String): String {
        return text
            .removePrefix("Hola. ")
            .removePrefix("Hola, ")
            .removePrefix("Entiendo que ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun buildPrompt(puntuacion: Int, nivelEstres: String): String {
        return """
            Eres un asistente de bienestar para una aplicacion movil academica de monitorizacion del estres.
            Responde solo en espanol.
            No hagas diagnosticos medicos.
            No uses tono alarmista.
            No saludes ni empieces con frases como "Hola" o "Entiendo que".
            Empieza directamente por la recomendacion.
            Da una recomendacion personalizada breve, clara y util.
            Incluye habitos saludables, respiracion, organizacion del tiempo o descanso si encaja con el nivel detectado.
            Si el nivel es alto, menciona de forma prudente que podria consultar con un profesional si el malestar persiste.
            Devuelve un unico parrafo completo de entre 60 y 90 palabras.

            Puntuacion PSS-14: $puntuacion
            Nivel de estres: $nivelEstres
        """.trimIndent()
    }

    data class RecommendationResult(
        val texto: String,
        val generadaPorGemini: Boolean,
        val aviso: String?
    )

    private data class GeminiApiException(
        val code: Int,
        override val message: String
    ) : Exception(message)

    companion object {
        private const val TAG = "AIRecommendationService"
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        private const val MAX_RETRIES = 3
        private const val MIN_RECOMMENDATION_LENGTH = 45
        private const val MIN_RECOMMENDATION_LENGTH_HARD = 25
        private val RETRY_DELAYS_MS = listOf(1500L, 3000L, 5000L)
    }
}
