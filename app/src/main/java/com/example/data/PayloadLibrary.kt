package com.example.data

data class PayloadEntry(
    val id: String,
    val name: String,
    val country: String,
    val carrierFlag: String,
    val description: String,
    val rawPayload: String
)

object PayloadLibrary {
    val categories: Map<String, List<PayloadEntry>> = mapOf(
        "Global / Cloud CDN" to listOf(
            PayloadEntry(
                id = "cf_ws_direct",
                name = "Cloudflare WS Direct",
                country = "Global",
                carrierFlag = "🌐",
                description = "Standard CDN websocket handshake with keep-alive upgrade",
                rawPayload = "GET /stream HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf]User-Agent: [ua][crlf][crlf]"
            ),
            PayloadEntry(
                id = "fastly_edge",
                name = "Fastly Edge CDN Bypass",
                country = "Global",
                carrierFlag = "⚡",
                description = "HTTP CONNECT injection with rotated host header",
                rawPayload = "CONNECT [host]:[port] HTTP/1.1[crlf]Host: [rotate][crlf]X-Online-Host: [host][crlf]Connection: Keep-Alive[crlf][crlf]"
            )
        ),
        "United States" to listOf(
            PayloadEntry(
                id = "us_tmo_ssl",
                name = "T-Mobile Zero-Rating Forward",
                country = "United States",
                carrierFlag = "🇺🇸",
                description = "Injected proxy gateway headers with random request ID",
                rawPayload = "GET / HTTP/1.1[crlf]Host: [host][crlf]X-Forward-For: 10.[random=8].[random=8][crlf]Connection: Keep-Alive[crlf][crlf]"
            ),
            PayloadEntry(
                id = "us_att_ws",
                name = "AT&T Stream Bypass",
                country = "United States",
                carrierFlag = "🇺🇸",
                description = "Custom Host header with SSL SNI proxy tunnel",
                rawPayload = "HEAD / HTTP/1.1[crlf]Host: [host][crlf]X-Online-Host: [host][crlf]Upgrade: websocket[crlf][crlf]"
            )
        ),
        "Latin America" to listOf(
            PayloadEntry(
                id = "latam_telcel",
                name = "Telcel / Claro FreeNet",
                country = "Mexico / LATAM",
                carrierFlag = "🇲🇽",
                description = "HTTP 1.1 keep-alive with zero-rated host injection",
                rawPayload = "GET / HTTP/1.1[crlf]Host: m.whatsapp.net[crlf]X-Online-Host: [host][crlf]Connection: Upgrade[crlf][crlf]"
            ),
            PayloadEntry(
                id = "br_vivo",
                name = "Vivo / TIM Brasil CDN",
                country = "Brazil",
                carrierFlag = "🇧🇷",
                description = "Split header inject with CRLF injection",
                rawPayload = "CONNECT [host]:[port]@[rotate] HTTP/1.1[crlf]Host: [host][crlf]Connection: Keep-Alive[crlf][crlf]"
            )
        ),
        "Southeast Asia" to listOf(
            PayloadEntry(
                id = "id_telkomsel",
                name = "Telkomsel Ilmupedia WS",
                country = "Indonesia",
                carrierFlag = "🇮🇩",
                description = "Websocket tunnel injection targeting educational endpoints",
                rawPayload = "GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf]Sec-WebSocket-Key: [random=16][crlf][crlf]"
            ),
            PayloadEntry(
                id = "ph_smart",
                name = "Smart / Globe Giga Sni Bug",
                country = "Philippines",
                carrierFlag = "🇵🇭",
                description = "Direct payload with user-agent spoofing",
                rawPayload = "GET / HTTP/1.1[crlf]Host: [host][crlf]User-Agent: [ua][crlf]Connection: Keep-Alive[crlf][crlf]"
            )
        ),
        "Europe" to listOf(
            PayloadEntry(
                id = "de_telekom",
                name = "Deutsche Telekom Fast Tunnel",
                country = "Germany",
                carrierFlag = "🇩🇪",
                description = "Standard HTTP-CONNECT proxy handshake",
                rawPayload = "CONNECT [host]:[port] HTTP/1.1[crlf]Host: [host][crlf]Proxy-Connection: Keep-Alive[crlf][crlf]"
            )
        )
    )
}
