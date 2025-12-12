package com.lucid.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The Mode Switch - A physical interaction to toggle consciousness states.
 *
 * Mode A: EXPLORE - The internet as it is. Raw.
 * Mode B: LUCID - The filter is up. The dopamine loops are severed.
 *
 * Supports:
 * - Swipe gesture
 * - Gyroscope tilt (shake to toggle)
 * - Haptic feedback
 */
@Composable
fun ModeSwitch(
    isLucidMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Animation
    val switchOffset by animateIntOffsetAsState(
        targetValue = if (isLucidMode) IntOffset.Zero else IntOffset(100, 0),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "switchOffset"
    )

    // Haptic feedback
    fun triggerHaptic() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    // Gyroscope listener for shake detection
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        var lastShakeTime = 0L
        val shakeThreshold = 3.0f
        val shakeDebounce = 1000L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationRate = abs(event.values[0]) + abs(event.values[1]) + abs(event.values[2])
                val currentTime = System.currentTimeMillis()

                if (rotationRate > shakeThreshold && currentTime - lastShakeTime > shakeDebounce) {
                    lastShakeTime = currentTime
                    coroutineScope.launch {
                        triggerHaptic()
                        onModeChange(!isLucidMode)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        gyroscope?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Swipe state
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(200.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(isLucidMode) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(dragOffset) > 50) {
                            triggerHaptic()
                            onModeChange(dragOffset < 0)
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            }
    ) {
        // Background labels
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LUCID",
                style = MaterialTheme.typography.labelLarge,
                color = if (isLucidMode)
                    MaterialTheme.colorScheme.onBackground
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isLucidMode) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = "EXPLORE",
                style = MaterialTheme.typography.labelLarge,
                color = if (!isLucidMode)
                    MaterialTheme.colorScheme.onBackground
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (!isLucidMode) FontWeight.Bold else FontWeight.Normal
            )
        }

        // Sliding indicator
        Box(
            modifier = Modifier
                .offset { switchOffset }
                .padding(4.dp)
                .width(92.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.background)
                .clickable {
                    triggerHaptic()
                    onModeChange(!isLucidMode)
                }
        )
    }
}

/**
 * Minimal mode indicator for the status area
 */
@Composable
fun ModeIndicator(
    isLucidMode: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = if (isLucidMode) "LUCID" else "EXPLORE",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
