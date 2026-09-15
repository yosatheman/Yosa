package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DeepCurrentApp
import com.example.ui.theme.AccentChoice
import com.example.ui.theme.DeepCurrentMonoStyle
import com.example.ui.theme.DeepCurrentTheme
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = DeepCurrentTheme.colors
    val scrollState = rememberScrollState()

    val currentAccent by viewModel.accentChoice.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val engine = viewModel.engine
    val killSwitch by engine.killSwitch.collectAsState()
    val autoReconnect by engine.autoReconnect.collectAsState()

    var showKillSwitchModal by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showCrashLogSheet by remember { mutableStateOf(false) }
    var crashLogContent by remember { mutableStateOf("") }
    var ipv6Protection by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .verticalScroll(scrollState)
            .padding(horizontal = DeepCurrentTheme.spacing.x4)
    ) {
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

        Text(
            text = "Settings",
            style = DeepCurrentTheme.typography.displayLarge.copy(fontSize = 28.sp),
            color = colors.textHi
        )

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

        // Group 1: Appearance
        SettingsSectionHeader(title = "Appearance")
        SettingsCard {
            // Theme Mode
            Text("Theme Mode", style = DeepCurrentTheme.typography.labelMedium, color = colors.textMid)
            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
            val themeModes = listOf("Dark", "Light", "System")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DeepCurrentTheme.radius.chip)
                    .background(colors.bgElevated)
                    .padding(3.dp)
            ) {
                themeModes.forEach { mode ->
                    val isSelected = themeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(DeepCurrentTheme.radius.chip)
                            .background(if (isSelected) colors.accent else Color.Transparent)
                            .clickable { viewModel.themeMode.value = mode }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            style = DeepCurrentTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.bgBase else colors.textMid
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

            // Accent Color Swatches
            Text("Accent Color", style = DeepCurrentTheme.typography.labelMedium, color = colors.textMid)
            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccentChoice.entries.forEach { choice ->
                    val swatchColor = when (choice) {
                        AccentChoice.CYAN -> Color(0xFF22D3EE)
                        AccentChoice.VIOLET -> Color(0xFFA78BFA)
                        AccentChoice.AMBER -> Color(0xFFF59E0B)
                        AccentChoice.ROSE -> Color(0xFFFB7185)
                        AccentChoice.MINT -> Color(0xFF34D399)
                    }
                    val isSelected = currentAccent == choice

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) colors.textHi else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.accentChoice.value = choice },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = colors.bgBase,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

            // Font Scale
            Text("Font Scale", style = DeepCurrentTheme.typography.labelMedium, color = colors.textMid)
            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))
            val fontScales = listOf("Small" to 0.9f, "Default" to 1.0f, "Large" to 1.15f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DeepCurrentTheme.radius.chip)
                    .background(colors.bgElevated)
                    .padding(3.dp)
            ) {
                fontScales.forEach { (label, scale) ->
                    val isSelected = fontScale == scale
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(DeepCurrentTheme.radius.chip)
                            .background(if (isSelected) colors.accent else Color.Transparent)
                            .clickable { viewModel.fontScale.value = scale }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = DeepCurrentTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.bgBase else colors.textMid
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

        // Group 2: Security
        SettingsSectionHeader(title = "Security & Network")
        SettingsCard {
            // Kill Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kill Switch", style = DeepCurrentTheme.typography.titleMedium, color = colors.textHi)
                    Text("Block all traffic when tunnel drops", style = DeepCurrentTheme.typography.bodyMedium, color = colors.textLo)
                }
                Switch(
                    checked = killSwitch,
                    onCheckedChange = { willEnable ->
                        if (willEnable) showKillSwitchModal = true
                        else engine.killSwitch.value = false
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.bgElevated)
                )
            }

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            // IPv6 leak protection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("IPv6 Leak Protection", style = DeepCurrentTheme.typography.titleMedium, color = colors.textHi)
                    Text("Disable system IPv6 routing through unencrypted adapters", style = DeepCurrentTheme.typography.bodyMedium, color = colors.textLo)
                }
                Switch(
                    checked = ipv6Protection,
                    onCheckedChange = { ipv6Protection = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.bgElevated)
                )
            }

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            // System VPN Settings shortcut
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                        try { context.startActivity(intent) } catch (e: Exception) {}
                    }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Android System VPN Settings", style = DeepCurrentTheme.typography.bodyLarge, color = colors.textHi)
                }
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = colors.textMid, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

        // Group 3: Diagnostics & Data
        SettingsSectionHeader(title = "Diagnostics & Data")
        SettingsCard {
            // View Session History
            SettingsRowAction(
                icon = Icons.Default.History,
                label = "View Session History",
                onClick = { viewModel.isSessionHistoryOpen.value = true }
            )

            // Import Config (.hc, .json, SSH URI)
            SettingsRowAction(
                icon = Icons.Default.QrCode,
                label = "Import Config (.hc, .json, SSH URI)",
                onClick = { viewModel.isImportConfigOpen.value = true }
            )

            // View Crash Log
            SettingsRowAction(
                icon = Icons.Default.BugReport,
                label = "View Crash Log",
                onClick = {
                    val crashFile = File(context.filesDir, "crash_log.txt")
                    crashLogContent = if (crashFile.exists()) {
                        crashFile.readText()
                    } else {
                        "No crash logs recorded. Application running normally."
                    }
                    showCrashLogSheet = true
                }
            )

            // Reset App
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetDialog = true }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = colors.err, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Reset Application to Defaults", style = DeepCurrentTheme.typography.bodyLarge, color = colors.err)
            }
        }

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

        // Group 4: About
        SettingsSectionHeader(title = "About")
        SettingsCard {
            Text("Deep Current", style = DeepCurrentTheme.typography.titleLarge, color = colors.textHi)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Deep Current engine v1.4.2 · Build 104", style = DeepCurrentMonoStyle.copy(fontSize = 12.sp, color = colors.textLo))
            Text("Commit: 8f4c2e1 (prod)", style = DeepCurrentMonoStyle.copy(fontSize = 11.sp, color = colors.textLo))

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Documentation ↗",
                    style = DeepCurrentTheme.typography.labelMedium,
                    color = colors.accent,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                        try { context.startActivity(intent) } catch (e: Exception) {}
                    }
                )
                Text(
                    text = "Report Bug ↗",
                    style = DeepCurrentTheme.typography.labelMedium,
                    color = colors.accent,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                        try { context.startActivity(intent) } catch (e: Exception) {}
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }

    // Kill switch modal
    if (showKillSwitchModal) {
        AlertDialog(
            onDismissRequest = { showKillSwitchModal = false },
            title = { Text("Enable Kill Switch?", color = colors.textHi) },
            text = {
                Text(
                    "Kill Switch enforces zero data leaks by dropping all network traffic if the tunnel disconnects unexpectedly.",
                    color = colors.textMid
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        engine.killSwitch.value = true
                        showKillSwitchModal = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accent)
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillSwitchModal = false }) {
                    Text("Cancel", color = colors.textMid)
                }
            },
            containerColor = colors.bgElevated
        )
    }

    // Reset app dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset App State?", color = colors.err) },
            text = { Text("This will delete all custom profiles, clear session history and reset settings to defaults.", color = colors.textMid) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetApp()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.err)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = colors.textMid)
                }
            },
            containerColor = colors.bgElevated
        )
    }

    // Crash Log Modal
    if (showCrashLogSheet) {
        AlertDialog(
            onDismissRequest = { showCrashLogSheet = false },
            title = { Text("Local Crash Log", color = colors.textHi) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(DeepCurrentTheme.radius.card)
                        .background(colors.bgBase)
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = crashLogContent,
                        style = DeepCurrentMonoStyle.copy(fontSize = 11.sp, color = colors.textMid)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCrashLogSheet = false }) {
                    Text("Close", color = colors.accent)
                }
            },
            containerColor = colors.bgElevated
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = DeepCurrentTheme.colors
    Text(
        text = title,
        style = DeepCurrentTheme.typography.titleMedium.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = colors.textMid,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val colors = DeepCurrentTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DeepCurrentTheme.radius.card)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
            .padding(DeepCurrentTheme.spacing.x4)
    ) {
        content()
    }
}

@Composable
private fun SettingsRowAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = DeepCurrentTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, style = DeepCurrentTheme.typography.bodyLarge, color = colors.textHi)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textMid, modifier = Modifier.size(16.dp))
    }
}
