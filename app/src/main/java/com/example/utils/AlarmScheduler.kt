package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.TaskOccurrence
import com.example.receiver.TaskAlarmReceiver

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    /**
     * Generates a unique, deterministic request code for an occurrence.
     * This is crucial to reschedule/cancel the exact alarm later.
     */
    fun getRequestCode(occurrence: TaskOccurrence): Int {
        // Use a combination of target targetTimeEpochMillis and presetId as request code
        val timeSec = (occurrence.targetTimeEpochMillis / 1000).toInt()
        return timeSec + occurrence.presetId
    }

    fun scheduleAlarm(context: Context, occurrence: TaskOccurrence) {
        // Do not schedule alarms for past occurrences
        if (occurrence.targetTimeEpochMillis <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("dutyId", occurrence.dutyId)
            putExtra("presetId", occurrence.presetId)
            putExtra("targetTime", occurrence.targetTimeEpochMillis)
            putExtra("title", occurrence.title)
            putExtra("disableInFreeShift", occurrence.disableInFreeShift)
        }

        val requestCode = getRequestCode(occurrence)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        occurrence.targetTimeEpochMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        occurrence.targetTimeEpochMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    occurrence.targetTimeEpochMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for ${occurrence.title} at ${occurrence.targetTimeEpochMillis} with ReqCode $requestCode")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm, falling back to inexact alarm: ${e.message}")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                occurrence.targetTimeEpochMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, occurrence: TaskOccurrence) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val requestCode = getRequestCode(occurrence)
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            @Suppress("DEPRECATION")
            PendingIntent.FLAG_NO_CREATE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for ${occurrence.title} with ReqCode $requestCode")
        }
    }
}
