package com.example.tunnel

import com.example.data.ProfileEntity
import org.json.JSONObject
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ConfigParser {

    const val TERMUX_EXTENSION = ".termux"

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

    fun exportToTermux(profile: ProfileEntity): String {
        val jsonPayload = exportToJson(profile)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val termuxCommand = when (profile.transport.uppercase()) {
            "SSH" -> "ssh -p ${profile.port} -D 1080 ${if (profile.username.isNotEmpty()) "${profile.username}@" else ""}${profile.host}"
            "WS" -> "curl -i -N -H \"Connection: Upgrade\" -H \"Upgrade: websocket\" http://${profile.host}:${profile.port}${profile.wsPath}"
            else -> "curl -vk https://${profile.host}:${profile.port}"
        }

        return buildString {
            appendLine("#!/bin/bash")
            appendLine("# ========================================================")
            appendLine("# MR UNKNOWN CONFIGURATION FILE (.termux)")
            appendLine("# Target Application : Mr Unknown Android & Termux Shell")
            appendLine("# Profile Name       : ${profile.name}")
            appendLine("# Transport Protocol : ${profile.transport}")
            appendLine("# Target Server      : ${profile.host}:${profile.port}")
            appendLine("# File Extension     : $TERMUX_EXTENSION")
            appendLine("# Exported Date      : $timestamp")
            appendLine("# ========================================================")
            appendLine("#")
            appendLine("# Termux Direct Test Command:")
            appendLine("#   $termuxCommand")
            appendLine("#")
            appendLine("# [MR_UNKNOWN_TERMUX_CONFIG]")
            appendLine(jsonPayload)
            appendLine("# [/MR_UNKNOWN_TERMUX_CONFIG]")
        }
    }

    fun getTermuxFileName(profile: ProfileEntity): String {
        val sanitized = profile.name
            .trim()
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .lowercase(Locale.ROOT)
            .ifEmpty { "tunnel_profile" }
        return "$sanitized$TERMUX_EXTENSION"
    }

    fun parse(content: String): ProfileEntity? {
        val trimmed = content.trim()
        return try {
            when {
                // 1. .termux configuration block
                trimmed.contains("[MR_UNKNOWN_TERMUX_CONFIG]") || trimmed.contains("[MR_UNKNOWN_CONFIG]") -> {
                    parseTermuxBlock(trimmed)
                }
                // 2. Shell file with embedded JSON
                trimmed.startsWith("#!/bin/bash") || trimmed.contains(".termux") || trimmed.contains("MR UNKNOWN") -> {
                    val startIdx = trimmed.indexOf("{")
                    val endIdx = trimmed.lastIndexOf("}")
                    if (startIdx != -1 && endIdx > startIdx) {
                        parseJson(trimmed.substring(startIdx, endIdx + 1))
                    } else {
                        parseTermuxBlock(trimmed)
                    }
                }
                // 3. Raw JSON
                trimmed.startsWith("{") && trimmed.endsWith("}") -> parseJson(trimmed)
                // 4. SSH URI
                trimmed.startsWith("ssh://", ignoreCase = true) -> parseSshUri(trimmed)
                // 5. HTTP Custom (.hc)
                trimmed.contains("[crlf]") || trimmed.contains("HTTP Custom") || trimmed.contains(".hc") -> parseHc(trimmed)
                // 6. Plaintext host:port
                trimmed.contains(":") -> parsePlaintext(trimmed)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTermuxBlock(content: String): ProfileEntity? {
        val startMarker1 = "[MR_UNKNOWN_TERMUX_CONFIG]"
        val endMarker1 = "[/MR_UNKNOWN_TERMUX_CONFIG]"
        val startMarker2 = "[MR_UNKNOWN_CONFIG]"
        val endMarker2 = "[/MR_UNKNOWN_CONFIG]"

        val rawJson = when {
            content.contains(startMarker1) && content.contains(endMarker1) -> {
                content.substringAfter(startMarker1).substringBefore(endMarker1).trim()
            }
            content.contains(startMarker2) && content.contains(endMarker2) -> {
                content.substringAfter(startMarker2).substringBefore(endMarker2).trim()
            }
            else -> {
                val start = content.indexOf("{")
                val end = content.lastIndexOf("}")
                if (start != -1 && end > start) content.substring(start, end + 1) else null
            }
        }

        return if (!rawJson.isNullOrBlank()) {
            parseJson(rawJson)
        } else {
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
