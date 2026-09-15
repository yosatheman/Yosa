package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProfileEntity
import com.example.tunnel.PayloadExpander
import com.example.ui.theme.DeepCurrentMonoStyle
import com.example.ui.theme.DeepCurrentTheme
import org.json.JSONArray
import org.json.JSONObject

private enum class EditorTab(val label: String) {
    CONNECTION("Connection"),
    PAYLOAD("Payload"),
    ADVANCED("Advanced"),
    HEADERS("Headers"),
    SPLIT_TUNNEL("Split Tunnel")
}

@Composable
fun ProfileEditorScreen(
    initialProfile: ProfileEntity,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = DeepCurrentTheme.colors

    var selectedTab by remember { mutableStateOf(EditorTab.CONNECTION) }
    var overflowExpanded by remember { mutableStateOf(false) }

    // State fields
    var name by remember { mutableStateOf(initialProfile.name) }
    var transport by remember { mutableStateOf(initialProfile.transport) }
    var host by remember { mutableStateOf(initialProfile.host) }
    var portText by remember { mutableStateOf(initialProfile.port.toString()) }
    var username by remember { mutableStateOf(initialProfile.username) }
    var password by remember { mutableStateOf(initialProfile.password) }
    var usePrivateKey by remember { mutableStateOf(initialProfile.usePrivateKey) }
    var privateKey by remember { mutableStateOf(initialProfile.privateKey) }

    var overrideSni by remember { mutableStateOf(initialProfile.overrideSni) }
    var sniHost by remember { mutableStateOf(initialProfile.sniHost) }

    var wsPath by remember { mutableStateOf(initialProfile.wsPath) }
    var wsSubprotocol by remember { mutableStateOf(initialProfile.wsSubprotocol) }

    var useUpstreamProxy by remember { mutableStateOf(initialProfile.useUpstreamProxy) }
    var proxyType by remember { mutableStateOf(initialProfile.proxyType) }
    var proxyHost by remember { mutableStateOf(initialProfile.proxyHost) }
    var proxyPortText by remember { mutableStateOf(initialProfile.proxyPort.toString()) }

    var usePayload by remember { mutableStateOf(initialProfile.usePayload) }
    var rawPayload by remember { mutableStateOf(TextFieldValue(initialProfile.rawPayload)) }
    var isPreviewExpanded by remember { mutableStateOf(false) }

    var mtu by remember { mutableStateOf(initialProfile.mtu.toFloat()) }
    var dnsMode by remember { mutableStateOf(initialProfile.dnsMode) }
    var dnsPrimary by remember { mutableStateOf(initialProfile.dnsPrimary) }
    var dnsSecondary by remember { mutableStateOf(initialProfile.dnsSecondary) }
    var keepAlive by remember { mutableStateOf(initialProfile.keepAlive) }
    var keepAliveInterval by remember { mutableStateOf(initialProfile.keepAliveInterval) }
    var autoReconnect by remember { mutableStateOf(initialProfile.autoReconnect) }
    var maxReconnectAttempts by remember { mutableStateOf(initialProfile.maxReconnectAttempts) }
    var logLevel by remember { mutableStateOf(initialProfile.logLevel) }

    // Repeatable headers
    val headerRows = remember {
        val list = mutableStateListOf<Pair<String, String>>()
        try {
            val arr = JSONArray(initialProfile.headersJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(obj.optString("k") to obj.optString("v"))
            }
        } catch (e: Exception) {
            list.add("User-Agent" to "DeepCurrent/1.0")
        }
        list
    }

    // Split Tunneling excluded packages
    val excludedSet = remember {
        val set = mutableStateListOf<String>()
        initialProfile.excludedAppsPackages.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { set.add(it) }
        set
    }

    val installedApps by viewModel.installedApps.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context)
    }

    val isTestingConnection by viewModel.isTestingEditorConnection.collectAsState()
    val testLatencyResult by viewModel.editorTestLatencyResult.collectAsState()

    // Inline validation for name
    val isNameValid = name.trim().isNotEmpty()

    val currentProfileDraft = initialProfile.copy(
        name = name.trim(),
        transport = transport,
        host = host.trim(),
        port = portText.toIntOrNull() ?: 443,
        username = username.trim(),
        password = password,
        usePrivateKey = usePrivateKey,
        privateKey = privateKey.trim(),
        overrideSni = overrideSni,
        sniHost = sniHost.trim(),
        wsPath = wsPath.trim(),
        wsSubprotocol = wsSubprotocol.trim(),
        useUpstreamProxy = useUpstreamProxy,
        proxyType = proxyType,
        proxyHost = proxyHost.trim(),
        proxyPort = proxyPortText.toIntOrNull() ?: 8080,
        usePayload = usePayload,
        rawPayload = rawPayload.text,
        mtu = mtu.toInt(),
        dnsMode = dnsMode,
        dnsPrimary = dnsPrimary.trim(),
        dnsSecondary = dnsSecondary.trim(),
        keepAlive = keepAlive,
        keepAliveInterval = keepAliveInterval,
        autoReconnect = autoReconnect,
        maxReconnectAttempts = maxReconnectAttempts,
        logLevel = logLevel,
        headersJson = JSONArray(headerRows.map { JSONObject().apply { put("k", it.first); put("v", it.second) } }).toString(),
        excludedAppsPackages = excludedSet.joinToString(",")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBase)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgElevated)
                .padding(horizontal = DeepCurrentTheme.spacing.x2, vertical = DeepCurrentTheme.spacing.x2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textHi
                )
            }

            Text(
                text = if (initialProfile.id == 0L) "New profile" else name.ifEmpty { "Edit profile" },
                style = DeepCurrentTheme.typography.titleMedium.copy(fontSize = 17.sp),
                color = colors.textHi,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                maxLines = 1
            )

            TextButton(
                onClick = {
                    if (isNameValid) {
                        viewModel.saveEditingProfile(currentProfileDraft)
                    }
                },
                enabled = isNameValid,
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text(
                    text = "Save",
                    style = DeepCurrentTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isNameValid) colors.accent else colors.textDis
                )
            }

            Box {
                IconButton(onClick = { overflowExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = colors.textMid
                    )
                }

                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false },
                    modifier = Modifier.background(colors.bgElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Export Config", color = colors.textHi) },
                        onClick = {
                            overflowExpanded = false
                            viewModel.qrShareProfile.value = currentProfileDraft
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate", color = colors.textHi) },
                        onClick = {
                            overflowExpanded = false
                            viewModel.duplicateProfile(currentProfileDraft)
                            onBack()
                        }
                    )
                    if (initialProfile.id != 0L) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = colors.err) },
                            onClick = {
                                overflowExpanded = false
                                viewModel.deleteProfile(initialProfile)
                                onBack()
                            }
                        )
                    }
                }
            }
        }

        // Horizontal scroll of pill tabs
        val tabScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgBase)
                .horizontalScroll(tabScrollState)
                .padding(horizontal = DeepCurrentTheme.spacing.x4, vertical = DeepCurrentTheme.spacing.x3),
            horizontalArrangement = Arrangement.spacedBy(DeepCurrentTheme.spacing.x2)
        ) {
            EditorTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val bg = if (isSelected) colors.accent else colors.bgSurface
                val textColor = if (isSelected) colors.bgBase else colors.textMid
                val border = if (isSelected) colors.accent else colors.borderSubtle

                Box(
                    modifier = Modifier
                        .clip(DeepCurrentTheme.radius.pill)
                        .background(bg)
                        .border(1.dp, border, DeepCurrentTheme.radius.pill)
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = tab.label,
                        style = DeepCurrentTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = textColor
                    )
                }
            }
        }

        // Tab Content
        val contentScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(contentScrollState)
                .padding(horizontal = DeepCurrentTheme.spacing.x4, vertical = DeepCurrentTheme.spacing.x2)
        ) {
            when (selectedTab) {
                EditorTab.CONNECTION -> {
                    ConnectionTab(
                        name = name,
                        onNameChange = { name = it },
                        isNameValid = isNameValid,
                        transport = transport,
                        onTransportChange = { transport = it },
                        host = host,
                        onHostChange = { host = it },
                        portText = portText,
                        onPortChange = { portText = it },
                        username = username,
                        onUsernameChange = { username = it },
                        password = password,
                        onPasswordChange = { password = it },
                        usePrivateKey = usePrivateKey,
                        onToggleUsePrivateKey = { usePrivateKey = it },
                        privateKey = privateKey,
                        onPrivateKeyChange = { privateKey = it },
                        overrideSni = overrideSni,
                        onToggleOverrideSni = { overrideSni = it },
                        sniHost = sniHost,
                        onSniHostChange = { sniHost = it },
                        wsPath = wsPath,
                        onWsPathChange = { wsPath = it },
                        wsSubprotocol = wsSubprotocol,
                        onWsSubprotocolChange = { wsSubprotocol = it },
                        useUpstreamProxy = useUpstreamProxy,
                        onToggleUpstreamProxy = { useUpstreamProxy = it },
                        proxyType = proxyType,
                        onProxyTypeChange = { proxyType = it },
                        proxyHost = proxyHost,
                        onProxyHostChange = { proxyHost = it },
                        proxyPortText = proxyPortText,
                        onProxyPortChange = { proxyPortText = it },
                        isTesting = isTestingConnection,
                        testResult = testLatencyResult,
                        onTestConnection = {
                            viewModel.testEditorConnection(host, portText.toIntOrNull() ?: 443)
                        }
                    )
                }
                EditorTab.PAYLOAD -> {
                    PayloadTab(
                        usePayload = usePayload,
                        onToggleUsePayload = { usePayload = it },
                        rawPayload = rawPayload,
                        onPayloadChange = { rawPayload = it },
                        profileDraft = currentProfileDraft,
                        isPreviewExpanded = isPreviewExpanded,
                        onTogglePreview = { isPreviewExpanded = !isPreviewExpanded },
                        onOpenLibrary = { viewModel.isPayloadLibraryOpen.value = true }
                    )
                }
                EditorTab.ADVANCED -> {
                    AdvancedTab(
                        mtu = mtu,
                        onMtuChange = { mtu = it },
                        dnsMode = dnsMode,
                        onDnsModeChange = { dnsMode = it },
                        dnsPrimary = dnsPrimary,
                        onDnsPrimaryChange = { dnsPrimary = it },
                        dnsSecondary = dnsSecondary,
                        onDnsSecondaryChange = { dnsSecondary = it },
                        keepAlive = keepAlive,
                        onToggleKeepAlive = { keepAlive = it },
                        keepAliveInterval = keepAliveInterval,
                        onKeepAliveIntervalChange = { keepAliveInterval = it },
                        autoReconnect = autoReconnect,
                        onToggleAutoReconnect = { autoReconnect = it },
                        maxAttempts = maxReconnectAttempts,
                        onMaxAttemptsChange = { maxReconnectAttempts = it },
                        logLevel = logLevel,
                        onLogLevelChange = { logLevel = it }
                    )
                }
                EditorTab.HEADERS -> {
                    HeadersTab(
                        headers = headerRows,
                        onAddHeader = { k, v -> headerRows.add(k to v) },
                        onRemoveHeader = { idx -> headerRows.removeAt(idx) }
                    )
                }
                EditorTab.SPLIT_TUNNEL -> {
                    SplitTunnelTab(
                        installedApps = installedApps,
                        excludedPackages = excludedSet,
                        onToggleApp = { pkg ->
                            if (excludedSet.contains(pkg)) excludedSet.remove(pkg)
                            else excludedSet.add(pkg)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun ConnectionTab(
    name: String,
    onNameChange: (String) -> Unit,
    isNameValid: Boolean,
    transport: String,
    onTransportChange: (String) -> Unit,
    host: String,
    onHostChange: (String) -> Unit,
    portText: String,
    onPortChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    usePrivateKey: Boolean,
    onToggleUsePrivateKey: (Boolean) -> Unit,
    privateKey: String,
    onPrivateKeyChange: (String) -> Unit,
    overrideSni: Boolean,
    onToggleOverrideSni: (Boolean) -> Unit,
    sniHost: String,
    onSniHostChange: (String) -> Unit,
    wsPath: String,
    onWsPathChange: (String) -> Unit,
    wsSubprotocol: String,
    onWsSubprotocolChange: (String) -> Unit,
    useUpstreamProxy: Boolean,
    onToggleUpstreamProxy: (Boolean) -> Unit,
    proxyType: String,
    onProxyTypeChange: (String) -> Unit,
    proxyHost: String,
    onProxyHostChange: (String) -> Unit,
    proxyPortText: String,
    onProxyPortChange: (String) -> Unit,
    isTesting: Boolean,
    testResult: com.example.tunnel.LatencyResult?,
    onTestConnection: () -> Unit
) {
    val colors = DeepCurrentTheme.colors
    val transports = listOf("SSH", "SSL", "WS", "DNS", "HTTP-CONNECT")

    // Name Field
    EditorTextField(
        value = name,
        onValueChange = onNameChange,
        label = "Profile Name",
        isError = !isNameValid,
        helperText = if (!isNameValid) "Name cannot be empty" else null
    )

    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

    // Transport Segmented Control
    Text(
        text = "Transport Mode",
        style = DeepCurrentTheme.typography.labelMedium,
        color = colors.textMid
    )
    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DeepCurrentTheme.radius.card)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        transports.forEach { mode ->
            val isSelected = transport == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(DeepCurrentTheme.radius.chip)
                    .background(if (isSelected) colors.accent else Color.Transparent)
                    .clickable { onTransportChange(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode,
                    style = DeepCurrentMonoStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) colors.bgBase else colors.textMid
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

    // Host + Port (side by side, port 88dp wide)
    Row(modifier = Modifier.fillMaxWidth()) {
        EditorTextField(
            value = host,
            onValueChange = onHostChange,
            label = "Server Host / IP",
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(DeepCurrentTheme.spacing.x3))
        EditorTextField(
            value = portText,
            onValueChange = onPortChange,
            label = "Port",
            modifier = Modifier.width(88.dp)
        )
    }

    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

    // Auth section
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Use Private Key", style = DeepCurrentTheme.typography.bodyLarge, color = colors.textHi)
        Switch(
            checked = usePrivateKey,
            onCheckedChange = onToggleUsePrivateKey,
            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.bgElevated)
        )
    }

    if (usePrivateKey) {
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
        EditorTextField(
            value = privateKey,
            onValueChange = onPrivateKeyChange,
            label = "Private Key (PEM / OpenSSH format)",
            singleLine = false,
            minLines = 3
        )
    } else {
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
        Row(modifier = Modifier.fillMaxWidth()) {
            EditorTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = "Username (optional)",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(DeepCurrentTheme.spacing.x3))
            EditorTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                modifier = Modifier.weight(1f)
            )
        }
    }

    // SNI Section (visible only for SSL/WS)
    if (transport == "SSL" || transport == "WS") {
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Override SNI", style = DeepCurrentTheme.typography.bodyLarge, color = colors.textHi)
            Switch(
                checked = overrideSni,
                onCheckedChange = onToggleOverrideSni,
                colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.bgElevated)
            )
        }
        if (overrideSni) {
            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
            EditorTextField(
                value = sniHost,
                onValueChange = onSniHostChange,
                label = "SNI Host (e.g. edge.cloudflare.com)"
            )
        }
    }

    // WS Section (visible only for WS)
    if (transport == "WS") {
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))
        Row(modifier = Modifier.fillMaxWidth()) {
            EditorTextField(
                value = wsPath,
                onValueChange = onWsPathChange,
                label = "WS Path",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(DeepCurrentTheme.spacing.x3))
            EditorTextField(
                value = wsSubprotocol,
                onValueChange = onWsSubprotocolChange,
                label = "Subprotocol",
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Proxy Section
    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Use Upstream Proxy", style = DeepCurrentTheme.typography.bodyLarge, color = colors.textHi)
        Switch(
            checked = useUpstreamProxy,
            onCheckedChange = onToggleUpstreamProxy,
            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.bgElevated)
        )
    }

    if (useUpstreamProxy) {
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
        Row(modifier = Modifier.fillMaxWidth()) {
            EditorTextField(
                value = proxyHost,
                onValueChange = onProxyHostChange,
                label = "Proxy Host",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(DeepCurrentTheme.spacing.x3))
            EditorTextField(
                value = proxyPortText,
                onValueChange = onProxyPortChange,
                label = "Proxy Port",
                modifier = Modifier.width(96.dp)
            )
        }
    }

    // Test Connection Button
    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x6))
    OutlinedButton(
        onClick = onTestConnection,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent),
        shape = DeepCurrentTheme.radius.pill
    ) {
        Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isTesting) "Testing Reachability..." else "Test Connection",
            fontWeight = FontWeight.SemiBold
        )
    }

    if (testResult != null) {
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = if (testResult.reachable) colors.ok else colors.err
            val statusText = if (testResult.reachable) "✓ Reachable: min ${testResult.min}ms / avg ${testResult.avg}ms / max ${testResult.max}ms"
            else "✗ Unreachable: ${testResult.error ?: "Connection timed out"}"

            Text(
                text = statusText,
                style = DeepCurrentMonoStyle.copy(fontSize = 12.sp),
                color = statusColor
            )
        }
    }
}

