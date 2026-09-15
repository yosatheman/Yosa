package com.example.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProfileEntity
import com.example.tunnel.TunnelEngine
import com.example.tunnel.TunnelState
import com.example.ui.theme.DeepCurrentMonoStyle
import com.example.ui.theme.DeepCurrentTheme
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val engine = viewModel.engine
    val state by engine.state.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val durationSeconds by engine.durationSeconds.collectAsState()
    val bytesDownloaded by engine.bytesDownloaded.collectAsState()
    val bytesUploaded by engine.bytesUploaded.collectAsState()
    val currentLatency by engine.currentLatencyMs.collectAsState()
    val isMeasuringLatency by engine.isMeasuringLatency.collectAsState()
    val sparklineRtt by engine.sparklineRtt.collectAsState()

    val autoReconnect by engine.autoReconnect.collectAsState()
    val killSwitch by engine.killSwitch.collectAsState()
    val dnsForward by engine.dnsForward.collectAsState()

    val colors = DeepCurrentTheme.colors
    val scrollState = rememberScrollState()

    // Haptic feedback trigger on state changes
    LaunchedEffect(state) {
        when (state) {
            is TunnelState.Connected -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            is TunnelState.Connecting, is TunnelState.Disconnecting -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            is TunnelState.Failed -> view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            else -> {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .verticalScroll(scrollState)
            .padding(horizontal = DeepCurrentTheme.spacing.x4),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))

        // 1. Status Strip (48dp height)
        StatusStrip(
            profile = activeProfile,
            latency = currentLatency,
            isMeasuring = isMeasuringLatency,
            onProfileClick = { viewModel.isProfilePickerOpen.value = true },
            modifier = Modifier.height(48.dp)
        )

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

        // 2. Hero Connect Zone (centered, ~260dp height container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp),
            contentAlignment = Alignment.Center
        ) {
            HeroConnectButton(
                state = state,
                onClick = {
                    if (state is TunnelState.Failed) {
                        viewModel.logFilter.value = "Error"
                        viewModel.currentTab.value = MainTab.LOGS
                    } else {
                        viewModel.toggleConnect(context)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

        // 3. Sparkline RTT Chart (Quality graph)
        if (state is TunnelState.Connected && sparklineRtt.isNotEmpty()) {
            SparklineQualityGraph(
                rttPoints = sparklineRtt,
                currentLatency = currentLatency ?: 45,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = DeepCurrentTheme.spacing.x2)
            )
            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
        }

        // 4. Stats Row (Duration · Download · Upload)
        StatsRow(
            durationSeconds = durationSeconds,
            bytesDownloaded = bytesDownloaded,
            bytesUploaded = bytesUploaded,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

        // 5. Server Card
        ServerCard(
            profile = activeProfile,
            onClick = { viewModel.isServerDetailOpen.value = true },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

        // 6. Quick Actions Row
        QuickActionsRow(
            autoReconnect = autoReconnect,
            killSwitch = killSwitch,
            dnsForward = dnsForward,
            onToggleAutoReconnect = { engine.autoReconnect.value = !autoReconnect },
            onToggleKillSwitch = { engine.killSwitch.value = !killSwitch },
            onToggleDnsForward = { engine.dnsForward.value = !dnsForward },
            onSpeedTestClick = { viewModel.isSpeedTestOpen.value = true },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x8))
    }
}

@Composable
private fun StatusStrip(
    profile: ProfileEntity?,
    latency: Int?,
    isMeasuring: Boolean,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DeepCurrentTheme.colors
    val pulseTransition = rememberInfiniteTransition(label = "latencyPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Profile Name Chip (tap = profile picker bottom sheet)
        Row(
            modifier = Modifier
                .clip(DeepCurrentTheme.radius.pill)
                .background(colors.bgSurface)
                .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.pill)
                .clickable(onClick = onProfileClick)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = profile?.flagEmoji ?: "🌐",
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = profile?.name ?: "No Profile Selected",
                style = DeepCurrentTheme.typography.titleMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.textHi,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select profile",
                tint = colors.textMid,
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 4.dp)
            )
        }

        // Right: Latency Pill (<80ms green, <200ms amber, red above, "—" when idle)
        val (latencyText, latencyColor) = when {
            latency == null -> "—" to colors.textLo
            latency < 80 -> "${latency}ms" to colors.ok
            latency < 200 -> "${latency}ms" to colors.warn
            else -> "${latency}ms" to colors.err
        }

        val pillAlpha = if (isMeasuring) pulseAlpha else 1.0f

        Row(
            modifier = Modifier
                .clip(DeepCurrentTheme.radius.pill)
                .background(colors.bgSurface)
                .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.pill)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(latencyColor.copy(alpha = pillAlpha))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = latencyText,
                style = DeepCurrentMonoStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = latencyColor.copy(alpha = pillAlpha)
            )
        }
    }
}

