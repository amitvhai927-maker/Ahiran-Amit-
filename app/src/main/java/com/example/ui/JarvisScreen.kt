package com.example.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiPersona
import com.example.data.model.JarvisMessage
import com.example.data.model.JarvisState
import com.example.data.model.MessageSender
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.components.AppLauncherDialog
import com.example.ui.components.ArcReactor
import com.example.ui.components.DeviceControlsDialog
import com.example.ui.components.NotesDialog
import com.example.ui.components.QuickCallDialog
import com.example.ui.components.QuickPaymentDialog
import com.example.ui.components.QuickWhatsAppDialog
import com.example.ui.theme.JarvisAlert
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisLaserBlue
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisWarning
import java.util.Locale

@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val persona by viewModel.persona.collectAsState()
    val jarvisState by viewModel.jarvisState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val isTtsMuted by viewModel.isTtsMuted.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val showNotesSheet by viewModel.showNotesSheet.collectAsState()
    val showAppLauncher by viewModel.showAppLauncher.collectAsState()
    val speechPartialText by viewModel.speechPartialText.collectAsState()
    val speechRmsLevel by viewModel.speechRmsLevel.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Dialogs state
    var showCallDialog by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeviceDialog by remember { mutableStateOf(false) }

    val accentColor = if (persona == AiPersona.MAYA) Color(0xFFFF2A85) else JarvisCyan

    // Fallback Speech Recognizer Intent Launcher
    val speechFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.setListening(false)
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.processCommand(spokenText)
            }
        }
    }

    // Audio Permission Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (viewModel.isSpeechRecognitionAvailable()) {
                viewModel.startVoiceRecognition()
            } else {
                startSpeechRecognitionFallback(speechFallbackLauncher, viewModel, persona)
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice commands", Toast.LENGTH_SHORT).show()
        }
    }

    // Unified voice trigger using SpeechRecognizer API
    fun triggerVoiceInput() {
        if (jarvisState == JarvisState.LISTENING) {
            viewModel.stopVoiceRecognition()
            return
        }

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) {
            if (viewModel.isSpeechRecognitionAvailable()) {
                viewModel.startVoiceRecognition()
            } else {
                startSpeechRecognitionFallback(speechFallbackLauncher, viewModel, persona)
            }
        } else {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // Auto-scroll list when message added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("jarvis_screen_scaffold"),
        containerColor = JarvisBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Top HUD Bar with Persona Switcher
            JarvisTopBar(
                persona = persona,
                onTogglePersona = {
                    val next = if (persona == AiPersona.JARVIS) AiPersona.MAYA else AiPersona.JARVIS
                    viewModel.setPersona(next)
                },
                deviceStatus = deviceStatus,
                isTtsMuted = isTtsMuted,
                onToggleMute = { viewModel.toggleMuteTts() },
                onOpenControls = { showDeviceDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // 2. Central Arc Reactor with Persona theme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                ArcReactor(
                    state = jarvisState,
                    persona = persona,
                    rmsLevel = speechRmsLevel,
                    partialSpeechText = speechPartialText,
                    onClick = { triggerVoiceInput() }
                )
            }

            // 3. Quick Action Chips Row (expanded with Notes, Apps, etc.)
            QuickActionChipsRow(
                persona = persona,
                onNotesClick = { viewModel.setShowNotesSheet(true) },
                onAppsClick = { viewModel.setShowAppLauncher(true) },
                onYouTubeClick = { viewModel.deviceController.searchYouTube("") },
                onCalculatorClick = { viewModel.deviceController.openCalculator() },
                onCallClick = { showCallDialog = true },
                onWhatsAppClick = { showWhatsAppDialog = true },
                onPaymentClick = { showPaymentDialog = true },
                onTorchClick = { viewModel.toggleTorchDirect() },
                onBatteryClick = { viewModel.processCommand("battery status") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // 4. Conversation Feed / Terminal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(JarvisSurface.copy(alpha = 0.85f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .testTag("messages_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message = message, activePersona = persona)
                    }
                }
            }

            // 5. Command Suggestion Prompts
            CommandSuggestionsRow(
                persona = persona,
                onSuggestionClick = { command ->
                    viewModel.processCommand(command)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            )

            // 6. Bottom Input & Voice Console
            BottomInputBar(
                persona = persona,
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.processCommand(inputText)
                        inputText = ""
                    }
                },
                onMicClick = {
                    triggerVoiceInput()
                },
                isListening = jarvisState == JarvisState.LISTENING,
                partialSpeechText = speechPartialText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }

    // Confirmation Dialog ("persimmon ke baad")
    pendingAction?.let { action ->
        ActionConfirmationDialog(
            action = action,
            onConfirm = {
                viewModel.confirmPendingAction()
            },
            onDismiss = {
                viewModel.dismissPendingAction()
            }
        )
    }

    // Notes Sheet Dialog
    if (showNotesSheet) {
        NotesDialog(
            notes = notes,
            onAddNote = { title, content -> viewModel.addNote(title, content) },
            onDeleteNote = { id -> viewModel.deleteNote(id) },
            onDismiss = { viewModel.setShowNotesSheet(false) },
            persona = persona
        )
    }

    // App Launcher Dialog
    if (showAppLauncher) {
        AppLauncherDialog(
            onLaunchApp = { appName -> viewModel.deviceController.launchAppByName(appName) },
            onSearchWeb = { q -> viewModel.deviceController.searchWeb(q) },
            onPlayYouTube = { q -> viewModel.deviceController.searchYouTube(q) },
            onDismiss = { viewModel.setShowAppLauncher(false) },
            persona = persona
        )
    }

    // Direct Action Dialogs
    if (showCallDialog) {
        QuickCallDialog(
            onInitiateCall = { phone, name ->
                showCallDialog = false
                viewModel.initiateDirectCall(phone, name)
            },
            onDismiss = { showCallDialog = false }
        )
    }

    if (showWhatsAppDialog) {
        QuickWhatsAppDialog(
            onInitiateMessage = { phone, msg ->
                showWhatsAppDialog = false
                viewModel.initiateDirectWhatsApp(phone, msg)
            },
            onDismiss = { showWhatsAppDialog = false }
        )
    }

    if (showPaymentDialog) {
        QuickPaymentDialog(
            onInitiatePayment = { upi, name, amt, note ->
                showPaymentDialog = false
                viewModel.initiateDirectPayment(upi, name, amt, note)
            },
            onDismiss = { showPaymentDialog = false }
        )
    }

    if (showDeviceDialog) {
        DeviceControlsDialog(
            status = deviceStatus,
            onToggleTorch = { viewModel.toggleTorchDirect() },
            onVolumeUp = { viewModel.volumeUpDirect() },
            onVolumeDown = { viewModel.volumeDownDirect() },
            onVolumeMute = { viewModel.volumeMuteDirect() },
            onLaunchCamera = {
                showDeviceDialog = false
                viewModel.launchCameraDirect()
            },
            onOpenWifi = {
                showDeviceDialog = false
                viewModel.openWifiDirect()
            },
            onOpenSettings = {
                showDeviceDialog = false
                viewModel.openSettingsDirect()
            },
            onDismiss = { showDeviceDialog = false }
        )
    }
}

