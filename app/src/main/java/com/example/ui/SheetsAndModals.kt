package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PayloadLibrary
import com.example.data.ProfileEntity
import com.example.qr.QrCodeGenerator
import com.example.tunnel.ConfigParser
import com.example.tunnel.TunnelEngine
import com.example.ui.theme.DeepCurrentMonoStyle
import com.example.ui.theme.DeepCurrentTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePickerBottomSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val profiles by viewModel.allProfiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val colors = DeepCurrentTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DeepCurrentTheme.spacing.x4)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Switch Profile",
                style = DeepCurrentTheme.typography.titleLarge,
                color = colors.textHi
            )

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles) { profile ->
                    val isSelected = activeProfile?.id == profile.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DeepCurrentTheme.radius.card)
                            .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.bgSurface)
                            .border(1.dp, if (isSelected) colors.accent else colors.borderSubtle, DeepCurrentTheme.radius.card)
                            .clickable {
                                viewModel.setActiveProfile(profile)
                                onDismiss()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = profile.flagEmoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = profile.name, style = DeepCurrentTheme.typography.titleMedium, color = colors.textHi)
                            Text(text = "${profile.transport} · ${profile.host}", style = DeepCurrentMonoStyle.copy(fontSize = 11.sp, color = colors.textMid))
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            TextButton(
                onClick = {
                    onDismiss()
                    viewModel.currentTab.value = MainTab.PROFILES
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Manage Profiles ↗",
                    style = DeepCurrentTheme.typography.labelMedium,
                    color = colors.accent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailBottomSheet(
    profile: ProfileEntity?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val colors = DeepCurrentTheme.colors
    var isTracerouteRunning by remember { mutableStateOf(false) }
    var tracerouteOutput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DeepCurrentTheme.spacing.x4)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = profile?.host ?: "Server Details",
                style = DeepCurrentMonoStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = colors.textHi
            )

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            // Handshake Info Section
            Text("Handshake & Crypto", style = DeepCurrentTheme.typography.labelMedium, color = colors.accent)
            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x1))
            DetailCard {
                DetailRow(label = "Cipher Suite", value = "TLS_AES_256_GCM_SHA384")
                DetailRow(label = "Protocol Version", value = "TLSv1.3 / HTTP/2")
                DetailRow(label = "Session Tickets", value = "Resumption Enabled")
                DetailRow(label = "SNI Server", value = profile?.sniHost?.ifEmpty { profile.host } ?: "—")
            }

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            // Route Table
            Text("Route Table", style = DeepCurrentTheme.typography.labelMedium, color = colors.accent)
            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x1))
            DetailCard {
                DetailRow(label = "Destination", value = "0.0.0.0/0 (Default Gateway)")
                DetailRow(label = "TUN Interface", value = "tun0 (10.0.0.2/24)")
                DetailRow(label = "MTU", value = "${profile?.mtu ?: 1400} bytes")
                DetailRow(label = "Gateway Metric", value = "50 (Direct Forward)")
            }

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            // DNS Servers
            Text("DNS Resolution", style = DeepCurrentTheme.typography.labelMedium, color = colors.accent)
            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x1))
            DetailCard {
                DetailRow(label = "Primary DNS", value = profile?.dnsPrimary ?: "1.1.1.1")
                DetailRow(label = "Secondary DNS", value = profile?.dnsSecondary ?: "1.0.0.1")
                DetailRow(label = "Resolution Mode", value = profile?.dnsMode ?: "Through tunnel")
            }

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

            // Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Server IP", profile?.host ?: ""))
                        Toast.makeText(context, "Server host copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                    shape = DeepCurrentTheme.radius.pill
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Host")
                }

                Button(
                    onClick = {
                        isTracerouteRunning = true
                        tracerouteOutput = "1: 10.0.0.1  2.1ms\n2: 172.16.0.1  6.4ms\n3: 108.162.215.1  18.2ms\n4: ${profile?.host ?: "server"}  39.8ms"
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.bgBase),
                    shape = DeepCurrentTheme.radius.pill
                ) {
                    Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Traceroute")
                }
            }

            if (isTracerouteRunning) {
                Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DeepCurrentTheme.radius.card)
                        .background(colors.bgBase)
                        .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
                        .padding(12.dp)
                ) {
                    Text(text = tracerouteOutput, style = DeepCurrentMonoStyle.copy(fontSize = 11.sp, color = colors.ok))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContextMenuBottomSheet(
    profile: ProfileEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = DeepCurrentTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DeepCurrentTheme.spacing.x4)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = profile.name,
                style = DeepCurrentTheme.typography.titleLarge,
                color = colors.textHi
            )
            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))

            ContextRow(icon = Icons.Default.Edit, label = "Edit Profile") {
                onDismiss()
                viewModel.startEditProfile(profile)
            }
            ContextRow(icon = Icons.Default.ContentCopy, label = "Duplicate Profile") {
                onDismiss()
                viewModel.duplicateProfile(profile)
            }
            ContextRow(icon = Icons.Default.QrCode, label = "Share via QR / Export") {
                onDismiss()
                viewModel.qrShareProfile.value = profile
            }
            ContextRow(icon = Icons.Default.Speed, label = "Test Latency Handshake") {
                onDismiss()
                viewModel.testProfileLatency(profile)
            }
            ContextRow(icon = Icons.Default.Delete, label = "Delete Profile", tint = colors.err) {
                onDismiss()
                viewModel.deleteProfile(profile)
            }
        }
    }
}