@Composable
private fun PayloadTab(
    usePayload: Boolean,
    onToggleUsePayload: (Boolean) -> Unit,
    rawPayload: TextFieldValue,
    onPayloadChange: (TextFieldValue) -> Unit,
    profileDraft: ProfileEntity,
    isPreviewExpanded: Boolean,
    onTogglePreview: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val colors = DeepCurrentTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Use Payload Injection", style = DeepCurrentTheme.typography.titleMedium, color = colors.textHi)
            Text("Inject HTTP headers prior to tunnel handshake", style = DeepCurrentTheme.typography.bodyMedium, color = colors.textLo)
        }
        Switch(
            checked = usePayload,
            onCheckedChange = onToggleUsePayload,
            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.bgElevated)
        )
    }

    if (usePayload) {
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

        // Load from Library Button
        OutlinedButton(
            onClick = onOpenLibrary,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
            shape = DeepCurrentTheme.radius.pill
        ) {
            Icon(Icons.Default.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Load from Payload Library", style = DeepCurrentTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

        // Token Palette Chips
        Text("Token Palette (tap to insert)", style = DeepCurrentTheme.typography.labelMedium, color = colors.textMid)
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
        val tokenScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(tokenScrollState),
            horizontalArrangement = Arrangement.spacedBy(DeepCurrentTheme.spacing.x2)
        ) {
            PayloadExpander.TOKENS.forEach { token ->
                Box(
                    modifier = Modifier
                        .clip(DeepCurrentTheme.radius.chip)
                        .background(colors.bgSurface)
                        .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.chip)
                        .clickable {
                            val currentText = rawPayload.text
                            val selection = rawPayload.selection
                            val newText = currentText.substring(0, selection.start) + token + currentText.substring(selection.end)
                            onPayloadChange(
                                TextFieldValue(
                                    text = newText,
                                    selection = androidx.compose.ui.text.TextRange(selection.start + token.length)
                                )
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = token,
                        style = DeepCurrentMonoStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.accent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

        // Monospace Payload Editor with syntax highlighting (8 rows visible)
        OutlinedTextField(
            value = rawPayload,
            onValueChange = onPayloadChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(DeepCurrentTheme.radius.card)
                .background(colors.bgSurface),
            textStyle = DeepCurrentMonoStyle.copy(fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.borderSubtle,
                focusedContainerColor = colors.bgSurface,
                unfocusedContainerColor = colors.bgSurface,
                cursorColor = colors.accent
            )
        )

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

        // Preview Expansion Collapsible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DeepCurrentTheme.radius.card)
                .background(colors.bgSurface)
                .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
                .clickable(onClick = onTogglePreview)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Preview expansion", style = DeepCurrentTheme.typography.titleMedium.copy(fontSize = 14.sp), color = colors.textHi)
            Icon(
                imageVector = if (isPreviewExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = colors.textMid
            )
        }

        AnimatedVisibility(
            visible = isPreviewExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val expandedText = remember(rawPayload.text, profileDraft) {
                PayloadExpander.expand(rawPayload.text, profileDraft)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(DeepCurrentTheme.radius.card)
                    .background(colors.bgTint)
                    .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
                    .padding(14.dp)
            ) {
                Text(
                    text = expandedText.replace("\r\n", "\n"),
                    style = DeepCurrentMonoStyle.copy(fontSize = 12.sp, color = colors.accent)
                )
            }
        }
    }
}

@Composable
private fun AdvancedTab(
    mtu: Float,
    onMtuChange: (Float) -> Unit,
    dnsMode: String,
    onDnsModeChange: (String) -> Unit,
    dnsPrimary: String,
    onDnsPrimaryChange: (String) -> Unit,
    dnsSecondary: String,
    onDnsSecondaryChange: (String) -> Unit,
    keepAlive: Boolean,
    onToggleKeepAlive: (Boolean) -> Unit,
    keepAliveInterval: Int,
    onKeepAliveIntervalChange: (Int) -> Unit,
    autoReconnect: Boolean,
    onToggleAutoReconnect: (Boolean) -> Unit,
    maxAttempts: Int,
    onMaxAttemptsChange: (Int) -> Unit,
    logLevel: String,
    onLogLevelChange: (String) -> Unit
) {
    val colors = DeepCurrentTheme.colors

    // MTU Slider (1280–1500, step 20)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("MTU Size", style = DeepCurrentTheme.typography.bodyLarge, color = colors.textHi)
            Text("${mtu.toInt()} bytes", style = DeepCurrentMonoStyle.copy(fontWeight = FontWeight.Bold), color = colors.accent)
        }
        Slider(
            value = mtu,
            onValueChange = onMtuChange,
            valueRange = 1280f..1500f,
            steps = 10,
            colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent)
        )
        Text("1400 safe over TLS. Default is 1400 bytes.", style = DeepCurrentTheme.typography.labelMedium, color = colors.textLo)
    }

    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

    // DNS Mode segmented: Through tunnel · Split · Direct
    Text("DNS Mode", style = DeepCurrentTheme.typography.labelMedium, color = colors.textMid)
    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
    val dnsModes = listOf("Through tunnel", "Split", "Direct")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DeepCurrentTheme.radius.card)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
            .padding(4.dp)
    ) {
        dnsModes.forEach { mode ->
            val isSelected = dnsMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(DeepCurrentTheme.radius.chip)
                    .background(if (isSelected) colors.accent else Color.Transparent)
                    .clickable { onDnsModeChange(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode,
                    style = DeepCurrentTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) colors.bgBase else colors.textMid
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

    // Custom DNS Fields
    Row(modifier = Modifier.fillMaxWidth()) {
        EditorTextField(
            value = dnsPrimary,
            onValueChange = onDnsPrimaryChange,
            label = "Primary DNS",
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(DeepCurrentTheme.spacing.x3))
        EditorTextField(
            value = dnsSecondary,
            onValueChange = onDnsSecondaryChange,
            label = "Secondary DNS",
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

    // Keep-alive toggle + interval stepper
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Keep-alive", style = DeepCurrentTheme.typography.bodyLarge, color = colors.textHi)
        Switch(
            checked = keepAlive,
            onCheckedChange = onToggleKeepAlive,
            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.bgElevated)
        )
    }

    if (keepAlive) {
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(10, 30, 60).forEach { interval ->
                val isSelected = keepAliveInterval == interval
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(DeepCurrentTheme.radius.chip)
                        .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.bgSurface)
                        .border(1.dp, if (isSelected) colors.accent else colors.borderSubtle, DeepCurrentTheme.radius.chip)
                        .clickable { onKeepAliveIntervalChange(interval) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${interval}s",
                        style = DeepCurrentMonoStyle.copy(fontSize = 12.sp),
                        color = if (isSelected) colors.accent else colors.textMid
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

    // Log Level Segmented: Error · Warn · Info · Debug
    Text("Log Level", style = DeepCurrentTheme.typography.labelMedium, color = colors.textMid)
    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
    val logLevels = listOf("Error", "Warn", "Info", "Debug")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DeepCurrentTheme.radius.card)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
            .padding(4.dp)
    ) {
        logLevels.forEach { level ->
            val isSelected = logLevel == level
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(DeepCurrentTheme.radius.chip)
                    .background(if (isSelected) colors.accent else Color.Transparent)
                    .clickable { onLogLevelChange(level) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level,
                    style = DeepCurrentTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) colors.bgBase else colors.textMid
                )
            }
        }
    }
}

