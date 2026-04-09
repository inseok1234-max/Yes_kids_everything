package com.ois.stickymemo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.ois.stickymemo.data.MemoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            return
        }

        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

            triggeringGeofences.forEach { geofence ->
                val memoId = geofence.requestId.toIntOrNull() ?: return@forEach

                CoroutineScope(Dispatchers.IO).launch {
                    val dao = MemoDatabase.getDatabase(context).memoDao()
                    val memo = dao.getMemoById(memoId) ?: return@launch
                    showNotification(context, memo.id, memo.title, memo.content)
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        memoId: Int,
        title: String,
        content: String
    ) {
        val channelId = "geofence_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "위치 알림 메모",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "등록한 위치에 도착했을 때 메모를 알려드립니다"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("memo_id", memoId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, memoId, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📍 ${title.ifEmpty { "위치 메모" }}")
            .setContentText(content.ifEmpty { "등록한 위치에 도착했습니다!" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(memoId, notification)
    }
}