@Composable
private fun HeroConnectButton(
    state: TunnelState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DeepCurrentTheme.colors
    val coroutineScope = rememberCoroutineScope()

    // Shake offset for error state (±4dp 3 times over 300ms)
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(state) {
        if (state is TunnelState.Failed) {
            for (i in 0 until 3) {
                shakeOffset.animateTo(4f, tween(50))
                shakeOffset.animateTo(-4f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    // Breathing glow animation for Connected state (opacity 0.15 ↔ 0.28, 3s cycle)
    val glowTransition = rememberInfiniteTransition(label = "glowTransition")
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Connecting ring rotation (1.2s per rotation)
    val connectingTransition = rememberInfiniteTransition(label = "connectingSweep")
    val connectingAngle by connectingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "connectingAngle"
    )

    Box(
        modifier = modifier
            .size(220.dp)
            .padding(shakeOffset.value.dp, 0.dp, 0.dp, 0.dp)
            .testTag("hero_connect_button"),
        contentAlignment = Alignment.Center
    ) {
        // Soft radial glow behind connected state
        if (state is TunnelState.Connected) {
            Canvas(modifier = Modifier.size(260.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension / 2
                    )
                )
            }
        }

        // Circular background body
        val buttonBgColor = when (state) {
            is TunnelState.Connected -> colors.accent.copy(alpha = 0.12f)
            else -> colors.bgSurface
        }

        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(buttonBgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Animated Perimeter Ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                when (state) {
                    is TunnelState.Idle -> {
                        // 1px border in accent at 30% alpha
                        drawCircle(
                            color = colors.accent.copy(alpha = 0.30f),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    is TunnelState.Connecting -> {
                        // 3dp arc sweeping around perimeter
                        drawArc(
                            color = colors.accent,
                            startAngle = connectingAngle,
                            sweepAngle = 100f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    is TunnelState.Connected -> {
                        // solid accent 2dp full-circle
                        drawCircle(
                            color = colors.accent,
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    is TunnelState.Disconnecting -> {
                        // ring draining counter-clockwise
                        drawArc(
                            color = colors.textMid,
                            startAngle = 270f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    is TunnelState.Failed -> {
                        // Flash err border
                        drawCircle(
                            color = colors.err,
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // Center Content: Icon & Status Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (state) {
                    is TunnelState.Idle -> {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Connect",
                            tint = colors.textHi,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "CONNECT",
                            style = DeepCurrentTheme.typography.labelMedium.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textHi
                        )
                    }
                    is TunnelState.Connecting -> {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Connecting",
                            tint = colors.accent,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "CONNECTING",
                            style = DeepCurrentTheme.typography.labelMedium.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.accent
                        )
                    }
                    is TunnelState.Connected -> {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Connected",
                            tint = colors.accent,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "CONNECTED",
                            style = DeepCurrentTheme.typography.labelMedium.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.accent
                        )
                    }
                    is TunnelState.Disconnecting -> {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Disconnecting",
                            tint = colors.textMid,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "DISCONNECTING",
                            style = DeepCurrentTheme.typography.labelMedium.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textMid
                        )
                    }
                    is TunnelState.Failed -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Failed",
                            tint = colors.err,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "FAILED — tap to retry",
                            style = DeepCurrentTheme.typography.labelMedium.copy(
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = colors.err
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SparklineQualityGraph(
    rttPoints: List<Int>,
    currentLatency: Int,
    modifier: Modifier = Modifier
) {
    val colors = DeepCurrentTheme.colors
    val graphColor = when {
        currentLatency < 80 -> colors.ok
        currentLatency < 200 -> colors.warn
        else -> colors.err
    }

    Box(
        modifier = modifier
            .clip(DeepCurrentTheme.radius.chip)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.chip)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (rttPoints.size < 2) return@Canvas

            val minRtt = (rttPoints.minOrNull() ?: 20).toFloat().coerceAtLeast(10f)
            val maxRtt = (rttPoints.maxOrNull() ?: 120).toFloat().coerceAtLeast(minRtt + 20)
            val range = maxRtt - minRtt

            val stepX = size.width / (rttPoints.size - 1)
            val path = Path()

            rttPoints.forEachIndexed { index, rtt ->
                val x = index * stepX
                val normalizedY = 1f - ((rtt - minRtt) / range).coerceIn(0.1f, 0.9f)
                val y = normalizedY * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = graphColor,
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun StatsRow(
    durationSeconds: Long,
    bytesDownloaded: Long,
    bytesUploaded: Long,
    modifier: Modifier = Modifier
) {
    val colors = DeepCurrentTheme.colors

    Row(
        modifier = modifier
            .clip(DeepCurrentTheme.radius.card)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
            .padding(vertical = DeepCurrentTheme.spacing.x4),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(
            label = "Duration",
            value = TunnelEngine.formatDuration(durationSeconds),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(36.dp)
                .background(colors.borderSubtle)
        )
        StatCell(
            label = "↓ Download",
            value = TunnelEngine.formatBytes(bytesDownloaded),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(36.dp)
                .background(colors.borderSubtle)
        )
        StatCell(
            label = "↑ Upload",
            value = TunnelEngine.formatBytes(bytesUploaded),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = DeepCurrentTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = DeepCurrentTheme.typography.labelMedium,
            color = colors.textLo
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Subtle fade-swap on change, no slide
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(160)) },
            label = "statFadeSwap"
        ) { targetValue ->
            Text(
                text = targetValue,
                style = DeepCurrentMonoStyle.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.textHi
            )
        }
    }
}

@Composable
private fun ServerCard(
    profile: ProfileEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DeepCurrentTheme.colors
    var isRevealed by remember { mutableStateOf(false) }

    val rawHost = profile?.host ?: "us-01.deepcurrent.net"
    val maskedHost = if (isRevealed) {
        rawHost
    } else {
        if (rawHost.length > 8) {
            val prefix = rawHost.take(5)
            val suffix = rawHost.takeLast(4)
            "$prefix.****.$suffix"
        } else {
            "us-01.****.net"
        }
    }

    Box(
        modifier = modifier
            .clip(DeepCurrentTheme.radius.card)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isRevealed = true
                        tryAwaitRelease()
                        isRevealed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(DeepCurrentTheme.spacing.x4)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile?.flagEmoji ?: "🌐",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = maskedHost,
                        style = DeepCurrentMonoStyle.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textHi
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Transport chip
                    Box(
                        modifier = Modifier
                            .clip(DeepCurrentTheme.radius.chip)
                            .background(colors.bgElevated)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = profile?.transport ?: "SSL",
                            style = DeepCurrentMonoStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.accent
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // SNI status dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (profile?.overrideSni == true) colors.ok else colors.textLo)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (profile?.overrideSni == true) "SNI Active" else "Direct SNI",
                        style = DeepCurrentTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        color = colors.textMid
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Server details",
                tint = colors.textMid,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    autoReconnect: Boolean,
    killSwitch: Boolean,
    dnsForward: Boolean,
    onToggleAutoReconnect: () -> Unit,
    onToggleKillSwitch: () -> Unit,
    onToggleDnsForward: () -> Unit,
    onSpeedTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(DeepCurrentTheme.spacing.x2)
    ) {
        QuickActionPill(
            label = "Auto-reconnect",
            isActive = autoReconnect,
            onClick = onToggleAutoReconnect
        )
        QuickActionPill(
            label = "Kill switch",
            isActive = killSwitch,
            onClick = onToggleKillSwitch
        )
        QuickActionPill(
            label = "DNS forward",
            isActive = dnsForward,
            onClick = onToggleDnsForward
        )
        QuickActionPill(
            label = "Speed test",
            isActive = false,
            onClick = onSpeedTestClick,
            icon = Icons.Default.Bolt
        )
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val colors = DeepCurrentTheme.colors
    val bg = if (isActive) colors.accent.copy(alpha = 0.20f) else colors.bgSurface
    val border = if (isActive) colors.accent else colors.borderSubtle
    val textColor = if (isActive) colors.accent else colors.textMid

    Row(
        modifier = modifier
            .clip(DeepCurrentTheme.radius.pill)
            .background(bg)
            .border(1.dp, border, DeepCurrentTheme.radius.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 4.dp)
            )
        }
        Text(
            text = label,
            style = DeepCurrentTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = textColor
        )
    }
}
