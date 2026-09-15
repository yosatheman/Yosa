package com.example.tunnel

import com.example.data.ProfileEntity
import org.json.JSONObject
import java.net.URI

object ConfigParser {

    fun exportToJson(profile: ProfileEntity): String {
        val json = JSONObject().apply {
            put("version", 1)
            put("name", profile.name)
            put("transport", profile.transport)
            put("host", profile.host)
            put("port", profile.port)
            put("username", profile.username)
            put("password", profile.password)
            put("usePrivateKey", profile.usePrivateKey)
            put("privateKey", profile.privateKey)
            put("overrideSni", profile.overrideSni)
            put("sniHost", profile.sniHost)
            put("wsPath", profile.wsPath)
            put("wsSubprotocol", profile.wsSubprotocol)
            put("wsHeadersJson", profile.wsHeadersJson)
            put("useUpstreamProxy", profile.useUpstreamProxy)
            put("proxyType", profile.proxyType)
            put("proxyHost", profile.proxyHost)
            put("proxyPort", profile.proxyPort)
            put("proxyAuth", profile.proxyAuth)
            put("usePayload", profile.usePayload)
            put("rawPayload", profile.rawPayload)
            put("mtu", profile.mtu)
            put("dnsMode", profile.dnsMode)
            put("dnsPrimary", profile.dnsPrimary)
            put("dnsSecondary", profile.dnsSecondary)
            put("keepAlive", profile.keepAlive)
            put("keepAliveInterval", profile.keepAliveInterval)
            put("autoReconnect", profile.autoReconnect)
            put("maxReconnectAttempts", profile.maxReconnectAttempts)
            put("logLevel", profile.logLevel)
            put("headersJson", profile.headersJson)
            put("flagEmoji", profile.flagEmoji)
        }
        return json.toString(2)
    }

    fun parse(content: String): ProfileEntity? {
        val trimmed = content.trim()
        return try {
            when {
                trimmed.startsWith("{") && trimmed.endsWith("}") -> parseJson(trimmed)
                trimmed.startsWith("ssh://", ignoreCase = true) -> parseSshUri(trimmed)
                trimmed.contains("[crlf]") || trimmed.contains("HTTP Custom") || trimmed.contains(".hc") -> parseHc(trimmed)
                trimmed.contains(":") -> parsePlaintext(trimmed)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseJson(jsonStr: String): ProfileEntity {
        val json = JSONObject(jsonStr)
        return ProfileEntity(
            name = json.optString("name", "Imported Profile"),
            transport = json.optString("transport", "SSL"),
            host = json.optString("host", "127.0.0.1"),
            port = json.optInt("port", 443),
            username = json.optString("username", ""),
            password = json.optString("password", ""),
            usePrivateKey = json.optBoolean("usePrivateKey", false),
            privateKey = json.optString("privateKey", ""),
            overrideSni = json.optBoolean("overrideSni", false),
            sniHost = json.optString("sniHost", ""),
            wsPath = json.optString("wsPath", "/stream"),
            wsSubprotocol = json.optString("wsSubprotocol", ""),
            wsHeadersJson = json.optString("wsHeadersJson", "[]"),
            useUpstreamProxy = json.optBoolean("useUpstreamProxy", false),
            proxyType = json.optString("proxyType", "HTTP"),
            proxyHost = json.optString("proxyHost", ""),
            proxyPort = json.optInt("proxyPort", 8080),
            proxyAuth = json.optString("proxyAuth", ""),
            usePayload = json.optBoolean("usePayload", false),
            rawPayload = json.optString("rawPayload", "GET / HTTP/1.1[crlf]Host: [host][crlf][crlf]"),
            mtu = json.optInt("mtu", 1400),
            dnsMode = json.optString("dnsMode", "Through tunnel"),
            dnsPrimary = json.optString("dnsPrimary", "1.1.1.1"),
            dnsSecondary = json.optString("dnsSecondary", "1.0.0.1"),
            keepAlive = json.optBoolean("keepAlive", true),
            keepAliveInterval = json.optInt("keepAliveInterval", 30),
            autoReconnect = json.optBoolean("autoReconnect", true),
            maxReconnectAttempts = json.optInt("maxReconnectAttempts", 5),
            logLevel = json.optString("logLevel", "Info"),
            headersJson = json.optString("headersJson", "[]"),
            flagEmoji = json.optString("flagEmoji", "🌐")
        )
    }

    private fun parseSshUri(uriStr: String): ProfileEntity {
        val uri = URI(uriStr)
        val userInfo = uri.userInfo?.split(":") ?: emptyList()
        val username = userInfo.getOrNull(0) ?: ""
        val password = userInfo.getOrNull(1) ?: ""
        val host = uri.host ?: "127.0.0.1"
        val port = if (uri.port > 0) uri.port else 22

        return ProfileEntity(
            name = "SSH $host",
            transport = "SSH",
            host = host,
            port = port,
            username = username,
            password = password,
            flagEmoji = "🔑"
        )
    }

    private fun parseHc(hcContent: String): ProfileEntity {
        // HTTP Custom format: commonly has payload before/after host:port
        val lines = hcContent.lines()
        var host = "127.0.0.1"
        var port = 443
        var payload = ""
        var user = ""
        var pass = ""

        for (line in lines) {
            val l = line.trim()
            if (l.contains("@") && l.contains(":")) {
                // user:pass@host:port or host:port@user:pass
                val parts = l.split("@")
                if (parts.size == 2) {
                    val hp = parts[1].split(":")
                    if (hp.size >= 2) {
                        host = hp[0]
                        port = hp[1].toIntOrNull() ?: 443
                    }
                    val up = parts[0].split(":")
                    if (up.size >= 2) {
                        user = up[0]
                        pass = up[1]
                    }
                }
            } else if (l.contains("[crlf]")) {
                payload = l
            }
        }

        return ProfileEntity(
            name = "HTTP Custom Config",
            transport = if (payload.isNotEmpty()) "WS" else "SSL",
            host = host,
            port = port,
            username = user,
            password = pass,
            usePayload = payload.isNotEmpty(),
            rawPayload = payload.ifEmpty { "GET / HTTP/1.1[crlf]Host: [host][crlf][crlf]" },
            flagEmoji = "⚡"
        )
    }

    private fun parsePlaintext(line: String): ProfileEntity {
        // host:port[:user[:pass]]
        val parts = line.split(":")
        val host = parts.getOrNull(0)?.trim() ?: "127.0.0.1"
        val port = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 443
        val user = parts.getOrNull(2)?.trim() ?: ""
        val pass = parts.getOrNull(3)?.trim() ?: ""

        return ProfileEntity(
            name = "Server $host:$port",
            transport = if (port == 22) "SSH" else if (port == 53) "DNS" else "SSL",
            host = host,
            port = port,
            username = user,
            password = pass,
            flagEmoji = "🌐"
        )
    }
}
