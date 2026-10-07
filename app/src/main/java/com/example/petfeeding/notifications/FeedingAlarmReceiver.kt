package com.example.petfeeding.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.petfeeding.MainActivity
import com.example.petfeeding.R
import java.util.Calendar

class FeedingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        createChannel(context)
        val hour = intent.getIntExtra(EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val petName = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).getString("pet_name", "Pet")?.trim().orEmpty().ifBlank { "Pet" }
        val openIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pets)
            .setContentTitle("$petName • Feeding time")
            .setContentText("Feed $petName at %02d:%02d".format(hour, minute))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(intent.getLongExtra(EXTRA_ID, System.currentTimeMillis()).toInt(), notification)
        }
        val itemId = intent.getLongExtra(EXTRA_ID, 0L)
        val nextIntent = Intent(context, FeedingAlarmReceiver::class.java).apply { putExtras(intent) }
        val pending = PendingIntent.getBroadcast(context, requestCode(itemId), nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
        if (Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()) alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, next.timeInMillis, pending) else alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
    }

    private fun requestCode(id: Long) = (id xor (id ushr 32)).toInt()

    companion object {
        const val CHANNEL_ID = "feeding_reminders"
        const val EXTRA_ID = "feeding_id"
        const val EXTRA_HOUR = "feeding_hour"
        const val EXTRA_MINUTE = "feeding_minute"
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.notifications_channel_name), NotificationManager.IMPORTANCE_HIGH).apply { description = context.getString(R.string.notifications_channel_description); enableVibration(true) })
        }
    }
}
