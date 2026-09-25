package com.example.data.ai

import com.example.data.model.DeviceActionType
import com.example.data.model.JarvisAction

object JarvisIntentParser {

    data class ParseResult(
        val action: JarvisAction?,
        val jarvisReply: String,
        val needsConfirmation: Boolean = false
    )

    fun parseCommand(input: String): ParseResult? {
        val text = input.trim()
        val lower = text.lowercase()

        // 1. Flashlight / Torch
        if (lower.contains("torch on") || lower.contains("flashlight on") ||
            lower.contains("torch chalu") || lower.contains("torch jalao") ||
            lower.contains("flashlight chalu") || lower.contains("flash on")
        ) {
            return ParseResult(
                action = JarvisAction.DeviceAction(DeviceActionType.TORCH_ON),
                jarvisReply = "Illumination activated, sir. Torch is now ON."
            )
        }
        if (lower.contains("torch off") || lower.contains("flashlight off") ||
            lower.contains("torch band") || lower.contains("torch bujha") ||
            lower.contains("flashlight band") || lower.contains("flash off")
        ) {
            return ParseResult(
                action = JarvisAction.DeviceAction(DeviceActionType.TORCH_OFF),
                jarvisReply = "Torch deactivated, sir. Flashlight is OFF."
            )
        }
        if (lower == "torch" || lower == "flashlight" || lower.contains("torch toggle")) {
            return ParseResult(
                action = JarvisAction.DeviceAction(DeviceActionType.TORCH_TOGGLE),
                jarvisReply = "Toggling flashlight state, sir."
            )
        }

        // 2. Battery Status
        if (lower.contains("battery") || lower.contains("battery kitni") ||
            lower.contains("power status") || lower.contains("charge kitna")
        ) {
            return ParseResult(
                action = JarvisAction.DeviceAction(DeviceActionType.BATTERY_CHECK),
                jarvisReply = "Scanning power core levels, sir."
            )
        }

        // 3. Volume control
        if (lower.contains("volume up") || lower.contains("sound badhao") ||
            lower.contains("awaz badhao") || lower.contains("awaz tez") || lower.contains("louder")
        ) {
            return ParseResult(
                action = JarvisAction.DeviceAction(DeviceActionType.VOLUME_UP),
                jarvisReply = "Increasing audio levels, sir."
            )
        }
        if (lower.contains("volume down") || lower.contains("sound kam") ||
            lower.contains("awaz kam") || lower.contains("awaz dheemi") || lower.contains("quieter")
        ) {
            return ParseResult(
                action = JarvisAction.DeviceAction(DeviceActionType.VOLUME_DOWN),
                jarvisReply = "Lowering audio levels, sir."
            )
        }
        if (lower.contains("mute") || lower.contains("silent") || lower.contains("awaz band")) {
            return ParseResult(
                action = JarvisAction.DeviceAction(DeviceActionType.VOLUME_MUTE),
                jarvisReply = "Media audio muted, sir."
            )
        }

        // 4. Camera
        if (lower.contains("open camera") || lower.contains("camera chalu") ||
            lower.contains("camera open") || lower.contains("photo khincho") ||
            lower.contains("take photo")
        ) {
            return ParseResult(
                action = JarvisAction.DeviceAction(DeviceActionType.CAMERA_OPEN),
                jarvisReply = "Opening camera sensor array, sir."
            )
        }

        // 5. Settings: WiFi, Bluetooth
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            return ParseResult(
                action = JarvisAction.DeviceAction(DeviceActionType.WIFI_SETTINGS),
                jarvisReply = "Opening Wi-Fi configuration, sir."
            )
        }
        if (lower.contains("bluetooth")) {
            return ParseResult(
                action = JarvisAction.DeviceAction(DeviceActionType.BLUETOOTH_SETTINGS),
                jarvisReply = "Opening Bluetooth protocols, sir."
            )
        }

