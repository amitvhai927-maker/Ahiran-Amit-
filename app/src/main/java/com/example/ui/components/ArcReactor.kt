package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiPersona
import com.example.data.model.JarvisState
import com.example.ui.theme.JarvisAlert
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisLaserBlue
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisWarning
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactor(
    state: JarvisState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    persona: AiPersona = AiPersona.JARVIS,
    rmsLevel: Float = 0f,
    partialSpeechText: String = ""
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_anim")

    // Outer ring rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    // Inner ring reverse rotation
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    // Core pulsing scale augmented dynamically by real-time voice RMS level
    val basePulseScale by infiniteTransition.animateFloat(
        initialValue = if (state == JarvisState.LISTENING || state == JarvisState.SPEAKING) 0.92f else 0.97f,
        targetValue = if (state == JarvisState.LISTENING || state == JarvisState.SPEAKING) 1.08f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == JarvisState.LISTENING) 600 else 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val pulseScale = if (state == JarvisState.LISTENING) {
        (basePulseScale + (rmsLevel * 0.18f)).coerceIn(0.88f, 1.25f)
    } else {
        basePulseScale
    }

    // Dynamic color depending on state & persona
    val coreColor = if (persona == AiPersona.MAYA) {
        when (state) {
            JarvisState.IDLE -> Color(0xFFFF2A85) // Neon Rose Pink
            JarvisState.LISTENING -> Color(0xFF00E676) // Emerald Green
            JarvisState.THINKING -> Color(0xFFE040FB) // Violet Purple
            JarvisState.SPEAKING -> Color(0xFFFF4081) // Vibrant Rose
            JarvisState.ACTION_CONFIRMATION -> JarvisWarning
        }
    } else {
        when (state) {
            JarvisState.IDLE -> JarvisCyan
            JarvisState.LISTENING -> JarvisGreen
            JarvisState.THINKING -> JarvisLaserBlue
            JarvisState.SPEAKING -> JarvisCyan
            JarvisState.ACTION_CONFIRMATION -> JarvisWarning
        }
    }

    Column(
        modifier = modifier.testTag("arc_reactor_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .testTag("arc_reactor_button"),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Outer Tech Ring Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(outerRotation)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.width / 2 - 8.dp.toPx()

                // Thin outer boundary circle
                drawCircle(
                    color = coreColor.copy(alpha = 0.25f),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // 12 outer tech ticks
                val tickCount = 16
                for (i in 0 until tickCount) {
                    val angle = (i * (360f / tickCount)) * (Math.PI / 180.0)
                    val r1 = outerRadius - 2.dp.toPx()
                    val r2 = outerRadius - if (i % 4 == 0) 10.dp.toPx() else 5.dp.toPx()

                    val start = Offset(
                        (center.x + r1 * cos(angle)).toFloat(),
                        (center.y + r1 * sin(angle)).toFloat()
                    )
                    val end = Offset(
                        (center.x + r2 * cos(angle)).toFloat(),
                        (center.y + r2 * sin(angle)).toFloat()
                    )

                    drawLine(
                        color = if (i % 4 == 0) coreColor else coreColor.copy(alpha = 0.4f),
                        start = start,
                        end = end,
                        strokeWidth = if (i % 4 == 0) 2.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Rotating Middle Ring Canvas
            Canvas(
                modifier = Modifier
                    .size(136.dp)
                    .rotate(innerRotation)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val midRadius = size.width / 2 - 4.dp.toPx()

                // Dashed arc segments
                for (i in 0 until 6) {
                    val startAngle = i * 60f + 10f
                    drawArc(
                        color = coreColor.copy(alpha = 0.7f),
                        startAngle = startAngle,
                        sweepAngle = 40f,
                        useCenter = false,
                        topLeft = Offset(center.x - midRadius, center.y - midRadius),
                        size = androidx.compose.ui.geometry.Size(midRadius * 2, midRadius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Glowing Pulsing Center Core
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.95f),
                                coreColor.copy(alpha = 0.85f),
                                coreColor.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(2.dp, coreColor.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Core center icon/text
                Text(
                    text = when (state) {
                        JarvisState.LISTENING -> "REC"
                        JarvisState.THINKING -> "AI"
                        JarvisState.SPEAKING -> "VOX"
                        JarvisState.ACTION_CONFIRMATION -> "AUTH"
                        JarvisState.IDLE -> if (persona == AiPersona.MAYA) "MAYA" else "JARVIS"
                    },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Futuristic Status Label with Animated Audio Equalizer Bars
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(JarvisSurfaceVariant.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                .border(1.dp, coreColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Blinking status LED indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(coreColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            val statusText = when (state) {
                JarvisState.IDLE -> if (persona == AiPersona.MAYA) "MAYA AI CORE ONLINE" else "JARVIS CORE ONLINE"
                JarvisState.LISTENING -> "LISTENING TO VOICE..."
                JarvisState.THINKING -> "PROCESSING COMMAND..."
                JarvisState.SPEAKING -> if (persona == AiPersona.MAYA) "MAYA TRANSMITTING..." else "JARVIS TRANSMITTING..."
                JarvisState.ACTION_CONFIRMATION -> "AUTHORIZATION REQUIRED"
            }

            Text(
                text = statusText,
                color = coreColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            if (state == JarvisState.LISTENING) {
                Spacer(modifier = Modifier.width(8.dp))
                // Real-time audio waveform equalizer bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barHeights = listOf(
                        (6.dp + (rmsLevel * 14).dp),
                        (10.dp + (rmsLevel * 20).dp),
                        (5.dp + (rmsLevel * 12).dp),
                        (12.dp + (rmsLevel * 24).dp),
                        (8.dp + (rmsLevel * 16).dp)
                    )
                    barHeights.forEach { height ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(height)
                                .clip(RoundedCornerShape(2.dp))
                                .background(coreColor)
                        )
                    }
                }
            }
        }

        // Live real-time streaming speech-to-text transcript HUD
        if (state == JarvisState.LISTENING && partialSpeechText.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(coreColor.copy(alpha = 0.12f))
                    .border(1.dp, coreColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "🎙️ \"$partialSpeechText\"",
                    color = JarvisTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
