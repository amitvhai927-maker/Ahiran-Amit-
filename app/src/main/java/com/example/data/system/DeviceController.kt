package com.example.data.system

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.data.model.DeviceStatus

class DeviceController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var isTorchEnabled = false

    /**
     * Check if Call Phone permission is granted
     */
    fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Make a phone call:
     * If directCall is true and permission is granted, uses ACTION_CALL.
     * Otherwise opens the dialer with the number pre-filled using ACTION_DIAL.
     */
    fun makeCall(phoneNumber: String, directCall: Boolean = true): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) return false

        return try {
            val intent = if (directCall && hasCallPermission()) {
                Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to dial
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
                true
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }
        }
    }

    /**
     * Send WhatsApp message to a specific number
     */
    fun sendWhatsAppMessage(phoneNumber: String, message: String): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "").let { num ->
            // If 10-digit Indian number without country code, prepend 91
            if (num.length == 10 && !num.startsWith("+")) "91$num"
            else num.removePrefix("+")
        }

        return try {
            val url = if (cleanNumber.isNotEmpty()) {
                "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback generic send
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    `package` = "com.whatsapp"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(shareIntent)
                true
            } catch (ex: Exception) {
                Toast.makeText(context, "WhatsApp is not installed or available", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    /**
     * Launch UPI payment app (Google Pay, PhonePe, Paytm, BHIM, etc.)
     */
    fun launchUpiPayment(
        upiId: String,
        payeeName: String,
        amount: Double,
        note: String = "Jarvis Payment"
    ): Boolean {
        return try {
            val formattedAmount = String.format(java.util.Locale.US, "%.2f", amount)
            val uriBuilder = Uri.Builder().apply {
                scheme("upi")
                authority("pay")
                appendQueryParameter("pa", upiId)
                appendQueryParameter("pn", payeeName.ifBlank { "Recipient" })
                appendQueryParameter("am", formattedAmount)
                appendQueryParameter("cu", "INR")
                if (note.isNotBlank()) {
                    appendQueryParameter("tn", note)
                }
            }

            val upiUri = uriBuilder.build()
            val upiIntent = Intent(Intent.ACTION_VIEW).apply {
                data = upiUri
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(upiIntent, "Pay ₹$formattedAmount via UPI").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No UPI payment apps found on device", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Toggle flashlight / torch
     */
    fun toggleTorch(turnOn: Boolean? = null): Boolean {
        if (cameraManager == null) return false
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return false

            val newState = turnOn ?: !isTorchEnabled
            cameraManager.setTorchMode(cameraId, newState)
            isTorchEnabled = newState
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isTorchOn(): Boolean = isTorchEnabled

    /**
     * Adjust audio volume
     */
    fun adjustVolume(increase: Boolean) {
        audioManager?.let { am ->
            val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        }
    }

    fun setMuteVolume() {
        audioManager?.let { am ->
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
        }
    }

    /**
     * Launch Camera
     */
    fun openCamera(): Boolean {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    /**
     * Open WiFi Settings
     */
    fun openWifiSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            openSystemSettings()
        }
    }

    /**
     * Open Bluetooth Settings
     */
    fun openBluetoothSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            openSystemSettings()
        }
    }

    /**
     * Open General System Settings
     */
    fun openSystemSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Set Alarm
     */
    fun setAlarm(hour: Int, minute: Int, message: String = "Jarvis Alarm"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to clock
            try {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    /**
     * Get Battery Info
     */
    fun getBatteryStatus(): DeviceStatus {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15

        return DeviceStatus(
            batteryLevel = batteryPct,
            isCharging = isCharging,
            isTorchOn = isTorchEnabled,
            volumeLevel = currentVol,
            maxVolume = maxVol
        )
    }

    /**
     * Launch app by friendly name or package
     */
    fun launchAppByName(appName: String): Boolean {
        val target = appName.lowercase().trim()
        val packageMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "whatsapp" to "com.whatsapp",
            "calculator" to "com.google.android.calculator",
            "chrome" to "com.android.chrome",
            "instagram" to "com.instagram.android",
            "spotify" to "com.spotify.music",
            "camera" to "com.android.camera",
            "contacts" to "com.android.contacts",
            "clock" to "com.android.deskclock",
            "settings" to "com.android.settings",
            "telegram" to "org.telegram.messenger"
        )

        // Special handling for calculator
        if (target.contains("calculator") || target.contains("hisab")) {
            return openCalculator()
        }

        // Special handling for camera
        if (target.contains("camera")) {
            return openCamera()
        }

        // Special handling for settings
        if (target.contains("settings")) {
            return openSystemSettings()
        }

        val pkg = packageMap.entries.firstOrNull { target.contains(it.key) }?.value ?: ""
        if (pkg.isNotEmpty()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return true
            }
        }

        // Web fallbacks
        if (target.contains("youtube")) {
            return searchYouTube("")
        }
        if (target.contains("map")) {
            return openGoogleMaps("")
        }
        if (target.contains("chrome") || target.contains("browser")) {
            return searchWeb("")
        }

        // Generic launch intent query
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
            val matched = resolveInfos.firstOrNull {
                it.loadLabel(context.packageManager).toString().contains(target, ignoreCase = true)
            }
            if (matched != null) {
                val appIntent = context.packageManager.getLaunchIntentForPackage(matched.activityInfo.packageName)
                if (appIntent != null) {
                    appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(appIntent)
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Toast.makeText(context, "$appName is not installed on this device", Toast.LENGTH_SHORT).show()
        return false
    }

    /**
     * Search Google Web
     */
    fun searchWeb(query: String): Boolean {
        return try {
            val url = if (query.isNotBlank()) {
                "https://www.google.com/search?q=${Uri.encode(query)}"
            } else {
                "https://www.google.com"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Search or Play on YouTube
     */
    fun searchYouTube(query: String): Boolean {
        return try {
            val intent = if (query.isNotBlank()) {
                Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                val launch = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                launch?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            }

            if (intent != null && intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                // Browser fallback
                val webUrl = if (query.isNotBlank()) {
                    "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
                } else {
                    "https://www.youtube.com"
                }
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Open Google Maps with optional location/query
     */
    fun openGoogleMaps(query: String = ""): Boolean {
        return try {
            val uri = if (query.isNotBlank()) {
                Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            } else {
                Uri.parse("geo:0,0")
            }
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
            true
        } catch (e: Exception) {
            try {
                val webUrl = "https://www.google.com/maps/search/${Uri.encode(query)}"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    /**
     * Open Calculator
     */
    fun openCalculator(): Boolean {
        val calcPackages = listOf(
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.sec.android.app.popupcalculator",
            "com.miui.calculator"
        )
        for (pkg in calcPackages) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            }
        }
        // Fallback intent
        return try {
            val intent = Intent().apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_APP_CALCULATOR)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Web calculator fallback
            searchWeb("online calculator")
        }
    }
}