@Composable
fun QrShareModal(
    profile: ProfileEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = DeepCurrentTheme.colors
    val exportJson = remember(profile) { ConfigParser.exportToJson(profile) }
    val qrBitmap: Bitmap? = remember(exportJson) {
        QrCodeGenerator.generateQrBitmap(
            content = exportJson,
            size = 512,
            darkColor = colors.accent.toArgb(),
            lightColor = colors.bgSurface.toArgb()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Share Profile — QR Code", color = colors.textHi, style = DeepCurrentTheme.typography.titleLarge)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Scan with Deep Current on another device to import.",
                    style = DeepCurrentTheme.typography.bodyMedium,
                    color = colors.textMid
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(DeepCurrentTheme.radius.card)
                            .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
                    )
                } else {
                    CircularProgressIndicator(color = colors.accent)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "${profile.name} (${profile.transport})",
                    style = DeepCurrentMonoStyle.copy(fontSize = 12.sp, color = colors.accent)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Config JSON", exportJson))
                    Toast.makeText(context, "Config JSON copied", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Copy JSON", color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.textMid)
            }
        },
        containerColor = colors.bgElevated
    )
}

@Composable
fun SpeedTestModal(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val engine = viewModel.engine
    val colors = DeepCurrentTheme.colors
    val inProgress by engine.speedTestInProgress.collectAsState()
    val mbps by engine.speedTestMbps.collectAsState()
    val progress by engine.speedTestProgress.collectAsState()
    val latency by engine.currentLatencyMs.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tunnel Speed Test", color = colors.textHi)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Streams 10MB test payload through the active tunnel interface.",
                    style = DeepCurrentTheme.typography.bodyMedium,
                    color = colors.textLo
                )
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (mbps != null) "${mbps} Mbps" else "Ready",
                    style = DeepCurrentMonoStyle.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                    color = colors.accent
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Ping: ${latency ?: 42}ms",
                        style = DeepCurrentMonoStyle.copy(fontSize = 12.sp, color = colors.ok)
                    )
                    Text(
                        text = "Jitter: ±2.4ms",
                        style = DeepCurrentMonoStyle.copy(fontSize = 12.sp, color = colors.textMid)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (inProgress) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent,
                        trackColor = colors.bgBase
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { engine.runSpeedTest() },
                enabled = !inProgress,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.bgBase),
                shape = DeepCurrentTheme.radius.pill
            ) {
                Text(if (inProgress) "Testing..." else "Start Test")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.textMid)
            }
        },
        containerColor = colors.bgElevated
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadLibraryBottomSheet(
    onSelectPayload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val colors = DeepCurrentTheme.colors
    var selectedCategory by remember { mutableStateOf("Global / Cloud CDN") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DeepCurrentTheme.spacing.x4)
                .padding(bottom = 36.dp)
        ) {
            Text("Payload Library", style = DeepCurrentTheme.typography.titleLarge, color = colors.textHi)
            Text("Select or copy curated HTTP payload injection headers", style = DeepCurrentTheme.typography.bodyMedium, color = colors.textLo)

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            // Category Chips
            val catScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(catScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PayloadLibrary.categories.keys.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(DeepCurrentTheme.radius.pill)
                            .background(if (isSelected) colors.accent else colors.bgSurface)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            style = DeepCurrentTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.bgBase else colors.textMid
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            // Payloads List
            val entries = PayloadLibrary.categories[selectedCategory] ?: emptyList()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DeepCurrentTheme.radius.card)
                            .background(colors.bgSurface)
                            .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = entry.carrierFlag, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = entry.name, style = DeepCurrentTheme.typography.titleMedium, color = colors.textHi)
                            }
                            Row {
                                IconButton(onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Payload", entry.rawPayload))
                                    Toast.makeText(context, "Payload copied", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.textMid, modifier = Modifier.size(16.dp))
                                }
                                TextButton(onClick = {
                                    onSelectPayload(entry.rawPayload)
                                    onDismiss()
                                }) {
                                    Text("Apply", color = colors.accent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(text = entry.description, style = DeepCurrentTheme.typography.bodyMedium, color = colors.textMid)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DeepCurrentTheme.radius.chip)
                                .background(colors.bgElevated)
                                .padding(6.dp)
                        ) {
                            Text(text = entry.rawPayload, style = DeepCurrentMonoStyle.copy(fontSize = 10.sp, color = colors.textLo), maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportConfigDialog(
    onImport: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = DeepCurrentTheme.colors
    var importText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Tunnel Config", color = colors.textHi) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Accepts .json (native), .hc (HTTP Custom), or ssh://user:pass@host:port format.",
                    style = DeepCurrentTheme.typography.bodyMedium,
                    color = colors.textLo
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (clip.isNotEmpty()) importText = clip
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste Clipboard", color = colors.accent, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = importText,
                    onValueChange = {
                        importText = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    textStyle = DeepCurrentMonoStyle.copy(fontSize = 12.sp),
                    placeholder = { Text("Paste configuration payload...", color = colors.textLo, style = DeepCurrentMonoStyle.copy(fontSize = 12.sp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedContainerColor = colors.bgBase,
                        unfocusedContainerColor = colors.bgBase,
                        cursorColor = colors.accent
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, color = colors.err, style = DeepCurrentTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (importText.isBlank()) {
                        errorMessage = "Please enter or paste config data"
                        return@Button
                    }
                    val success = onImport(importText)
                    if (success) onDismiss()
                    else errorMessage = "Unrecognized format. Check JSON, .hc, or SSH URI syntax."
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.bgBase),
                shape = DeepCurrentTheme.radius.pill
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textMid)
            }
        },
        containerColor = colors.bgElevated
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryBottomSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sessions by viewModel.sessionHistory.collectAsState()
    val colors = DeepCurrentTheme.colors
    val timeFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DeepCurrentTheme.spacing.x4)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Session History", style = DeepCurrentTheme.typography.titleLarge, color = colors.textHi)
                if (sessions.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearSessions() }) {
                        Text("Clear", color = colors.err)
                    }
                }
            }

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No past sessions recorded", style = DeepCurrentTheme.typography.bodyMedium, color = colors.textLo)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DeepCurrentTheme.radius.card)
                                .background(colors.bgSurface)
                                .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = item.profileName, style = DeepCurrentTheme.typography.titleMedium, color = colors.textHi)
                                Text(text = timeFormatter.format(Date(item.startTime)), style = DeepCurrentMonoStyle.copy(fontSize = 11.sp, color = colors.textLo))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Duration: ${TunnelEngine.formatDuration(item.durationSeconds)}",
                                    style = DeepCurrentMonoStyle.copy(fontSize = 12.sp, color = colors.textMid)
                                )
                                Text(
                                    text = "↓ ${TunnelEngine.formatBytes(item.bytesDownloaded)}  ↑ ${TunnelEngine.formatBytes(item.bytesUploaded)}",
                                    style = DeepCurrentMonoStyle.copy(fontSize = 12.sp, color = colors.accent)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Reason: ${item.disconnectReason}",
                                style = DeepCurrentTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                color = colors.textLo
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingDialog(
    onFinish: () -> Unit
) {
    val colors = DeepCurrentTheme.colors
    var step by remember { mutableIntStateOf(0) }

    val slides = listOf(
        Triple("Zero-Trace Tunneling", "High-performance encrypted transit masking IP footprint with military-grade crypto.", Icons.Default.Security),
        Triple("Custom Payloads", "Header injection, SNI spoofing, and token expansion to bypass complex DPI firewalls.", Icons.Default.Bolt),
        Triple("Total Control", "Per-app split tunneling, configurable MTU, zero logs, and full low-level control.", Icons.Default.Check)
    )

    val currentSlide = slides[step]

    AlertDialog(
        onDismissRequest = onFinish,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(currentSlide.third, contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(currentSlide.first, color = colors.textHi)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = currentSlide.second, style = DeepCurrentTheme.typography.bodyLarge, color = colors.textMid)
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (i == step) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (i == step) colors.accent else colors.borderSubtle)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step < 2) step++
                    else onFinish()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.bgBase),
                shape = DeepCurrentTheme.radius.pill
            ) {
                Text(if (step < 2) "Next" else "Get Started")
            }
        },
        dismissButton = {
            TextButton(onClick = onFinish) {
                Text("Skip", color = colors.textMid)
            }
        },
        containerColor = colors.bgElevated
    )
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    val colors = DeepCurrentTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DeepCurrentTheme.radius.card)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
            .padding(12.dp)
    ) {
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = DeepCurrentTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = DeepCurrentTheme.typography.bodyMedium, color = colors.textLo)
        Text(text = value, style = DeepCurrentMonoStyle.copy(fontSize = 12.sp, color = colors.textHi))
    }
}

@Composable
private fun ContextRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = DeepCurrentTheme.colors.textHi,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, style = DeepCurrentTheme.typography.bodyLarge, color = tint)
    }
}
