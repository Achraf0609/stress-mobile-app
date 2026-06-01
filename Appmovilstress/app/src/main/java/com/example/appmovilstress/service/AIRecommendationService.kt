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

/*
 * Archivo encargado de comunicarse con la API de Gemini para generar
 * recomendaciones personalizadas a partir del nivel de estres del usuario.
 */
class AIRecommendationService(
    private val apiKey: String
) {

    // Metodo principal que intenta obtener una recomendacion de Gemini y aplica fallback si falla.
    suspend fun generarRecomendacion(
        puntuacion: Int,
        nivelEstres: String,
        recomendacionesPrevias: List<String> = emptyList()
    ): RecommendationResult {
        // Si no hay clave API configurada, se devuelve directamente la recomendacion local.
        if (apiKey.isBlank()) {
            return RecommendationResult(
                texto = generarRecomendacionLocal(nivelEstres, recomendacionesPrevias),
                generadaPorGemini = false,
                aviso = "No hay clave de Gemini configurada. Se usa la recomendacion local."
            )
        }

        // Reintenta automaticamente varias veces ante errores temporales del servicio.
        repeat(MAX_RETRIES) { attempt ->
            try {
                val texto = generarConGemini(
                    apiKey = apiKey,
                    puntuacion = puntuacion,
                    nivelEstres = nivelEstres,
                    recomendacionesPrevias = recomendacionesPrevias,
                    attempt = attempt
                )
                return RecommendationResult(
                    texto = texto,
                    generadaPorGemini = true,
                    aviso = null
                )
            } catch (exception: GeminiApiException) {
                Log.e(TAG, "Gemini error ${exception.code}: ${exception.message}")

                // Se reintenta tanto en errores transitorios del servicio como en respuestas incompletas.
                val retryable = shouldRetry(exception)
                if (retryable && attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAYS_MS[attempt])
                } else {
                    return RecommendationResult(
                        texto = generarRecomendacionLocal(nivelEstres, recomendacionesPrevias),
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
                    texto = generarRecomendacionLocal(nivelEstres, recomendacionesPrevias),
                    generadaPorGemini = false,
                    aviso = "No se pudo contactar con Gemini. Se usa la recomendacion local."
                )
            }
        }

        // Salvaguarda final si no se ha podido completar el flujo anterior.
        return RecommendationResult(
            texto = generarRecomendacionLocal(nivelEstres, recomendacionesPrevias),
            generadaPorGemini = false,
            aviso = "Gemini no ha respondido. Se usa la recomendacion local."
        )
    }

    // Genera una recomendacion local por reglas para usarla como plan de respaldo.
    fun generarRecomendacionLocal(
        nivelEstres: String,
        recomendacionesPrevias: List<String> = emptyList()
    ): String {
        val candidates = when (nivelEstres) {
            "Estr\u00e9s bajo" -> LOW_STRESS_FALLBACKS
            "Estr\u00e9s moderado" -> MODERATE_STRESS_FALLBACKS
            else -> HIGH_STRESS_FALLBACKS
        }

        return selectNonRepeatedFallback(candidates, recomendacionesPrevias)
    }

    // Realiza la peticion HTTP a Gemini y valida la respuesta recibida.
    private fun generarConGemini(
        apiKey: String,
        puntuacion: Int,
        nivelEstres: String,
        recomendacionesPrevias: List<String>,
        attempt: Int
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
            // Construye el cuerpo JSON de la peticion con el prompt y la configuracion de generacion.
            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray().put(
                                    // Insercion del prompt construido dinamicamente dentro del cuerpo JSON enviado a Gemini.
                                    JSONObject().put("text", buildPrompt(puntuacion, nivelEstres, recomendacionesPrevias, attempt))
                                )
                            )
                        }
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", temperatureForAttempt(attempt))
                        put("maxOutputTokens", 512)
                        // Para consejos breves, evita que Gemini consuma la salida en razonamiento interno.
                        put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
                    }
                )
            }

            // Envia el JSON a la API remota.
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            // Obtencion de la respuesta HTTP devuelta por Gemini.
            // Si el codigo es correcto, se lee el inputStream; si no, se recupera el error devuelto por la API.
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: throw GeminiApiException(connection.responseCode, "Gemini request failed")
            }

            // Conversion de la respuesta recibida en texto para poder procesarla posteriormente.
            val body = BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }

            // Comprobacion de errores de la API antes de procesar el contenido generado.
            if (connection.responseCode !in 200..299) {
                val errorMessage = parseErrorMessage(body)
                throw GeminiApiException(connection.responseCode, errorMessage)
            }

            // Procesamiento de la respuesta de Gemini: extraccion del texto generado y verificacion basica del resultado.
            val recommendation = parseRecommendation(body)
            val finishReason = parseFinishReason(body)
            val sanitizedRecommendation = sanitizeRecommendation(recommendation)

            if (sanitizedRecommendation.isEmpty()) {
                throw GeminiApiException(connection.responseCode, "Empty Gemini response")
            }

            // Si la salida se corto por falta de tokens, se fuerza un reintento para evitar guardar texto incompleto.
            if (finishReason == "MAX_TOKENS") {
                throw GeminiApiException(
                    connection.responseCode,
                    "Incomplete Gemini response due to token limit: $sanitizedRecommendation"
                )
            }

            // Si el texto es excesivamente corto, se considera una respuesta invalida.
            if (sanitizedRecommendation.length < MIN_RECOMMENDATION_LENGTH_HARD) {
                throw GeminiApiException(
                    connection.responseCode,
                    "Too short Gemini response: $sanitizedRecommendation"
                )
            }

            // Intenta aprovechar respuestas casi validas antes de descartarlas por completo.
            val completedRecommendation = recoverRecommendation(
                text = sanitizedRecommendation,
                finishReason = finishReason,
                nivelEstres = nivelEstres
            )

            if (completedRecommendation == null) {
                throw GeminiApiException(
                    connection.responseCode,
                    "Incomplete Gemini sentence: $sanitizedRecommendation"
                )
            }

            // Evita guardar una recomendacion practicamente identica a alguna de las ultimas ya registradas.
            if (isDuplicateRecommendation(completedRecommendation, recomendacionesPrevias)) {
                throw GeminiApiException(
                    connection.responseCode,
                    "Repeated Gemini recommendation: $completedRecommendation"
                )
            }

            // Devuelve el texto final ya normalizado y validado.
            completedRecommendation
        } finally {
            connection.disconnect()
        }
    }

    // Extrae el texto de recomendacion desde la estructura JSON devuelta por Gemini.
    private fun parseRecommendation(responseBody: String): String {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""

        val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        if (parts.length() == 0) return ""

        val builder = StringBuilder()

        // Recorrido de los fragmentos devueltos por Gemini para unir el texto completo de la recomendacion.
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

    // Obtiene el mensaje de error textual cuando la API devuelve un codigo no valido.
    private fun parseErrorMessage(responseBody: String): String {
        return runCatching {
            JSONObject(responseBody)
                .optJSONObject("error")
                ?.optString("message")
                .orEmpty()
        }.getOrDefault(responseBody)
    }

    // Lee el motivo de finalizacion de Gemini para saber si la salida fue truncada o completada.
    private fun parseFinishReason(responseBody: String): String? {
        return runCatching {
            JSONObject(responseBody)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optString("finishReason")
        }.getOrNull()
    }

    // Elimina saludos y normaliza espacios para mejorar la recomendacion final mostrada.
    private fun sanitizeRecommendation(text: String): String {
        return text
            .removePrefix("Hola. ")
            .removePrefix("Hola, ")
            .removePrefix("Entiendo que ")
            .removePrefix("Comprendo que ")
            .removePrefix("Veo que ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // Comprueba si la recomendacion tiene un cierre natural y no termina en una frase a medias.
    private fun looksComplete(text: String): Boolean {
        if (text.length < MIN_RECOMMENDATION_LENGTH) {
            return false
        }

        val trimmed = text.trim()
        val endsWithSentencePunctuation = trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")
        if (!endsWithSentencePunctuation) {
            return false
        }

        if (endsWithDanglingWord(trimmed)) {
            return false
        }

        return true
    }

    // Intenta reparar una respuesta de Gemini si esta casi completa en lugar de descartarla directamente.
    private fun recoverRecommendation(
        text: String,
        finishReason: String?,
        nivelEstres: String
    ): String? {
        val trimmed = text.trim()

        if (looksComplete(trimmed)) {
            return trimmed
        }

        val lastSentenceCandidate = trimmedToLastCompleteSentence(trimmed)
        if (lastSentenceCandidate != null && looksComplete(lastSentenceCandidate)) {
            return lastSentenceCandidate
        }

        if (finishReason != "MAX_TOKENS" &&
            trimmed.length >= MIN_RECOMMENDATION_LENGTH &&
            !endsWithDanglingWord(trimmed)
        ) {
            return trimmed.trimEnd(',', ';', ':') + "."
        }

        val repaired = trimDanglingTail(trimmed)
        if (repaired != null) {
            return repaired
        }

        val completedWithClosure = appendContextualClosure(trimmed, nivelEstres)
        if (completedWithClosure != null) {
            return completedWithClosure
        }

        return null
    }

    // Recorta el texto hasta la ultima frase cerrada si Gemini dejo contenido extra a medias al final.
    private fun trimmedToLastCompleteSentence(text: String): String? {
        val lastDot = text.lastIndexOf('.')
        val lastQuestion = text.lastIndexOf('?')
        val lastExclamation = text.lastIndexOf('!')
        val endIndex = maxOf(lastDot, lastQuestion, lastExclamation)

        if (endIndex == -1) {
            return null
        }

        val candidate = text.substring(0, endIndex + 1).trim()
        return candidate.takeIf { it.length >= MIN_RECOVERED_RECOMMENDATION_LENGTH }
    }

    // Detecta finales abiertos como "como", "para" o "con" que suelen indicar una frase cortada.
    private fun endsWithDanglingWord(text: String): Boolean {
        val normalizedLastWord = text
            .trimEnd('.', '!', '?', ',', ';', ':')
            .substringAfterLast(' ')
            .lowercase()

        return normalizedLastWord in DANGLING_ENDINGS
    }

    // Recorta una cola final abierta y deja una recomendacion corta pero util.
    private fun trimDanglingTail(text: String): String? {
        if (!endsWithDanglingWord(text)) {
            return null
        }

        val separators = listOf(',', ';', ':')
        val lastSeparatorIndex = separators.maxOfOrNull { separator -> text.lastIndexOf(separator) } ?: -1
        if (lastSeparatorIndex == -1) {
            return null
        }

        val candidate = text.substring(0, lastSeparatorIndex).trim()
        return if (candidate.length >= MIN_RECOVERED_RECOMMENDATION_LENGTH) {
            candidate + "."
        } else {
            null
        }
    }

    // Completa una recomendacion parcialmente valida con una frase final segura segun el nivel de estres.
    private fun appendContextualClosure(text: String, nivelEstres: String): String? {
        if (text.length < MIN_RECOVERED_RECOMMENDATION_LENGTH) {
            return null
        }

        val base = text
            .trimEnd(',', ';', ':')
            .takeIf { it.isNotBlank() }
            ?: return null

        val completed = if (endsWithDanglingWord(base)) {
            trimDanglingTail(base)
        } else {
            "$base."
        } ?: return null

        val withClosure = "$completed ${contextualClosureForLevel(nivelEstres)}".trim()
        return withClosure.takeIf { it.length >= MIN_RECOMMENDATION_LENGTH_HARD }
    }

    // Frase final breve y segura para cerrar recomendaciones cuando Gemini deja el texto a medias.
    private fun contextualClosureForLevel(nivelEstres: String): String {
        return when (nivelEstres) {
            "Estr\u00e9s bajo" -> "Mantener una rutina equilibrada te ayudara a conservar este bienestar."
            "Estr\u00e9s moderado" -> "Aplicar estos cambios de forma constante puede ayudarte a reducir la tension diaria."
            else -> "Si el malestar persiste, seria recomendable buscar apoyo profesional."
        }
    }

    // Determina si un fallo de Gemini merece un nuevo intento automatico.
    private fun shouldRetry(exception: GeminiApiException): Boolean {
        return exception.code == 503 ||
            exception.code == 429 ||
            (exception.code in 200..299 && (
                exception.message.startsWith("Incomplete Gemini response") ||
                    exception.message.startsWith("Incomplete Gemini sentence") ||
                    exception.message.startsWith("Repeated Gemini recommendation")
                ))
    }

    // Construccion del prompt enviado a Gemini a partir de la puntuacion total y del nivel de estres detectado.
    // En este texto se define el rol del modelo, el idioma de respuesta, el tono esperado
    // y las restricciones sobre el tipo de recomendacion que debe generar.
    private fun buildPrompt(
        puntuacion: Int,
        nivelEstres: String,
        recomendacionesPrevias: List<String>,
        attempt: Int
    ): String {
        val recomendacionesPreviasTexto = if (recomendacionesPrevias.isEmpty()) {
            "No hay recomendaciones previas registradas para este usuario."
        } else {
            recomendacionesPrevias.joinToString(separator = "\n") { "- $it" }
        }
        val enfoquePrincipal = promptFocusForAttempt(nivelEstres, attempt)

        return """
            Eres un asistente de bienestar para una aplicacion movil academica de monitorizacion del estres.
            Responde solo en espanol.
            No hagas diagnosticos medicos.
            No uses tono alarmista.
            No saludes ni empieces con frases como "Hola" o "Entiendo que".
            Empieza directamente por la recomendacion.
            Da una recomendacion personalizada breve, clara y util.
            Devuelve exactamente dos frases completas.
            Cada frase debe quedar cerrada y la ultima debe terminar en punto.
            No dejes frases a medias ni acabes en conectores como "como", "y" o "para".
            Incluye habitos saludables, respiracion, organizacion del tiempo o descanso si encaja con el nivel detectado.
            Si el nivel es alto, menciona de forma prudente que podria consultar con un profesional si el malestar persiste.
            Devuelve un unico parrafo completo de entre 60 y 90 palabras.
            Evita repetir literalmente o casi literalmente recomendaciones previas del mismo usuario.
            Si la idea base es parecida, reformulala y cambia el consejo principal para aportar variedad.
            En esta respuesta, el consejo principal debe centrarse especialmente en: $enfoquePrincipal.
            ${retryInstructionForAttempt(attempt)}

            Puntuacion PSS-14: $puntuacion
            Nivel de estres: $nivelEstres
            Recomendaciones previas a evitar:
            $recomendacionesPreviasTexto
        """.trimIndent()
    }

    // Selecciona un enfoque distinto entre intentos para forzar mas variedad en la salida de Gemini.
    private fun promptFocusForAttempt(nivelEstres: String, attempt: Int): String {
        val focuses = when (nivelEstres) {
            "Estr\u00e9s bajo" -> listOf(
                "mantener rutinas saludables y constancia",
                "actividad fisica suave y descanso",
                "ocio reparador y equilibrio estudio-descanso"
            )
            "Estr\u00e9s moderado" -> listOf(
                "organizacion del tiempo y priorizacion de tareas",
                "respiracion consciente y pausas activas",
                "higiene del sueno y reduccion de sobrecarga diaria"
            )
            else -> listOf(
                "descanso y reduccion inmediata de la exigencia",
                "tecnicas de relajacion y apoyo del entorno cercano",
                "buscar ayuda profesional si el malestar persiste"
            )
        }

        return focuses[attempt % focuses.size]
    }

    // Aumenta ligeramente la creatividad en reintentos para favorecer respuestas distintas pero estables.
    private fun temperatureForAttempt(attempt: Int): Double {
        return when (attempt) {
            0 -> 0.45
            1 -> 0.6
            2 -> 0.75
            3 -> 0.85
            else -> 0.9
        }
    }

    // Refuerza las instrucciones cuando ya hubo intentos previos no validos.
    private fun retryInstructionForAttempt(attempt: Int): String {
        return when (attempt) {
            0 -> "Aporta un consejo practico y concreto sin repetir formulaciones previas."
            1 -> "Importante: la recomendacion debe ser distinta de las anteriores y cerrar ambas frases con naturalidad."
            2 -> "Cambia claramente el enfoque principal respecto a respuestas previas y evita expresiones genericas repetidas."
            3 -> "Redacta una recomendacion valida, completa y diferente, priorizando claridad y variedad sobre formulas habituales."
            else -> "Ultimo intento: genera una recomendacion completa, concreta y no repetida, con dos frases bien cerradas y sin copiar estructuras previas."
        }
    }

    // Comprueba si la nueva recomendacion coincide casi por completo con alguna recomendacion reciente.
    private fun isDuplicateRecommendation(
        recommendation: String,
        previousRecommendations: List<String>
    ): Boolean {
        if (previousRecommendations.isEmpty()) {
            return false
        }

        val normalizedRecommendation = normalizeForComparison(recommendation)
        return previousRecommendations.any { previous ->
            val normalizedPrevious = normalizeForComparison(previous)
            normalizedPrevious == normalizedRecommendation ||
                normalizedPrevious.contains(normalizedRecommendation) ||
                normalizedRecommendation.contains(normalizedPrevious)
        }
    }

    // Normaliza un texto para comparar similitud sin depender de mayusculas o signos.
    private fun normalizeForComparison(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // Devuelve una recomendacion local distinta de las recientes siempre que exista alguna alternativa disponible.
    private fun selectNonRepeatedFallback(
        candidates: List<String>,
        previousRecommendations: List<String>
    ): String {
        return candidates.firstOrNull { candidate ->
            !isDuplicateRecommendation(candidate, previousRecommendations)
        } ?: candidates.first()
    }

    // Estructura auxiliar que permite indicar si el texto proviene de Gemini o del fallback local.
    data class RecommendationResult(
        val texto: String,
        val generadaPorGemini: Boolean,
        val aviso: String?
    )

    // Excepcion interna para manejar de forma uniforme errores de la API.
    private data class GeminiApiException(
        val code: Int,
        override val message: String
    ) : Exception(message)

    companion object {
        // Constantes de configuracion utilizadas por la comunicacion con Gemini.
        private const val TAG = "AIRecommendationService"
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent"
        private const val MAX_RETRIES = 5
        private const val MIN_RECOMMENDATION_LENGTH = 45
        private const val MIN_RECOMMENDATION_LENGTH_HARD = 25
        private const val MIN_RECOVERED_RECOMMENDATION_LENGTH = 35
        private val RETRY_DELAYS_MS = listOf(1500L, 3000L, 5000L, 8000L, 12000L)
        private val DANGLING_ENDINGS = setOf(
            "como",
            "y",
            "e",
            "o",
            "u",
            "para",
            "con",
            "sin",
            "de",
            "del",
            "la",
            "el",
            "los",
            "las",
            "por",
            "que",
            "si"
        )
        private val LOW_STRESS_FALLBACKS = listOf(
            "Tu nivel de estr\u00e9s es bajo. Mant\u00e9n tus h\u00e1bitos saludables, reserva momentos de descanso y sigue dedicando tiempo a actividades que te ayuden a sentir equilibrio y bienestar.",
            "Tu nivel de estr\u00e9s es bajo. Te conviene conservar una rutina estable de sue\u00f1o, movimiento diario y pausas breves para mantener este estado de bienestar.",
            "Tu nivel de estr\u00e9s es bajo. Contin\u00faa reforzando las conductas que ya te funcionan, como organizar bien tu jornada, descansar lo suficiente y cuidar tus espacios de ocio."
        )
        private val MODERATE_STRESS_FALLBACKS = listOf(
            "Tu nivel de estr\u00e9s es moderado. Intenta introducir pausas activas, ejercicios de respiraci\u00f3n y una mejor organizaci\u00f3n del tiempo para reducir la carga diaria.",
            "Tu nivel de estr\u00e9s es moderado. Puede ayudarte dividir las tareas grandes en pasos peque\u00f1os, hacer descansos breves cada cierto tiempo y evitar acumular todo al final del d\u00eda.",
            "Tu nivel de estr\u00e9s es moderado. Prueba a combinar respiraci\u00f3n consciente, una rutina de sue\u00f1o m\u00e1s regular y momentos de desconexi\u00f3n para aliviar la tensi\u00f3n acumulada."
        )
        private val HIGH_STRESS_FALLBACKS = listOf(
            "Tu nivel de estr\u00e9s es alto. Prioriza el descanso, utiliza t\u00e9cnicas de relajaci\u00f3n como respiraci\u00f3n guiada o mindfulness y valora consultar con un profesional si esta situaci\u00f3n persiste.",
            "Tu nivel de estr\u00e9s es alto. Intenta reducir la sobrecarga de forma inmediata, reservar tiempo real para recuperarte y apoyarte en estrategias de relajaci\u00f3n que puedas repetir cada d\u00eda.",
            "Tu nivel de estr\u00e9s es alto. Si notas que el malestar se mantiene, adem\u00e1s de descansar y bajar el ritmo, puede ser conveniente buscar orientaci\u00f3n profesional para recibir apoyo."
        )
    }
}
