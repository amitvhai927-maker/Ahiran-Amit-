package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DeviceStatus
import com.example.ui.theme.JarvisAlert
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisLaserBlue
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisWarning

@Composable
fun QuickCallDialog(
    onInitiateCall: (phoneNumber: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("quick_call_dialog"),
            color = JarvisSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(JarvisCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = JarvisCyan)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CALL INTERFACE",
                            color = JarvisCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Initiate phone call protocol",
                            color = JarvisTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. +91 98765 43210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("call_phone_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisSurfaceVariant,
                        focusedLabelColor = JarvisCyan,
                        cursorColor = JarvisCyan
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contact Name (Optional)") },
                    placeholder = { Text("e.g. Mom, Amit") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("call_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisSurfaceVariant,
                        focusedLabelColor = JarvisCyan,
                        cursorColor = JarvisCyan
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisTextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (phoneNumber.isNotBlank()) {
                                onInitiateCall(phoneNumber.trim(), contactName.trim())
                            }
                        },
                        enabled = phoneNumber.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("call_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black)
                    ) {
                        Text("Call", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickWhatsAppDialog(
    onInitiateMessage: (phoneNumber: String, message: String) -> Unit,
    onDismiss: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, JarvisGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("quick_whatsapp_dialog"),
            color = JarvisSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(JarvisGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = JarvisGreen)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "WHATSAPP DISPATCH",
                            color = JarvisGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Send message via WhatsApp",
                            color = JarvisTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("WhatsApp Number") },
                    placeholder = { Text("e.g. 9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("whatsapp_phone_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisGreen,
                        unfocusedBorderColor = JarvisSurfaceVariant,
                        focusedLabelColor = JarvisGreen,
                        cursorColor = JarvisGreen
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message Content") },
                    placeholder = { Text("Type your message here...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("whatsapp_message_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisGreen,
                        unfocusedBorderColor = JarvisSurfaceVariant,
                        focusedLabelColor = JarvisGreen,
                        cursorColor = JarvisGreen
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisTextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (message.isNotBlank()) {
                                onInitiateMessage(phoneNumber.trim(), message.trim())
                            }
                        },
                        enabled = message.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("whatsapp_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisGreen, contentColor = Color.Black)
                    ) {
                        Text("Send", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPaymentDialog(
    onInitiatePayment: (upiId: String, name: String, amount: Double, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var upiId by remember { mutableStateOf("") }
    var payeeName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("100") }
    var note by remember { mutableStateOf("Jarvis AI Payment") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, JarvisWarning.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("quick_payment_dialog"),
            color = JarvisSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(JarvisWarning.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = JarvisWarning)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "UPI PAYMENT TERMINAL",
                            color = JarvisWarning,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Supports GPay, PhonePe, Paytm, BHIM",
                            color = JarvisTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("Receiver UPI ID") },
                    placeholder = { Text("e.g. mobile@upi, username@paytm") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_upi_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisWarning,
                        unfocusedBorderColor = JarvisSurfaceVariant,
                        focusedLabelColor = JarvisWarning,
                        cursorColor = JarvisWarning
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisWarning,
                            unfocusedBorderColor = JarvisSurfaceVariant,
                            focusedLabelColor = JarvisWarning,
                            cursorColor = JarvisWarning
                        )
                    )

                    OutlinedTextField(
                        value = payeeName,
                        onValueChange = { payeeName = it },
                        label = { Text("Payee Name") },
                        placeholder = { Text("Optional") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisWarning,
                            unfocusedBorderColor = JarvisSurfaceVariant,
                            focusedLabelColor = JarvisWarning,
                            cursorColor = JarvisWarning
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("100", "200", "500", "1000").forEach { amt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (amountText == amt) JarvisWarning.copy(alpha = 0.2f) else JarvisSurfaceVariant)
                                .border(1.dp, if (amountText == amt) JarvisWarning else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { amountText = amt }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("₹$amt", color = if (amountText == amt) JarvisWarning else JarvisTextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisTextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onInitiatePayment(upiId.trim(), payeeName.trim(), amt, note.trim())
                            }
                        },
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("payment_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisWarning, contentColor = Color.Black)
                    ) {
                        Text("Pay ₹${amountText.ifBlank { "0" }}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceControlsDialog(
    status: DeviceStatus,
    onToggleTorch: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeMute: () -> Unit,
    onLaunchCamera: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, JarvisLaserBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("device_controls_dialog"),
            color = JarvisSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "DEVICE CONTROL CENTER",
                    color = JarvisLaserBlue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Hardware diagnostics and system toggles",
                    color = JarvisTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Battery status row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisSurfaceVariant)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = if (status.batteryLevel > 20) JarvisGreen else JarvisAlert
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Battery Power",
                            color = JarvisTextPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = if (status.batteryLevel >= 0) "${status.batteryLevel}% ${if (status.isCharging) "(Charging)" else ""}" else "Monitoring",
                        color = if (status.batteryLevel > 20) JarvisGreen else JarvisAlert,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Controls Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Flashlight
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (status.isTorchOn) JarvisCyan.copy(alpha = 0.2f) else JarvisSurfaceVariant)
                            .border(1.dp, if (status.isTorchOn) JarvisCyan else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleTorch)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (status.isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = null,
                                tint = if (status.isTorchOn) JarvisCyan else JarvisTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (status.isTorchOn) "Torch ON" else "Torch OFF",
                                color = if (status.isTorchOn) JarvisCyan else JarvisTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Camera
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(JarvisSurfaceVariant)
                            .clickable(onClick = onLaunchCamera)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = JarvisLaserBlue)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Camera", color = JarvisTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Volume Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Audio Volume", color = JarvisTextPrimary, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onVolumeDown, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeDown, contentDescription = "Down", tint = JarvisCyan)
                        }
                        IconButton(onClick = onVolumeMute, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeMute, contentDescription = "Mute", tint = JarvisTextSecondary)
                        }
                        IconButton(onClick = onVolumeUp, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Up", tint = JarvisCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Settings Shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(JarvisSurfaceVariant)
                            .clickable(onClick = onOpenWifi)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = JarvisLaserBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wi-Fi Settings", color = JarvisTextPrimary, fontSize = 12.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(JarvisSurfaceVariant)
                            .clickable(onClick = onOpenSettings)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = JarvisLaserBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Settings", color = JarvisTextPrimary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant)
                ) {
                    Text("Close", color = JarvisTextPrimary)
                }
            }
        }
    }
}
