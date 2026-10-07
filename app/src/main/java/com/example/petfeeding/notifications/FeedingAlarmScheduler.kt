package com.example.petfeeding.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.petfeeding.data.FeedingTime
import java.util.Calendar

class FeedingAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    fun schedule(item: FeedingTime) {
        if (!item.enabled) { cancel(item); return }
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, item.hour); set(Calendar.MINUTE, item.minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (!after(now)) add(Calendar.DAY_OF_YEAR, 1) }
        val intent = Intent(context, FeedingAlarmReceiver::class.java).putExtra(FeedingAlarmReceiver.EXTRA_ID, item.id).putExtra(FeedingAlarmReceiver.EXTRA_HOUR, item.hour).putExtra(FeedingAlarmReceiver.EXTRA_MINUTE, item.minute)
        val pending = PendingIntent.getBroadcast(context, requestCode(item.id), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (android.os.Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pending) else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pending)
    }
    fun cancel(item: FeedingTime) { alarmManager.cancel(pendingIntent(item)) }
    private fun pendingIntent(item: FeedingTime): PendingIntent = PendingIntent.getBroadcast(context, requestCode(item.id), Intent(context, FeedingAlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: PendingIntent.getBroadcast(context, requestCode(item.id), Intent(context, FeedingAlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun requestCode(id: Long) = (id xor (id ushr 32)).toInt()
}