private fun startSpeechRecognitionFallback(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    viewModel: JarvisViewModel,
    persona: AiPersona
) {
    try {
        viewModel.setListening(true)
        val prompt = if (persona == AiPersona.MAYA) "Maya sun rahi hai, boliye..." else "JARVIS is listening. Speak command..."
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        launcher.launch(intent)
    } catch (e: Exception) {
        viewModel.setListening(false)
    }
}

@Composable
fun JarvisTopBar(
    persona: AiPersona,
    onTogglePersona: () -> Unit,
    deviceStatus: com.example.data.model.DeviceStatus,
    isTtsMuted: Boolean,
    onToggleMute: () -> Unit,
    onOpenControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (persona == AiPersona.MAYA) Color(0xFFFF2A85) else JarvisCyan

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App title & system badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (persona == AiPersona.MAYA) "M.A.Y.A." else "J.A.R.V.I.S.",
                    color = accentColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (persona == AiPersona.MAYA) "INTELLIGENT ASSISTANT" else "PERSONAL AI SYSTEM",
                    color = JarvisTextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // Persona toggle & Telemetry buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Persona Switch Chip Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.18f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onTogglePersona)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("persona_toggle_chip"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (persona == AiPersona.MAYA) "🌸 MAYA" else "⚡ JARVIS",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Battery badge
            if (deviceStatus.batteryLevel >= 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisSurfaceVariant)
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = if (deviceStatus.batteryLevel > 20) JarvisGreen else JarvisAlert,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${deviceStatus.batteryLevel}%",
                        color = JarvisTextPrimary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Audio TTS Mute/Unmute
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(JarvisSurfaceVariant)
                    .testTag("mute_tts_button")
            ) {
                Icon(
                    imageVector = if (isTtsMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Voice Speech",
                    tint = if (isTtsMuted) JarvisTextMuted else accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Settings / Device controls
            IconButton(
                onClick = onOpenControls,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(JarvisSurfaceVariant)
                    .testTag("open_device_controls_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Device Control",
                    tint = JarvisLaserBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionChipsRow(
    persona: AiPersona,
    onNotesClick: () -> Unit,
    onAppsClick: () -> Unit,
    onYouTubeClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onTorchClick: () -> Unit,
    onBatteryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val accentColor = if (persona == AiPersona.MAYA) Color(0xFFFF2A85) else JarvisCyan

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickChip(
            title = if (persona == AiPersona.MAYA) "Notes" else "Logs",
            icon = Icons.Default.NoteAlt,
            color = accentColor,
            onClick = onNotesClick,
            tag = "chip_notes"
        )
        QuickChip(
            title = "Apps",
            icon = Icons.Default.Apps,
            color = JarvisLaserBlue,
            onClick = onAppsClick,
            tag = "chip_apps"
        )
        QuickChip(
            title = "YouTube",
            icon = Icons.Default.VideoLibrary,
            color = Color(0xFFFF4444),
            onClick = onYouTubeClick,
            tag = "chip_youtube"
        )
        QuickChip(
            title = "Call",
            icon = Icons.Default.Call,
            color = JarvisCyan,
            onClick = onCallClick,
            tag = "chip_call"
        )
        QuickChip(
            title = "WhatsApp",
            icon = Icons.AutoMirrored.Outlined.Chat,
            color = JarvisGreen,
            onClick = onWhatsAppClick,
            tag = "chip_whatsapp"
        )
        QuickChip(
            title = "Pay UPI",
            icon = Icons.Default.Payment,
            color = JarvisWarning,
            onClick = onPaymentClick,
            tag = "chip_pay"
        )
        QuickChip(
            title = "Calculator",
            icon = Icons.Default.Calculate,
            color = JarvisLaserBlue,
            onClick = onCalculatorClick,
            tag = "chip_calculator"
        )
        QuickChip(
            title = "Flashlight",
            icon = Icons.Default.FlashlightOn,
            color = JarvisLaserBlue,
            onClick = onTorchClick,
            tag = "chip_torch"
        )
        QuickChip(
            title = "Battery",
            icon = Icons.Default.BatteryChargingFull,
            color = JarvisGreen,
            onClick = onBatteryClick,
            tag = "chip_battery"
        )
    }
}

@Composable
fun QuickChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = title, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MessageBubble(
    message: JarvisMessage,
    activePersona: AiPersona
) {
    val isUser = message.sender == MessageSender.USER
    val persona = message.persona
    val personaColor = if (persona == AiPersona.MAYA) Color(0xFFFF2A85) else JarvisCyan

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = if (isUser) "COMMAND" else if (persona == AiPersona.MAYA) "MAYA AI" else "JARVIS AI",
                color = if (isUser) JarvisLaserBlue else personaColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) JarvisCyan.copy(alpha = 0.18f)
                    else JarvisSurfaceVariant
                )
                .border(
                    1.dp,
                    if (isUser) JarvisCyan.copy(alpha = 0.6f) else personaColor.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = JarvisTextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun CommandSuggestionsRow(
    persona: AiPersona,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val suggestions = remember(persona) {
        if (persona == AiPersona.MAYA) {
            listOf(
                "Maya kon ho?",
                "Note banao: Grocery shopping",
                "YouTube par song sunao",
                "Call 9876543210",
                "WhatsApp message to 9876543210 Hello",
                "Pay ₹100 via UPI",
                "Torch chalu karo",
                "Battery kitni hai?",
                "Ek achha joke sunao",
                "Subhabichar sunao",
                "Switch to Jarvis"
            )
        } else {
            listOf(
                "Switch to Maya",
                "Note: Meeting at 3 PM",
                "Call 9876543210",
                "WhatsApp message to 9876543210 Hello",
                "Pay ₹500 via UPI",
                "Open YouTube",
                "Open Calculator",
                "Torch on",
                "Battery status",
                "Volume up",
                "Open camera",
                "Who are you?"
            )
        }
    }

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        suggestions.forEach { prompt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceVariant.copy(alpha = 0.6f))
                    .clickable { onSuggestionClick(prompt) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = prompt,
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun BottomInputBar(
    persona: AiPersona,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier,
    partialSpeechText: String = ""
) {
    val accentColor = if (persona == AiPersona.MAYA) Color(0xFFFF2A85) else JarvisCyan

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Voice mic button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isListening) JarvisGreen else accentColor.copy(alpha = 0.15f)
                )
                .border(1.5.dp, if (isListening) JarvisGreen else accentColor, CircleShape)
                .clickable(onClick = onMicClick)
                .testTag("mic_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Voice Input",
                tint = if (isListening) Color.Black else accentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Text input field with real-time speech preview
        OutlinedTextField(
            value = if (isListening && partialSpeechText.isNotBlank()) partialSpeechText else inputText,
            onValueChange = onInputChange,
            placeholder = {
                Text(
                    text = if (isListening) {
                        if (persona == AiPersona.MAYA) "Maya sun rahi hai..." else "Listening..."
                    } else {
                        if (persona == AiPersona.MAYA) "Boliye ya command likhiye..." else "Speak or enter command..."
                    },
                    color = JarvisTextMuted,
                    fontSize = 13.sp
                )
            },
            singleLine = true,
            readOnly = isListening,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier
                .weight(1f)
                .testTag("command_input_field"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = JarvisCardBorder,
                focusedContainerColor = JarvisSurface,
                unfocusedContainerColor = JarvisSurface,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary,
                cursorColor = accentColor
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Send button
        IconButton(
            onClick = onSend,
            enabled = inputText.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (inputText.isNotBlank()) accentColor else JarvisSurfaceVariant)
                .testTag("send_command_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Command",
                tint = if (inputText.isNotBlank()) Color.Black else JarvisTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
