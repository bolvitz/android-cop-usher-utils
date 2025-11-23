package com.cop.app.headcounter.presentation.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cop.app.headcounter.R
import com.cop.app.headcounter.data.local.entities.IncidentEntity
import com.cop.app.headcounter.domain.models.IncidentSeverity
import com.cop.app.headcounter.presentation.MainActivity

object IncidentNotificationHelper {
    private const val CHANNEL_ID = "incident_notifications"
    private const val CHANNEL_NAME = "Incident Reports"
    private const val CHANNEL_DESCRIPTION = "Notifications for incident reports"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showIncidentNotification(context: Context, incident: IncidentEntity) {
        // Only show notifications for high and critical incidents
        val severity = IncidentSeverity.fromString(incident.severity)
        if (severity != IncidentSeverity.HIGH && severity != IncidentSeverity.CRITICAL) {
            return
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("incident_id", incident.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            incident.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val priority = when (severity) {
            IncidentSeverity.CRITICAL -> NotificationCompat.PRIORITY_MAX
            IncidentSeverity.HIGH -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${severity.displayName} Incident Reported")
            .setContentText(incident.title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${incident.title}\n\n${incident.description}")
            )
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(incident.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permission not granted, silently fail
        }
    }

    fun cancelIncidentNotification(context: Context, incidentId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(incidentId.hashCode())
        } catch (e: SecurityException) {
            // Permission not granted, silently fail
        }
    }
}
