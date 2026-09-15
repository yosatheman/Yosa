package com.example.tunnel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.example.data.ProfileEntity
import java.security.SecureRandom

object PayloadExpander {

    val TOKENS = listOf(
        "[crlf]",
        "[host]",
        "[port]",
        "[rotate]",
        "[random=8]",
        "[random=16]",
        "[ua]"
    )

    private val random = SecureRandom()
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    fun expand(raw: String, profile: ProfileEntity): String {
        var result = raw
        // [crlf]
        result = result.replace("[crlf]", "\r\n")

        // [host]
        result = result.replace("[host]", profile.host)

        // [port]
        result = result.replace("[port]", profile.port.toString())

        // [rotate]
        val altHost = if (profile.sniHost.isNotBlank()) profile.sniHost else "${profile.host}.cdn"
        result = result.replace("[rotate]", altHost)

        // [ua]
        val ua = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        result = result.replace("[ua]", ua)

        // [random=N]
        val randomRegex = Regex("""\[random=(\d+)]""")
        result = randomRegex.replace(result) { match ->
            val count = match.groupValues[1].toIntOrNull() ?: 8
            (1..count)
                .map { random.nextInt(charPool.size) }
                .map(charPool::get)
                .joinToString("")
        }

        return result
    }

    fun highlight(text: String, accentColor: Color, baseTextColor: Color): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            // Color base text
            addStyle(SpanStyle(color = baseTextColor), 0, text.length)

            // Highlight recognized tokens
            val tokenRegex = Regex("""(\[crlf]|\[host]|\[port]|\[rotate]|\[random=\d+]|\[ua])""")
            tokenRegex.findAll(text).forEach { matchResult ->
                addStyle(
                    SpanStyle(
                        color = accentColor,
                        background = accentColor.copy(alpha = 0.15f)
                    ),
                    matchResult.range.first,
                    matchResult.range.last + 1
                )
            }
        }
    }
}
