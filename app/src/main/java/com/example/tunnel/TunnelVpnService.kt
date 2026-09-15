package com.example.tunnel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.DeepCurrentApp
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TunnelVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1L)
                startVpnTunnel(profileId)
            }
            ACTION_DISCONNECT -> {
                stopVpnTunnel()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpnTunnel(profileId: Long) {
        serviceScope.launch {
            val profile = (if (profileId > 0) DeepCurrentApp.instance.database.tunnelDao().getProfileById(profileId) else null)
                ?: DeepCurrentApp.instance.database.tunnelDao().getActiveProfileOnce()
                ?: return@launch

            try {
                val builder = Builder()
                    .setSession("Mr Unknown: ${profile.name}")
                    .setMtu(profile.mtu)
                    .addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0)

                // DNS
                if (profile.dnsMode == "Through tunnel" || profile.dnsMode == "Split") {
                    if (profile.dnsPrimary.isNotBlank()) builder.addDnsServer(profile.dnsPrimary)
                    if (profile.dnsSecondary.isNotBlank()) builder.addDnsServer(profile.dnsSecondary)
                }

                // Per-app split tunneling
                if (profile.excludedAppsPackages.isNotBlank()) {
                    val packages = profile.excludedAppsPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    for (pkg in packages) {
                        try {
                            builder.addDisallowedApplication(pkg)
                        } catch (e: Exception) {
                            Log.w("TunnelVpnService", "Could not exclude package: $pkg", e)
                        }
                    }
                }

                vpnInterface = builder.establish()

                val notification = buildForegroundNotification(profile.name)
                startForeground(NOTIFICATION_ID, notification)

            } catch (e: Exception) {
                Log.e("TunnelVpnService", "Failed to establish VPN interface", e)
            }
        }
    }

    private fun stopVpnTunnel() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e("TunnelVpnService", "Error closing VPN interface", e)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopVpnTunnel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mr Unknown Tunnel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN connection status and statistics"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(profileName: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, TunnelVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPending = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mr Unknown — Connected")
            .setContentText("Tunneling via $profileName")
            .setSmallIcon(com.example.R.drawable.ic_launcher_fg_art)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPending)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_CONNECT = "com.example.action.CONNECT_VPN"
        const val ACTION_DISCONNECT = "com.example.action.DISCONNECT_VPN"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        private const val CHANNEL_ID = "deep_current_vpn_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
