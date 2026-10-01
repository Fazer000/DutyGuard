package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.utils.NotificationHelper
import com.example.utils.VibrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Acquire WakeLock to keep CPU awake during Pocket Vibration when screen is off
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val wakeLock = powerManager?.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "MilitaryDuty:VibrationWakeLock"
        )
        wakeLock?.acquire(6000L) // 6 seconds duration is sufficient for the vibration pattern

        val dutyId = intent.getLongExtra("dutyId", -1L)
        val presetId = intent.getIntExtra("presetId", -1)
        val targetTime = intent.getLongExtra("targetTime", -1L)
        val title = intent.getStringExtra("title") ?: "Задача в наряде"

        Log.d("TaskAlarmReceiver", "Received alarm trigger for: $title, dutyId=$dutyId, targetTime=$targetTime")

        if (dutyId == -1L || presetId == -1 || targetTime == -1L) {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
            }
            return
        }

        val disableInFreeShift = intent.getBooleanExtra("disableInFreeShift", false)

        // 3. Show non-ongoing notification and update database asynchronously
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                
                // Update vibration state in database
                db.taskOccurrenceDao().updateVibrated(dutyId, presetId, targetTime, true)
                
                // Fetch current duty to check shift muting rules
                val activeDuty = db.dutyDao().getActiveDuty()
                
                var shouldAlarm = true
                if (activeDuty != null && disableInFreeShift) {
                    val ownerIndex = activeDuty.deviceOwnerIndex
                    if (ownerIndex > 0) {
                        // Calculate shift segments
                        val segments = com.example.data.ShiftSegment.getShiftSegments(activeDuty)
                        val activeSegment = segments.find { targetTime >= it.startMillis && targetTime < it.endMillis }
                        if (activeSegment != null) {
                            val activeGuard = activeSegment.activeGuardIndex
                            if (ownerIndex != activeGuard) {
                                shouldAlarm = false
                                Log.d("TaskAlarmReceiver", "Muting alarm for $title: Device owner ($ownerIndex) is resting (active guard is $activeGuard).")
                            }
                        }
                    }
                }
                
                if (shouldAlarm) {
                    // Trigger the physical long pocket vibration pattern
                    VibrationHelper.triggerLongVibration(context)
                    
                    val dutyTitle = activeDuty?.title ?: "Суточный наряд"
                    // Show/update single non-ongoing notification alerting the user to this task
                    NotificationHelper.showTaskNotification(
                        context,
                        dutyTitle,
                        "Время выполнения: $title"
                    )
                }
            } catch (e: Exception) {
                Log.e("TaskAlarmReceiver", "Error saving vibration update to db/notification: ${e.message}")
            } finally {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
                pendingResult.finish()
            }
        }
    }
}