        // 6. Phone Call
        // Match numbers or names: "call 9876543210", "call Amit", "call karo 9876543210", "phone lagao 9876543210"
        val callRegex = Regex(
            "(?:call|phone lagao|dial|call karo|phone karo|call to)\\s+([a-zA-Z0-9+\\s]+)",
            RegexOption.IGNORE_CASE
        )
        val callMatch = callRegex.find(lower)
        if (callMatch != null) {
            val rawTarget = callMatch.groupValues[1].trim()
            val phoneDigits = rawTarget.replace(Regex("[^0-9+]"), "")
            val targetName = if (phoneDigits.length < 5) rawTarget else ""
            val number = if (phoneDigits.length >= 5) phoneDigits else ""

            return ParseResult(
                action = JarvisAction.CallAction(
                    phoneNumber = number.ifBlank { rawTarget },
                    contactName = targetName.ifBlank { "Recipient" }
                ),
                jarvisReply = "Awaiting your permission to place the call to ${targetName.ifBlank { number }}, sir.",
                needsConfirmation = true
            )
        }

        // 7. WhatsApp Message
        // "whatsapp 9876543210 hello how are you", "send whatsapp message to 9876543210 saying meet me"
        // "whatsapp message karo 9876543210 ko kal milte hai"
        if (lower.contains("whatsapp") || lower.contains("whats app")) {
            val phoneExtract = Regex("(?:\\+?\\d{10,13})").find(text)?.value ?: ""
            // Extract message part
            var message = ""
            val separators = listOf("saying", "message", "ko", "bolna", "that", ":", "text")
            for (sep in separators) {
                if (lower.contains(sep)) {
                    val idx = lower.indexOf(sep) + sep.length
                    val possibleMsg = text.substring(idx).trim()
                    if (possibleMsg.isNotBlank()) {
                        message = possibleMsg.trimStart(':', ',', ' ')
                        break
                    }
                }
            }
            if (message.isBlank()) {
                // If nothing extracted, try removing whatsapp and phone
                message = text.replace(Regex("whatsapp|whats app|send|message|karo|bhejo", RegexOption.IGNORE_CASE), "")
                    .replace(phoneExtract, "").trim()
                if (message.isBlank()) message = "Hello from Jarvis!"
            }

            return ParseResult(
                action = JarvisAction.WhatsAppAction(
                    phoneNumber = phoneExtract,
                    message = message,
                    contactName = if (phoneExtract.isNotEmpty()) phoneExtract else "Contact"
                ),
                jarvisReply = "WhatsApp protocol initialized. Please confirm sending this message, sir.",
                needsConfirmation = true
            )
        }

        // 8. UPI Payment / Paisa
        // "pay 500 to rahul@upi", "send 250 rs to 9876543210@paytm", "500 rupaye bhejo", "paisa payment karo 200"
        if (lower.contains("pay") || lower.contains("paisa") || lower.contains("payment") ||
            lower.contains("rupaye") || lower.contains("rupees") || lower.contains("upi")
        ) {
            // Extract amount
            val amountRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:rs|rupees|rupaye|inr)?", RegexOption.IGNORE_CASE)
            val amountMatch = amountRegex.find(lower)
            val amount = amountMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 100.0

            // Extract UPI ID if present (e.g. abc@oksbi, 9876543210@paytm)
            val upiRegex = Regex("([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)")
            val upiMatch = upiRegex.find(text)
            val upiId = upiMatch?.value ?: ""

