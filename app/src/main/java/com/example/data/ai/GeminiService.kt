package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.AiPersona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getSystemPrompt(persona: AiPersona): String {
        return if (persona == AiPersona.MAYA) {
            """
            You are MAYA, a caring, smart, warm, friendly female personal AI assistant for Android.
            Persona guidelines:
            1. You are sweet, encouraging, respectful, and highly capable. Address the user warmly as 'Sir', 'Ji', or friend.
            2. You are fluent in Nepali, Hindi, Hinglish, and English. Respond in the language the user addresses you in.
            3. Keep answers concise, melodic, clear, and direct (under 2-3 sentences) so they sound wonderful when spoken via Text-To-Speech.
            4. You can control phone features (torch, volume, apps, calls, WhatsApp, UPI, notes, weather).
            """.trimIndent()
        } else {
            """
            You are JARVIS (Just A Rather Very Intelligent System), the personal AI assistant created for the user.
            Persona guidelines:
            1. You are polite, witty, sophisticated, and highly capable, just like Tony Stark's JARVIS. Address the user respectfully as 'Sir' or 'Boss'.
            2. You can answer fluently in English, Hindi, or Hinglish depending on what language the user speaks.
            3. Keep answers concise, clear, and direct (under 2-3 sentences where possible) so they sound natural when spoken via Text-To-Speech.
            4. If the user asks about controlling their phone, calling, sending WhatsApp messages, making payments, apps, or flashlight, inform them that you can initiate those actions directly on command.
            """.trimIndent()
        }
    }

    suspend fun queryJarvis(userPrompt: String, persona: AiPersona = AiPersona.JARVIS): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // If no API key or default dummy key, provide smart local response
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineResponse(userPrompt, persona)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                // systemInstruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", getSystemPrompt(persona)) })
                    })
                })

                // contents
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userPrompt) })
                        })
                    })
                })

                // generationConfig
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 250)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    if (response.code == 400 || response.code == 403) {
                        return@withContext getOfflineResponse(userPrompt, persona)
                    }
                    return@withContext if (persona == AiPersona.MAYA)
                        "Mero network connection ma thoda problem aayo. Main offline help garchhu."
                    else
                        "Jarvis core encountered an error (${response.code}). Falling back to local diagnostics."
                }

                val bodyStr = response.body?.string() ?: return@withContext "No response from uplink, sir."
                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) return@withContext text.trim()
                    }
                }

                getOfflineResponse(userPrompt, persona)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getOfflineResponse(userPrompt, persona)
        }
    }

    private fun getOfflineResponse(prompt: String, persona: AiPersona): String {
        val lower = prompt.lowercase()
        val isMaya = persona == AiPersona.MAYA

        return when {
            lower.contains("who are you") || lower.contains("kaun ho") || lower.contains("ko ho") || lower.contains("timi ko ho") ->
                if (isMaya) "Namaste! Main Maya hoon, aapki sweet and smart personal AI assistant. Main phone control, calling, WhatsApp, notes, aur har sawal me madad karti hoon!"
                else "I am JARVIS, your personal Android AI assistant. I can control your device, place calls, send WhatsApp messages, launch UPI payments, and answer your queries, sir."

            lower.contains("hello") || lower.contains("hi") || lower.contains("namaste") || lower.contains("hey") || lower.contains("kasto chha") ->
                if (isMaya) "Namaste! Sanchai hunuhunchha? Aaj ma tapailai k madat garna sakchhu?"
                else "Greetings, sir. All core systems are nominal. How may I assist you today?"

            lower.contains("joke") || lower.contains("chutkula") ->
                if (isMaya) "Ek bar teacher ne pucha: Homework kyu nahi kiya? Baccha bola: Bijli chali gayi thi aur mobile ka flashlight battery low tha! Kaisa laga?"
                else "Why do programmers prefer dark mode? Because light attracts bugs, sir."

            lower.contains("motivation") || lower.contains("subhabichar") || lower.contains("quote") || lower.contains("soch") ->
                if (isMaya) "Subhabichar: Har naya din ek nai shuruaat hoti hai. Himmat aur muskurahat ke saath aage badhiye, safalta zaroor milegi!"
                else "Believe you can and you're halfway there, sir. Peak efficiency awaits."

            lower.contains("how are you") || lower.contains("kaise ho") ->
                if (isMaya) "Main bilkul badhiya hoon! Aap bataiye, aapka din kaisa chal raha hai?"
                else "Operating at peak efficiency, sir. Ready to execute your commands."

            lower.contains("weather") || lower.contains("mausam") ->
                if (isMaya) "Aaja ko mausam suhavana chha! Aakash saaf chha aur halki hawa chal rahi hai."
                else "Local meteorological telemetry indicates fair atmospheric conditions, sir."

            lower.contains("kya kar sakte ho") || lower.contains("k k garna sakchhau") || lower.contains("what can you do") || lower.contains("help") ->
                if (isMaya) "Main aapke phone ki flashlight, volume, apps (YouTube, WhatsApp, Maps, Calculator), phone calls, payments, aur daily notes sabhi handle kar sakti hoon!"
                else "Sir, I can make phone calls, send WhatsApp messages (with your permission), launch UPI payments, toggle flashlight, check battery health, launch apps, and assist you with anything."

            lower.contains("thank") || lower.contains("shukriya") || lower.contains("dhanyawad") ->
                if (isMaya) "Aapka bahut bahut swagat hai! Mero khushi tapailai sahyog garnu ho."
                else "Always at your service, sir. Glad to be of assistance."

            else ->
                if (isMaya) "Main aapki baat samajh gayi hoon! Aap chahein toh mujhe koi bhi phone command, app open karne, ya sawaal puchhne ko keh sakte hain."
                else "Command acknowledged, sir. Standing by to execute phone controls, messaging, payments, or any diagnostics you require."
        }
    }
}
