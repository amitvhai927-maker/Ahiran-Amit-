package com.example.data.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.AiPersona
import java.util.Locale

class JarvisTtsEngine(
    context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var isMuted = false
    private var currentPersona = AiPersona.JARVIS

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.let { engine ->
                val res = engine.setLanguage(Locale.US)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.getDefault())
                }
                applyPersonaVoice(currentPersona)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingStateChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        onSpeakingStateChanged(false)
                    }
                })
            }
        }
    }

    fun setPersona(persona: AiPersona) {
        currentPersona = persona
        applyPersonaVoice(persona)
    }

    private fun applyPersonaVoice(persona: AiPersona) {
        tts?.let { engine ->
            if (persona == AiPersona.MAYA) {
                // Maya: melodic, female-frequency pitch, sweet cadence
                engine.setPitch(1.30f)
                engine.setSpeechRate(1.05f)
            } else {
                // Jarvis: crisp, slightly deeper tech butler
                engine.setPitch(1.00f)
                engine.setSpeechRate(1.00f)
            }
        }
    }

    fun speak(text: String) {
        if (isMuted || !isInitialized) return

        // Clean any markdown formatting or stars
        val cleanedText = text
            .replace(Regex("\\*\\*|\\*|_|`"), "")
            .replace(Regex("https?://\\S+"), "link")
            .take(300) // Keep spoken responses concise and punchy

        tts?.speak(
            cleanedText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "JARVIS_${System.currentTimeMillis()}"
        )
    }

    fun stop() {
        tts?.stop()
        onSpeakingStateChanged(false)
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        if (isMuted) stop()
        return isMuted
    }

    fun isMuted(): Boolean = isMuted

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