@Composable
private fun HeadersTab(
    headers: List<Pair<String, String>>,
    onAddHeader: (String, String) -> Unit,
    onRemoveHeader: (Int) -> Unit
) {
    val colors = DeepCurrentTheme.colors
    val presets = listOf("User-Agent", "X-Online-Host", "X-Forwarded-For", "Host", "Connection")

    Text("Common Header Presets", style = DeepCurrentTheme.typography.labelMedium, color = colors.textMid)
    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
    val presetScroll = rememberScrollState()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(presetScroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { preset ->
            Box(
                modifier = Modifier
                    .clip(DeepCurrentTheme.radius.chip)
                    .background(colors.bgSurface)
                    .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.chip)
                    .clickable { onAddHeader(preset, "") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = "+ $preset", style = DeepCurrentMonoStyle.copy(fontSize = 11.sp), color = colors.accent)
            }
        }
    }

    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

    headers.forEachIndexed { index, (key, value) ->
        var currentKey by remember(key) { mutableStateOf(key) }
        var currentValue by remember(value) { mutableStateOf(value) }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorTextField(
                value = currentKey,
                onValueChange = { currentKey = it },
                label = "Header Key",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            EditorTextField(
                value = currentValue,
                onValueChange = { currentValue = it },
                label = "Value",
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onRemoveHeader(index) }) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = colors.err)
            }
        }
    }

    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))
    OutlinedButton(
        onClick = { onAddHeader("X-Custom-Header", "value") },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
        shape = DeepCurrentTheme.radius.pill
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add Custom Header")
    }
}

