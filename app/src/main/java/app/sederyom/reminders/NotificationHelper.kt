package app.sederyom.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.sederyom.MainActivity
import app.sederyom.R
import app.sederyom.domain.model.NotifyMode

class NotificationHelper(private val context: Context) {
    /** Channel settings cannot change after creation, so sound and vibrate each get their own channel. */
    fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val soundChannel = NotificationChannel(
            CHANNEL_SOUND,
            context.getString(R.string.notification_channel_sound),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { enableVibration(false) }
        val vibrateChannel = NotificationChannel(
            CHANNEL_VIBRATE,
            context.getString(R.string.notification_channel_vibrate),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
        }
        manager.createNotificationChannels(listOf(soundChannel, vibrateChannel))
    }

    @SuppressLint("MissingPermission")
    fun showReminder(notificationId: Int, title: String, text: String, mode: NotifyMode) {
        if (!canPostNotifications()) return
        ensureChannels()
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val channel = if (mode == NotifyMode.VIBRATE) CHANNEL_VIBRATE else CHANNEL_SOUND
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        return permission == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val CHANNEL_SOUND = "reminders_sound"
        const val CHANNEL_VIBRATE = "reminders_vibrate"
        val VIBRATION_PATTERN = longArrayOf(0, 300, 200, 300)
    }
}
