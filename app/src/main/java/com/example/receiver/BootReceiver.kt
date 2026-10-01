package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device reboot completed, rescheduling active duty alarms...")
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val activeDuty = db.dutyDao().getActiveDuty()
                    if (activeDuty != null) {
                        val occurrences = db.taskOccurrenceDao().getOccurrencesForDuty(activeDuty.id)
                        val now = System.currentTimeMillis()
                        var scheduledCount = 0
                        
                        for (occurrence in occurrences) {
                            if (!occurrence.isCompleted && occurrence.targetTimeEpochMillis > now) {
                                AlarmScheduler.scheduleAlarm(context, occurrence)
                                scheduledCount++
                            }
                        }
                        Log.d("BootReceiver", "Rescheduled $scheduledCount alarms for active duty: ${activeDuty.title}")
                    } else {
                        Log.d("BootReceiver", "No active duty found to reschedule alarms for.")
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