@Composable
private fun SplitTunnelTab(
    installedApps: List<AppInfoItem>,
    excludedPackages: List<String>,
    onToggleApp: (String) -> Unit
) {
    val colors = DeepCurrentTheme.colors
    var appFilter by remember { mutableStateOf("") }

    Text(
        text = "Per-App Split Tunneling",
        style = DeepCurrentTheme.typography.titleMedium,
        color = colors.textHi
    )
    Text(
        text = "Checked apps will bypass the VPN tunnel and connect directly.",
        style = DeepCurrentTheme.typography.bodyMedium,
        color = colors.textLo
    )

    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

    EditorTextField(
        value = appFilter,
        onValueChange = { appFilter = it },
        label = "Filter installed apps..."
    )

    Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

    val filtered = installedApps.filter {
        appFilter.isBlank() || it.appName.contains(appFilter, ignoreCase = true) || it.packageName.contains(appFilter, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        filtered.take(60).forEach { app ->
            val isExcluded = excludedPackages.contains(app.packageName)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DeepCurrentTheme.radius.chip)
                    .background(colors.bgSurface)
                    .clickable { onToggleApp(app.packageName) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isExcluded,
                    onCheckedChange = { onToggleApp(app.packageName) },
                    colors = CheckboxDefaults.colors(checkedColor = colors.accent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = app.appName, style = DeepCurrentTheme.typography.bodyLarge, color = colors.textHi, maxLines = 1)
                    Text(text = app.packageName, style = DeepCurrentMonoStyle.copy(fontSize = 11.sp, color = colors.textLo), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    helperText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    val colors = DeepCurrentTheme.colors

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = DeepCurrentTheme.typography.labelMedium) },
            singleLine = singleLine,
            minLines = minLines,
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .clip(DeepCurrentTheme.radius.card)
                .background(colors.bgSurface),
            textStyle = DeepCurrentMonoStyle.copy(fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.borderSubtle,
                focusedLabelColor = colors.accent,
                unfocusedLabelColor = colors.textMid,
                focusedContainerColor = colors.bgSurface,
                unfocusedContainerColor = colors.bgSurface,
                cursorColor = colors.accent,
                errorBorderColor = colors.err,
                errorLabelColor = colors.err
            ),
            shape = DeepCurrentTheme.radius.card
        )
        if (helperText != null) {
            Text(
                text = helperText,
                style = DeepCurrentTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = if (isError) colors.err else colors.textLo,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
    }
}
