package com.thinh.aistudybuddy.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.thinh.aistudybuddy.MainActivity

class LocalReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == "ACTION_SNOOZE") {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val snoozeIntent = Intent(context, LocalReminderReceiver::class.java).apply {
                setAction("ACTION_SHOW_REMINDER")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + 15 * 60 * 1000
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            
            Toast.makeText(context, "Reminder snoozed for 15 minutes", Toast.LENGTH_SHORT).show()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(1002)
            return
        }

        showNotification(context)
    }

    private fun showNotification(context: Context) {
        val channelId = "offline_study_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Study Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Offline study streak protection notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "quiz")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            2001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, LocalReminderReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2002,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Keep your learning streak alive!")
            .setContentText("Open Buddy to complete your daily challenge.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Start Quiz Now", openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze (15m)", snoozePendingIntent)

        notificationManager.notify(1002, builder.build())
    }
}
