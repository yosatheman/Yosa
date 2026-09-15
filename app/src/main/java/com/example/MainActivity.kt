package com.example

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HomeScreen
import com.example.ui.ImportConfigDialog
import com.example.ui.LogsScreen
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.OnboardingDialog
import com.example.ui.PayloadLibraryBottomSheet
import com.example.ui.ProfileContextMenuBottomSheet
import com.example.ui.ProfileEditorScreen
import com.example.ui.ProfilePickerBottomSheet
import com.example.ui.ProfilesScreen
import com.example.ui.QrShareModal
import com.example.ui.ServerDetailBottomSheet
import com.example.ui.SessionHistoryBottomSheet
import com.example.ui.SettingsScreen
import com.example.ui.SpeedTestModal
import com.example.ui.theme.DeepCurrentTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val accentChoice by viewModel.accentChoice.collectAsState()
            val fontScale by viewModel.fontScale.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()

            val isDark = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            DeepCurrentTheme(
                accent = accentChoice,
                fontScale = fontScale,
                isDark = isDark
            ) {
                DeepCurrentMainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DeepCurrentMainContent(viewModel: MainViewModel) {
    val colors = DeepCurrentTheme.colors
    val currentTab by viewModel.currentTab.collectAsState()
    val editingProfile by viewModel.editingProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    // Request VPN permission launcher if needed
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle VPN approval
    }

    // Modal state
    val isProfilePickerOpen by viewModel.isProfilePickerOpen.collectAsState()
    val isServerDetailOpen by viewModel.isServerDetailOpen.collectAsState()
    val contextMenuProfile by viewModel.profileContextMenuTarget.collectAsState()
    val qrShareProfile by viewModel.qrShareProfile.collectAsState()
    val isSpeedTestOpen by viewModel.isSpeedTestOpen.collectAsState()
    val isPayloadLibraryOpen by viewModel.isPayloadLibraryOpen.collectAsState()
    val isImportConfigOpen by viewModel.isImportConfigOpen.collectAsState()
    val isSessionHistoryOpen by viewModel.isSessionHistoryOpen.collectAsState()
    val showDiscardConfirm by viewModel.showDiscardConfirm.collectAsState()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val logs by viewModel.filteredLogs.collectAsState()

    // Unread error log dot indicator
    var hasUnreadErrors by remember { mutableStateOf(false) }
    LaunchedEffect(logs) {
        val hasRecentErr = logs.any { it.level == "ERR" }
        if (hasRecentErr && currentTab != MainTab.LOGS) {
            hasUnreadErrors = true
        }
    }

    LaunchedEffect(currentTab) {
        if (currentTab == MainTab.LOGS) {
            hasUnreadErrors = false
        }
    }

    // Snackbar notifications
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.snackbarMessage.value = null
        }
    }

    // Back handler when in profile editor
    BackHandler(enabled = editingProfile != null) {
        viewModel.showDiscardConfirm.value = true
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase),
        containerColor = colors.bgBase,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (editingProfile == null) {
                DeepCurrentBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { tab -> viewModel.currentTab.value = tab },
                    hasUnreadErrors = hasUnreadErrors
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (editingProfile != null) {
                ProfileEditorScreen(
                    initialProfile = editingProfile!!,
                    viewModel = viewModel,
                    onBack = { viewModel.showDiscardConfirm.value = true }
                )
            } else {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tabTransition"
                ) { tab ->
                    when (tab) {
                        MainTab.HOME -> HomeScreen(viewModel = viewModel)
                        MainTab.PROFILES -> ProfilesScreen(viewModel = viewModel)
                        MainTab.LOGS -> LogsScreen(viewModel = viewModel)
                        MainTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Sheets & Dialogs
    if (isProfilePickerOpen) {
        ProfilePickerBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.isProfilePickerOpen.value = false }
        )
    }

    if (isServerDetailOpen) {
        ServerDetailBottomSheet(
            profile = activeProfile,
            onDismiss = { viewModel.isServerDetailOpen.value = false }
        )
    }

    if (contextMenuProfile != null) {
        ProfileContextMenuBottomSheet(
            profile = contextMenuProfile!!,
            viewModel = viewModel,
            onDismiss = { viewModel.profileContextMenuTarget.value = null }
        )
    }

    if (qrShareProfile != null) {
        QrShareModal(
            profile = qrShareProfile!!,
            onDismiss = { viewModel.qrShareProfile.value = null }
        )
    }

    if (isSpeedTestOpen) {
        SpeedTestModal(
            viewModel = viewModel,
            onDismiss = { viewModel.isSpeedTestOpen.value = false }
        )
    }

    if (isPayloadLibraryOpen) {
        PayloadLibraryBottomSheet(
            onSelectPayload = { payload ->
                viewModel.editingProfile.value?.let { prof ->
                    viewModel.editingProfile.value = prof.copy(
                        usePayload = true,
                        rawPayload = payload
                    )
                }
            },
            onDismiss = { viewModel.isPayloadLibraryOpen.value = false }
        )
    }

    if (isImportConfigOpen) {
        ImportConfigDialog(
            onImport = { raw -> viewModel.importConfig(raw) },
            onDismiss = { viewModel.isImportConfigOpen.value = false }
        )
    }

    if (isSessionHistoryOpen) {
        SessionHistoryBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.isSessionHistoryOpen.value = false }
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.showDiscardConfirm.value = false },
            title = { Text("Discard Changes?", color = colors.textHi) },
            text = { Text("Any unsaved modifications to this profile will be lost.", color = colors.textMid) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.showDiscardConfirm.value = false
                        viewModel.editingProfile.value = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.err)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDiscardConfirm.value = false }) {
                    Text("Keep Editing", color = colors.accent)
                }
            },
            containerColor = colors.bgElevated
        )
    }

    if (!onboardingCompleted) {
        OnboardingDialog(
            onFinish = { viewModel.onboardingCompleted.value = true }
        )
    }
}

@Composable
fun DeepCurrentBottomNavigation(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    hasUnreadErrors: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = DeepCurrentTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(colors.bgSurface)
            .border(width = 1.dp, color = colors.borderSubtle)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            label = "Home",
            icon = Icons.Default.Home,
            isSelected = currentTab == MainTab.HOME,
            hasBadge = false,
            onClick = { onTabSelected(MainTab.HOME) }
        )
        BottomNavItem(
            label = "Profiles",
            icon = Icons.Default.Dns,
            isSelected = currentTab == MainTab.PROFILES,
            hasBadge = false,
            onClick = { onTabSelected(MainTab.PROFILES) }
        )
        BottomNavItem(
            label = "Logs",
            icon = Icons.Default.ListAlt,
            isSelected = currentTab == MainTab.LOGS,
            hasBadge = hasUnreadErrors,
            onClick = { onTabSelected(MainTab.LOGS) }
        )
        BottomNavItem(
            label = "Settings",
            icon = Icons.Default.Settings,
            isSelected = currentTab == MainTab.SETTINGS,
            hasBadge = false,
            onClick = { onTabSelected(MainTab.SETTINGS) }
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    hasBadge: Boolean,
    onClick: () -> Unit
) {
    val colors = DeepCurrentTheme.colors
    val tint = if (isSelected) colors.accent else colors.textLo
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
    val yOffset = if (isSelected) (-2).dp else 0.dp

    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("nav_tab_${label.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.offset(y = yOffset)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
                if (hasBadge) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.err)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = DeepCurrentTheme.typography.titleMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = fontWeight
                ),
                color = tint
            )
        }
    }
}
