package com.example.data.model

enum class MessageSender {
    USER, JARVIS
}

enum class JarvisState {
    IDLE, LISTENING, THINKING, SPEAKING, ACTION_CONFIRMATION
}

enum class AiPersona {
    MAYA, JARVIS
}

enum class DeviceActionType {
    TORCH_TOGGLE,
    TORCH_ON,
    TORCH_OFF,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    CAMERA_OPEN,
    BATTERY_CHECK,
    WIFI_SETTINGS,
    BLUETOOTH_SETTINGS,
    ALARM_SET,
    LAUNCH_APP,
    SEARCH_WEB,
    PLAY_YOUTUBE,
    OPEN_MAPS,
    OPEN_CALCULATOR,
    WEATHER_CHECK,
    ADD_NOTE,
    SHOW_NOTES,
    SWITCH_PERSONA
}

sealed class JarvisAction {
    data class CallAction(
        val phoneNumber: String,
        val contactName: String = ""
    ) : JarvisAction()

    data class WhatsAppAction(
        val phoneNumber: String,
        val message: String,
        val contactName: String = ""
    ) : JarvisAction()

    data class PaymentAction(
        val upiId: String,
        val payeeName: String,
        val amount: Double,
        val note: String = "Jarvis Payment"
    ) : JarvisAction()

    data class DeviceAction(
        val type: DeviceActionType,
        val value: String = ""
    ) : JarvisAction()

    data class AppLaunch(
        val appName: String,
        val packageName: String = ""
    ) : JarvisAction()

    data class WebSearch(
        val query: String
    ) : JarvisAction()

    data class YouTubePlay(
        val query: String
    ) : JarvisAction()

    data class WeatherAction(
        val city: String
    ) : JarvisAction()

    data class NoteAction(
        val noteText: String
    ) : JarvisAction()

    data class PersonaSwitchAction(
        val newPersona: AiPersona
    ) : JarvisAction()
}

data class JarvisMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val action: JarvisAction? = null,
    val persona: AiPersona = AiPersona.JARVIS
)

data class DeviceStatus(
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val isTorchOn: Boolean = false,
    val volumeLevel: Int = 0,
    val maxVolume: Int = 15
)

