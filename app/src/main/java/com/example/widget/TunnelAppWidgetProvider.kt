package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.example.tunnel.TunnelEngine
import com.example.tunnel.TunnelState

class TunnelAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_TUNNEL) {
            TunnelEngine.instance.toggleConnect(context)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TunnelAppWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_TUNNEL = "com.example.ACTION_TOGGLE_TUNNEL"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val engine = TunnelEngine.instance
            val state = engine.state.value
            val profile = engine.activeProfile.value
            val duration = engine.durationSeconds.value

            val views = RemoteViews(context.packageName, R.layout.widget_tunnel)

            views.setTextViewText(R.id.widget_profile_name, profile?.name ?: "Deep Current")

            val (statusText, colorInt) = when (state) {
                is TunnelState.Connected -> "CONNECTED" to 0xFF22C55E.toInt()
                is TunnelState.Connecting -> "CONNECTING" to 0xFF22D3EE.toInt()
                is TunnelState.Disconnecting -> "CLOSING" to 0xFFA6AEBD.toInt()
                is TunnelState.Failed -> "FAILED" to 0xFFEF4444.toInt()
                is TunnelState.Idle -> "CONNECT" to 0xFF22D3EE.toInt()
            }

            views.setTextViewText(R.id.widget_status_text, statusText)
            views.setTextColor(R.id.widget_status_text, colorInt)

            val durationText = if (state is TunnelState.Connected) {
                TunnelEngine.formatDuration(duration)
            } else {
                "—"
            }
            views.setTextViewText(R.id.widget_duration_text, durationText)

            val toggleIntent = Intent(context, TunnelAppWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TUNNEL
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