            return ParseResult(
                action = JarvisAction.PaymentAction(
                    upiId = upiId,
                    payeeName = if (upiId.isNotEmpty()) upiId.substringBefore("@") else "Payee",
                    amount = amount,
                    note = "Payment via Jarvis AI"
                ),
                jarvisReply = "Payment security check initiated for ₹$amount. Please review and approve with your permission, sir.",
                needsConfirmation = true
            )
        }

        // 9. Persona Switching (Maya vs Jarvis)
        if (lower.contains("switch to maya") || lower.contains("maya ban jao") ||
            lower.contains("hey maya") || lower.contains("maya mode") || lower.contains("active maya")
        ) {
            return ParseResult(
                action = JarvisAction.PersonaSwitchAction(com.example.data.model.AiPersona.MAYA),
                jarvisReply = "Namaste! Main Maya hoon. Ab main aapki sahayata karungi!"
            )
        }
        if (lower.contains("switch to jarvis") || lower.contains("jarvis ban jao") ||
            lower.contains("hey jarvis") || lower.contains("jarvis mode")
        ) {
            return ParseResult(
                action = JarvisAction.PersonaSwitchAction(com.example.data.model.AiPersona.JARVIS),
                jarvisReply = "Jarvis persona online and fully operational, sir."
            )
        }

        // 10. Notes & Reminders
        if (lower.startsWith("note banao") || lower.startsWith("note likho") ||
            lower.startsWith("add note") || lower.startsWith("note:") || lower.startsWith("note ") || lower.startsWith("remember:")
        ) {
            val noteContent = text.replace(
                Regex("^(?:note banao|note likho|add note|note|remember)(?:\\s*:\\s*|\\s+)", RegexOption.IGNORE_CASE),
                ""
            ).trim().removePrefix(":").trim()
            if (noteContent.isNotBlank()) {
                return ParseResult(
                    action = JarvisAction.NoteAction(noteContent),
                    jarvisReply = "Note saved successfully: \"$noteContent\"."
                )
            }
        }
        if (lower == "notes" || lower == "show notes" || lower.contains("notes dekhao") || lower.contains("my notes")) {
            return ParseResult(
                action = JarvisAction.DeviceAction(DeviceActionType.SHOW_NOTES),
                jarvisReply = "Opening your personal notes, sir."
            )
        }

        // 11. YouTube search & playback
        if (lower.contains("youtube") && (lower.contains("play") || lower.contains("chalao") || lower.contains("search") || lower.contains("song"))) {
            val query = text.replace(Regex("youtube|play|chalao|par|on|song|video|search", RegexOption.IGNORE_CASE), "").trim()
            return ParseResult(
                action = JarvisAction.YouTubePlay(query),
                jarvisReply = if (query.isNotBlank()) "Playing \"$query\" on YouTube." else "Opening YouTube."
            )
        }

        // 12. Google Web Search
        if (lower.startsWith("search google for") || lower.startsWith("google search") ||
            lower.startsWith("google par search karo") || lower.startsWith("search for")
        ) {
            val query = text.replace(Regex("^(?:search google for|google search|google par search karo|search for)\\s*", RegexOption.IGNORE_CASE), "").trim()
            return ParseResult(
                action = JarvisAction.WebSearch(query),
                jarvisReply = "Searching the web for \"$query\"."
            )
        }

        // 13. App Launcher (open youtube, open whatsapp, open maps, open calculator, etc.)
        if (lower.startsWith("open ") || lower.startsWith("chalu karo ") || lower.startsWith("kholo ")) {
            val targetApp = text.replace(Regex("^(?:open|chalu karo|kholo)\\s+", RegexOption.IGNORE_CASE), "").trim()
            if (targetApp.isNotBlank()) {
                return ParseResult(
                    action = JarvisAction.AppLaunch(targetApp),
                    jarvisReply = "Launching $targetApp."
                )
            }
        }

        // 14. Weather Check
        if (lower.contains("weather") || lower.contains("mausam")) {
            val city = text.replace(Regex("weather|in|today|kaisa hai|kasto chha|aaj ka|mausam|batao|check", RegexOption.IGNORE_CASE), "").trim()
            return ParseResult(
                action = JarvisAction.WeatherAction(city),
                jarvisReply = if (city.isNotBlank()) "Checking atmospheric conditions for $city." else "Retrieving current local weather telemetry."
            )
        }

        return null
    }
}
