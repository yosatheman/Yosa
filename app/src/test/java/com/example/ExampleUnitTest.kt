package com.example

import com.example.data.ProfileEntity
import com.example.tunnel.ConfigParser
import com.example.tunnel.PayloadExpander
import com.example.tunnel.TunnelEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun testPayloadExpanderTokens() {
        val profile = ProfileEntity(
            name = "Test Server",
            host = "tunnel.example.com",
            port = 443,
            sniHost = "sni.cloudflare.com"
        )
        val raw = "GET / HTTP/1.1[crlf]Host: [host]:[port][crlf]Rotate: [rotate][crlf]Rand: [random=8][crlf][crlf]"
        val expanded = PayloadExpander.expand(raw, profile)

        assertTrue(expanded.contains("\r\n"))
        assertTrue(expanded.contains("Host: tunnel.example.com:443"))
        assertTrue(expanded.contains("Rotate: sni.cloudflare.com"))
        // Check random 8 alphanumeric characters
        val match = Regex("""Rand: ([a-zA-Z0-9]{8})""").find(expanded)
        assertNotNull(match)
    }

    @Test
    fun testConfigParserJsonRoundTrip() {
        val profile = ProfileEntity(
            name = "Singapore Edge",
            transport = "WS",
            host = "sg.test.net",
            port = 8443,
            wsPath = "/v2ray",
            overrideSni = true,
            sniHost = "edge.net"
        )

        val exportedJson = ConfigParser.exportToJson(profile)
        val parsed = ConfigParser.parse(exportedJson)

        assertNotNull(parsed)
        assertEquals("Singapore Edge", parsed?.name)
        assertEquals("WS", parsed?.transport)
        assertEquals("sg.test.net", parsed?.host)
        assertEquals(8443, parsed?.port)
        assertEquals("/v2ray", parsed?.wsPath)
        assertEquals("edge.net", parsed?.sniHost)
    }

    @Test
    fun testConfigParserSshUri() {
        val uri = "ssh://myuser:secret123@192.168.1.50:2222"
        val parsed = ConfigParser.parse(uri)

        assertNotNull(parsed)
        assertEquals("SSH", parsed?.transport)
        assertEquals("192.168.1.50", parsed?.host)
        assertEquals(2222, parsed?.port)
        assertEquals("myuser", parsed?.username)
        assertEquals("secret123", parsed?.password)
    }

    @Test
    fun testTunnelEngineFormatting() {
        assertEquals("00:45", TunnelEngine.formatDuration(45))
        assertEquals("02:05", TunnelEngine.formatDuration(125))
        assertEquals("01:00:05", TunnelEngine.formatDuration(3605))

        assertEquals("500 B", TunnelEngine.formatBytes(500))
        assertTrue(TunnelEngine.formatBytes(1048576).startsWith("1.0"))
    }
}
