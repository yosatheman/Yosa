package com.example.data

object DefaultProfiles {
    fun createDefaults(): List<ProfileEntity> = listOf(
        ProfileEntity(
            id = 1,
            name = "US-East Fast TLS",
            transport = "SSL",
            host = "us-01.deepcurrent.net",
            port = 443,
            overrideSni = true,
            sniHost = "edge.cloudflare.com",
            flagEmoji = "🇺🇸",
            lastTestedLatencyMin = 38,
            lastTestedLatencyAvg = 42,
            lastTestedLatencyMax = 49,
            isActive = true
        ),
        ProfileEntity(
            id = 2,
            name = "Frankfurt WS Edge",
            transport = "WS",
            host = "de-02.deepcurrent.net",
            port = 8443,
            wsPath = "/stream",
            overrideSni = true,
            sniHost = "de-gateway.cdn.net",
            flagEmoji = "🇩🇪",
            lastTestedLatencyMin = 68,
            lastTestedLatencyAvg = 74,
            lastTestedLatencyMax = 82,
            isActive = false
        ),
        ProfileEntity(
            id = 3,
            name = "Singapore Secure SSH",
            transport = "SSH",
            host = "sg-01.deepcurrent.net",
            port = 22,
            username = "tunnel_usr",
            password = "secure_pass_demo",
            flagEmoji = "🇸🇬",
            lastTestedLatencyMin = 112,
            lastTestedLatencyAvg = 120,
            lastTestedLatencyMax = 135,
            isActive = false
        ),
        ProfileEntity(
            id = 4,
            name = "Tokyo DNS Bypass",
            transport = "DNS",
            host = "jp-01.deepcurrent.net",
            port = 53,
            dnsMode = "Through tunnel",
            dnsPrimary = "1.1.1.1",
            dnsSecondary = "8.8.8.8",
            flagEmoji = "🇯🇵",
            lastTestedLatencyMin = 145,
            lastTestedLatencyAvg = 158,
            lastTestedLatencyMax = 172,
            isActive = false
        )
    )
}
