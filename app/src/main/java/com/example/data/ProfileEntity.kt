package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val transport: String = "SSL", // SSH / SSL / WS / DNS / HTTP-CONNECT
    val host: String = "127.0.0.1",
    val port: Int = 443,
    val username: String = "",
    val password: String = "",
    val usePrivateKey: Boolean = false,
    val privateKey: String = "",
    val overrideSni: Boolean = false,
    val sniHost: String = "",
    val wsPath: String = "/stream",
    val wsSubprotocol: String = "",
    val wsHeadersJson: String = "[]",
    val useUpstreamProxy: Boolean = false,
    val proxyType: String = "HTTP", // HTTP / SOCKS5
    val proxyHost: String = "",
    val proxyPort: Int = 8080,
    val proxyAuth: String = "",
    val usePayload: Boolean = false,
    val rawPayload: String = "GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf][crlf]",
    val mtu: Int = 1400,
    val dnsMode: String = "Through tunnel", // Through tunnel / Split / Direct
    val dnsPrimary: String = "1.1.1.1",
    val dnsSecondary: String = "1.0.0.1",
    val keepAlive: Boolean = true,
    val keepAliveInterval: Int = 30, // 10 / 30 / 60
    val autoReconnect: Boolean = true,
    val maxReconnectAttempts: Int = 5,
    val logLevel: String = "Info", // Error / Warn / Info / Debug
    val headersJson: String = "[]",
    val excludedAppsPackages: String = "", // Comma-separated package names
    val flagEmoji: String = "🌐",
    val lastTestedLatencyMin: Int? = null,
    val lastTestedLatencyAvg: Int? = null,
    val lastTestedLatencyMax: Int? = null,
    val isActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
