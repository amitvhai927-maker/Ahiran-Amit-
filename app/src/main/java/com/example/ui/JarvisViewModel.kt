package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiService
import com.example.data.ai.JarvisIntentParser
import com.example.data.db.JarvisDatabase
import com.example.data.db.JarvisHistoryEntity
import com.example.data.db.JarvisNoteEntity
import com.example.data.model.AiPersona
import com.example.data.model.DeviceActionType
import com.example.data.model.DeviceStatus
import com.example.data.model.JarvisAction
import com.example.data.model.JarvisMessage
import com.example.data.model.JarvisState
import com.example.data.model.MessageSender
import com.example.data.speech.JarvisSpeechRecognizer
import com.example.data.speech.JarvisTtsEngine
import com.example.data.system.DeviceController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getInstance(application)
    private val dao = db.jarvisDao()
    private val geminiService = GeminiService()
    val deviceController = DeviceController(application)

    private val _persona = MutableStateFlow(AiPersona.JARVIS)
    val persona: StateFlow<AiPersona> = _persona.asStateFlow()

    private val _jarvisState = MutableStateFlow(JarvisState.IDLE)
    val jarvisState: StateFlow<JarvisState> = _jarvisState.asStateFlow()

    private val _messages = MutableStateFlow<List<JarvisMessage>>(emptyList())
    val messages: StateFlow<List<JarvisMessage>> = _messages.asStateFlow()

    private val _pendingAction = MutableStateFlow<JarvisAction?>(null)
    val pendingAction: StateFlow<JarvisAction?> = _pendingAction.asStateFlow()

    private val _deviceStatus = MutableStateFlow(deviceController.getBatteryStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    private val _isTtsMuted = MutableStateFlow(false)
    val isTtsMuted: StateFlow<Boolean> = _isTtsMuted.asStateFlow()

    private val _showNotesSheet = MutableStateFlow(false)
    val showNotesSheet: StateFlow<Boolean> = _showNotesSheet.asStateFlow()

    private val _showAppLauncher = MutableStateFlow(false)
    val showAppLauncher: StateFlow<Boolean> = _showAppLauncher.asStateFlow()

    private val _speechPartialText = MutableStateFlow("")
    val speechPartialText: StateFlow<String> = _speechPartialText.asStateFlow()

    val notes: StateFlow<List<JarvisNoteEntity>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val ttsEngine = JarvisTtsEngine(application) { isSpeaking ->
        if (isSpeaking) {
            if (_jarvisState.value != JarvisState.ACTION_CONFIRMATION) {
                _jarvisState.value = JarvisState.SPEAKING
            }
        } else {
            if (_jarvisState.value == JarvisState.SPEAKING) {
                _jarvisState.value = JarvisState.IDLE
            }
        }
    }

    private val speechRecognizer = JarvisSpeechRecognizer(
        context = application,
        onListeningStateChanged = { listening ->
            if (listening) {
                ttsEngine.stop()
                _jarvisState.value = JarvisState.LISTENING
            } else if (_jarvisState.value == JarvisState.LISTENING) {
                _jarvisState.value = JarvisState.IDLE
            }
        },
        onPartialResult = { partial ->
            _speechPartialText.value = partial
        },
        onFinalResult = { finalResult ->
            _speechPartialText.value = ""
            processCommand(finalResult)
        },
        onErrorOccurred = { _ ->
            _speechPartialText.value = ""
            if (_jarvisState.value == JarvisState.LISTENING) {
                _jarvisState.value = JarvisState.IDLE
            }
        }
    )

    val speechRmsLevel: StateFlow<Float> = speechRecognizer.rmsLevel
    val isSpeechListening: StateFlow<Boolean> = speechRecognizer.isListening

    init {
        // Initial greeting
        val initialGreeting = JarvisMessage(
            text = "JARVIS systems initialized. All protocols active, sir. How may I assist you?",
            sender = MessageSender.JARVIS,
            persona = AiPersona.JARVIS
        )
        _messages.value = listOf(initialGreeting)
        speak(initialGreeting.text)
        refreshDeviceStatus()
    }

    fun setPersona(newPersona: AiPersona) {
        if (_persona.value == newPersona) return
        _persona.value = newPersona
        ttsEngine.setPersona(newPersona)

        val switchGreeting = if (newPersona == AiPersona.MAYA) {
            "Namaste! Main Maya hoon, aapki AI assistant. Main phone control, WhatsApp, calling, notes, aur har cheez me aapki madad ke liye taiyaar hoon!"
        } else {
            "Jarvis persona re-engaged, sir. Tactical HUD and voice diagnostics nominal."
        }

        val msg = JarvisMessage(
            text = switchGreeting,
            sender = MessageSender.JARVIS,
            persona = newPersona
        )
        _messages.value = _messages.value + msg
        speak(switchGreeting)
    }

    fun refreshDeviceStatus() {
        _deviceStatus.value = deviceController.getBatteryStatus()
    }

    fun setListening(isListening: Boolean) {
        if (isListening) {
            ttsEngine.stop()
            _jarvisState.value = JarvisState.LISTENING
        } else if (_jarvisState.value == JarvisState.LISTENING) {
            _jarvisState.value = JarvisState.IDLE
        }
    }

    fun startVoiceRecognition() {
        ttsEngine.stop()
        speechRecognizer.startListening(_persona.value)
    }

    fun stopVoiceRecognition() {
        speechRecognizer.stopListening()
    }

    fun cancelVoiceRecognition() {
        speechRecognizer.cancel()
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return speechRecognizer.isRecognitionAvailable()
    }

    fun toggleMuteTts() {
        val muted = ttsEngine.toggleMute()
        _isTtsMuted.value = muted
    }

    fun setShowNotesSheet(show: Boolean) {
        _showNotesSheet.value = show
    }

    fun setShowAppLauncher(show: Boolean) {
        _showAppLauncher.value = show
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            dao.insertNote(JarvisNoteEntity(title = title, content = content))
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            dao.deleteNoteById(noteId)
        }
    }

    private fun speak(text: String) {
        ttsEngine.speak(text)
    }

    fun processCommand(commandText: String) {
        val text = commandText.trim()
        if (text.isBlank()) return

        // 1. Add user message
        val userMsg = JarvisMessage(text = text, sender = MessageSender.USER, persona = _persona.value)
        _messages.value = _messages.value + userMsg

        _jarvisState.value = JarvisState.THINKING

        // 2. Parse command for intents
        val parseResult = JarvisIntentParser.parseCommand(text)

        if (parseResult != null) {
            if (parseResult.needsConfirmation && parseResult.action != null) {
                // Action requires permission / authorization
                _pendingAction.value = parseResult.action
                _jarvisState.value = JarvisState.ACTION_CONFIRMATION

                val replyMsg = JarvisMessage(
                    text = parseResult.jarvisReply,
                    sender = MessageSender.JARVIS,
                    action = parseResult.action,
                    persona = _persona.value
                )
                _messages.value = _messages.value + replyMsg
                speak(parseResult.jarvisReply)
            } else {
                // Execute direct action immediately
                executeDirectAction(parseResult.action, parseResult.jarvisReply)
            }
        } else {
            // 3. Fallback to Gemini AI query
            viewModelScope.launch {
                val aiReply = geminiService.queryJarvis(text, _persona.value)
                val jarvisMsg = JarvisMessage(text = aiReply, sender = MessageSender.JARVIS, persona = _persona.value)
                _messages.value = _messages.value + jarvisMsg
                speak(aiReply)

                // Log in database
                dao.insertHistory(
                    JarvisHistoryEntity(
                        commandText = text,
                        responseText = aiReply,
                        actionType = "CHAT",
                        actionTarget = "AI"
                    )
                )
            }
        }
    }

    private fun executeDirectAction(action: JarvisAction?, defaultReply: String) {
        var reply = defaultReply
        var actionType = "DEVICE"
        var target = ""

        when (action) {
            is JarvisAction.PersonaSwitchAction -> {
                setPersona(action.newPersona)
                return
            }
            is JarvisAction.AppLaunch -> {
                actionType = "APP_LAUNCH"
                target = action.appName
                val success = deviceController.launchAppByName(action.appName)
                reply = if (success) "Launching ${action.appName}." else "Could not locate ${action.appName} on this device."
            }
            is JarvisAction.WebSearch -> {
                actionType = "WEB_SEARCH"
                target = action.query
                deviceController.searchWeb(action.query)
                reply = "Searching Google for \"${action.query}\"."
            }
            is JarvisAction.YouTubePlay -> {
                actionType = "YOUTUBE"
                target = action.query
                deviceController.searchYouTube(action.query)
                reply = if (action.query.isNotBlank()) "Playing \"${action.query}\" on YouTube." else "Opening YouTube."
            }
            is JarvisAction.WeatherAction -> {
                actionType = "WEATHER"
                target = action.city
                viewModelScope.launch {
                    val weatherQuery = if (action.city.isNotBlank()) "Current weather in ${action.city}" else "Current local weather forecast"
                    val weatherReport = geminiService.queryJarvis(weatherQuery, _persona.value)
                    val jarvisMsg = JarvisMessage(text = weatherReport, sender = MessageSender.JARVIS, persona = _persona.value)
                    _messages.value = _messages.value + jarvisMsg
                    speak(weatherReport)
                }
                return
            }
            is JarvisAction.NoteAction -> {
                actionType = "NOTE"
                target = action.noteText
                addNote("Note", action.noteText)
                reply = if (_persona.value == AiPersona.MAYA) "Aapka note save ho gaya hai: \"${action.noteText}\"." else "Note logged into local memory, sir."
            }
            is JarvisAction.DeviceAction -> {
                when (action.type) {
                    DeviceActionType.SHOW_NOTES -> {
                        _showNotesSheet.value = true
                        reply = "Opening your personal notes."
                        actionType = "SHOW_NOTES"
                    }
                    DeviceActionType.LAUNCH_APP -> {
                        _showAppLauncher.value = true
                        reply = "Opening app launcher."
                        actionType = "APP_LAUNCHER"
                    }
                    DeviceActionType.OPEN_CALCULATOR -> {
                        deviceController.openCalculator()
                        actionType = "CALCULATOR"
                    }
                    DeviceActionType.OPEN_MAPS -> {
                        deviceController.openGoogleMaps()
                        actionType = "MAPS"
                    }
                    DeviceActionType.SEARCH_WEB -> {
                        deviceController.searchWeb(action.value)
                        actionType = "SEARCH"
                    }
                    DeviceActionType.PLAY_YOUTUBE -> {
                        deviceController.searchYouTube(action.value)
                        actionType = "YOUTUBE"
                    }
                    DeviceActionType.WEATHER_CHECK -> {
                        deviceController.searchWeb("current weather forecast")
                        actionType = "WEATHER"
                    }
                    DeviceActionType.ADD_NOTE -> {
                        addNote("Quick Note", action.value)
                        actionType = "NOTE"
                    }
                    DeviceActionType.SWITCH_PERSONA -> {
                        val targetPersona = if (_persona.value == AiPersona.JARVIS) AiPersona.MAYA else AiPersona.JARVIS
                        setPersona(targetPersona)
                        return
                    }
                    DeviceActionType.TORCH_ON -> {
                        deviceController.toggleTorch(true)
                        refreshDeviceStatus()
                        actionType = "TORCH_ON"
                    }
                    DeviceActionType.TORCH_OFF -> {
                        deviceController.toggleTorch(false)
                        refreshDeviceStatus()
                        actionType = "TORCH_OFF"
                    }
                    DeviceActionType.TORCH_TOGGLE -> {
                        deviceController.toggleTorch()
                        refreshDeviceStatus()
                        actionType = "TORCH_TOGGLE"
                    }
                    DeviceActionType.VOLUME_UP -> {
                        deviceController.adjustVolume(true)
                        actionType = "VOLUME_UP"
                    }
                    DeviceActionType.VOLUME_DOWN -> {
                        deviceController.adjustVolume(false)
                        actionType = "VOLUME_DOWN"
                    }
                    DeviceActionType.VOLUME_MUTE -> {
                        deviceController.setMuteVolume()
                        actionType = "VOLUME_MUTE"
                    }
                    DeviceActionType.CAMERA_OPEN -> {
                        deviceController.openCamera()
                        actionType = "CAMERA"
                    }
                    DeviceActionType.BATTERY_CHECK -> {
                        refreshDeviceStatus()
                        val status = _deviceStatus.value
                        reply = if (status.batteryLevel >= 0) {
                            "Battery is at ${status.batteryLevel} percent, ${if (status.isCharging) "charging now" else "discharging"}."
                        } else {
                            "Battery telemetry unavailable, sir."
                        }
                        actionType = "BATTERY"
                    }
                    DeviceActionType.WIFI_SETTINGS -> {
                        deviceController.openWifiSettings()
                        actionType = "WIFI"
                    }
                    DeviceActionType.BLUETOOTH_SETTINGS -> {
                        deviceController.openBluetoothSettings()
                        actionType = "BLUETOOTH"
                    }
                    DeviceActionType.ALARM_SET -> {
                        deviceController.setAlarm(7, 0, "Jarvis Alarm")
                        actionType = "ALARM"
                    }
                }
            }
            else -> {}
        }

        val jarvisMsg = JarvisMessage(text = reply, sender = MessageSender.JARVIS, persona = _persona.value)
        _messages.value = _messages.value + jarvisMsg
        speak(reply)

        viewModelScope.launch {
            dao.insertHistory(
                JarvisHistoryEntity(
                    commandText = defaultReply,
                    responseText = reply,
                    actionType = actionType,
                    actionTarget = target
                )
            )
        }
    }

    /**
     * User explicitly confirmed permission in the UI
     */
    fun confirmPendingAction() {
        val action = _pendingAction.value ?: return
        _pendingAction.value = null
        _jarvisState.value = JarvisState.IDLE

        var executedReply = if (_persona.value == AiPersona.MAYA) "Aapki permission mil gayi hai. Main execute kar rahi hoon!" else "Authorization verified. Executing protocol, sir."
        var actionType = "UNKNOWN"
        var target = ""

        when (action) {
            is JarvisAction.CallAction -> {
                actionType = "CALL"
                target = action.phoneNumber
                executedReply = if (_persona.value == AiPersona.MAYA)
                    "${action.contactName.ifBlank { action.phoneNumber }} ko call lagaya jaa raha hai."
                else
                    "Initiating voice transmission to ${action.contactName.ifBlank { action.phoneNumber }}, sir."
                deviceController.makeCall(action.phoneNumber, directCall = true)
            }
            is JarvisAction.WhatsAppAction -> {
                actionType = "WHATSAPP"
                target = action.phoneNumber
                executedReply = if (_persona.value == AiPersona.MAYA)
                    "WhatsApp open ho raha hai, message bhejne ke liye."
                else
                    "Opening WhatsApp to dispatch your message, sir."
                deviceController.sendWhatsAppMessage(action.phoneNumber, action.message)
            }
            is JarvisAction.PaymentAction -> {
                actionType = "PAYMENT"
                target = "${action.upiId} (₹${action.amount})"
                executedReply = if (_persona.value == AiPersona.MAYA)
                    "₹${action.amount} ke liye UPI payment gateway open kar diya gaya hai."
                else
                    "Launching secure UPI gateway for ₹${action.amount}, sir."
                deviceController.launchUpiPayment(action.upiId, action.payeeName, action.amount, action.note)
            }
            is JarvisAction.DeviceAction -> {
                executeDirectAction(action, "Executing device instruction.")
                return
            }
            else -> {
                executeDirectAction(action, "Command executed.")
                return
            }
        }

        val confirmMsg = JarvisMessage(text = executedReply, sender = MessageSender.JARVIS, persona = _persona.value)
        _messages.value = _messages.value + confirmMsg
        speak(executedReply)

        viewModelScope.launch {
            dao.insertHistory(
                JarvisHistoryEntity(
                    commandText = "Execute $actionType",
                    responseText = executedReply,
                    actionType = actionType,
                    actionTarget = target
                )
            )
        }
    }

    /**
     * User declined permission
     */
    fun dismissPendingAction() {
        _pendingAction.value = null
        _jarvisState.value = JarvisState.IDLE
        val abortMsg = JarvisMessage(
            text = if (_persona.value == AiPersona.MAYA) "Theek hai, action cancel kar diya gaya hai." else "Action aborted, sir. Standing by for next command.",
            sender = MessageSender.JARVIS,
            persona = _persona.value
        )
        _messages.value = _messages.value + abortMsg
        speak(abortMsg.text)
    }

    // Direct actions triggered from quick panels
    fun initiateDirectCall(phoneNumber: String, name: String) {
        _pendingAction.value = JarvisAction.CallAction(phoneNumber, name)
        _jarvisState.value = JarvisState.ACTION_CONFIRMATION
        val reply = if (_persona.value == AiPersona.MAYA)
            "${name.ifBlank { phoneNumber }} ko call karne ke liye aapki permission chahiye."
        else
            "Permission requested to call ${name.ifBlank { phoneNumber }}, sir."
        _messages.value = _messages.value + JarvisMessage(text = reply, sender = MessageSender.JARVIS, persona = _persona.value)
        speak(reply)
    }

    fun initiateDirectWhatsApp(phoneNumber: String, message: String) {
        _pendingAction.value = JarvisAction.WhatsAppAction(phoneNumber, message)
        _jarvisState.value = JarvisState.ACTION_CONFIRMATION
        val reply = if (_persona.value == AiPersona.MAYA)
            "WhatsApp message bhejne ke liye permission confirm karein."
        else
            "Permission requested to send WhatsApp message, sir."
        _messages.value = _messages.value + JarvisMessage(text = reply, sender = MessageSender.JARVIS, persona = _persona.value)
        speak(reply)
    }

    fun initiateDirectPayment(upiId: String, name: String, amount: Double, note: String) {
        _pendingAction.value = JarvisAction.PaymentAction(upiId, name, amount, note)
        _jarvisState.value = JarvisState.ACTION_CONFIRMATION
        val reply = if (_persona.value == AiPersona.MAYA)
            "₹$amount ka UPI payment initiate karne ke liye confirm karein."
        else
            "Permission requested to initiate UPI payment of ₹$amount, sir."
        _messages.value = _messages.value + JarvisMessage(text = reply, sender = MessageSender.JARVIS, persona = _persona.value)
        speak(reply)
    }

    fun toggleTorchDirect() {
        deviceController.toggleTorch()
        refreshDeviceStatus()
    }

    fun volumeUpDirect() = deviceController.adjustVolume(true)
    fun volumeDownDirect() = deviceController.adjustVolume(false)
    fun volumeMuteDirect() = deviceController.setMuteVolume()
    fun launchCameraDirect() = deviceController.openCamera()
    fun openWifiDirect() = deviceController.openWifiSettings()
    fun openSettingsDirect() = deviceController.openSystemSettings()

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
        ttsEngine.shutdown()
    }
}

