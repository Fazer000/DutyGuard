package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {
    private const val TASK_NOTIFICATION_ID = 1002
    private const val TASK_CHANNEL_ID = "military_duty_tasks_alert"

    fun showTaskNotification(context: Context, dutyTitle: String, taskTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TASK_CHANNEL_ID,
                "Регулярные задачи",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о времени выполнения регулярных задач"
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, flags)

        val builder = NotificationCompat.Builder(context, TASK_CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_notification_alert)
            .setContentTitle(dutyTitle)
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false) // Can be swiped away easily
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(null) // No vibration pattern

        // Using a single constant notification ID allows new alerts to replace previous ones instead of accumulating
        notificationManager.notify(TASK_NOTIFICATION_ID, builder.build())
    }

    fun cancelTaskNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        notificationManager.cancel(TASK_NOTIFICATION_ID)
    }
}
