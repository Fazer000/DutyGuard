package com.example.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationHelper {

    /**
     * Triggers a long, strong vibration pattern to ensure the user feels it in their pocket.
     * Pattern: Start immediately (0), vibrate 1000ms, pause 500ms, vibrate 1000ms, pause 500ms, vibrate 1500ms.
     */
    fun triggerLongVibration(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            val timings = longArrayOf(0, 400, 300, 400, 300, 600)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255) // full power

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Waveform vibration with Max Amplitude (255 represents peak physical vibration power)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                
                // CRITICAL: Set alarm USAGE attributes so it bypasses DND / Doze / screen-off modes safely!
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                
                vibrator.vibrate(effect, audioAttributes)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        }
    }
}
