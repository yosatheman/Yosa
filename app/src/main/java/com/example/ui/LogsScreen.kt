package com.example.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LogEntryEntity
import com.example.ui.theme.DeepCurrentMonoStyle
import com.example.ui.theme.DeepCurrentTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = DeepCurrentTheme.colors

    val filteredLogs by viewModel.filteredLogs.collectAsState()
    val activeFilter by viewModel.logFilter.collectAsState()
    val autoScroll by viewModel.autoScrollLogs.collectAsState()

    var isPaused by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isScrolledToBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= filteredLogs.size - 2
        }
    }

    // Auto-scroll when new items arrive if autoScroll is enabled
    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && !isPaused && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    val displayedLogs = remember(filteredLogs, searchQuery, isPaused) {
        val base = if (searchQuery.isBlank()) filteredLogs else filteredLogs.filter {
            it.message.contains(searchQuery, ignoreCase = true) || it.level.contains(searchQuery, ignoreCase = true)
        }
        base
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBase)
            .padding(horizontal = DeepCurrentTheme.spacing.x4)
    ) {
        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

        // Top Header + Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Logs",
                style = DeepCurrentTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = colors.textHi
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Auto scroll toggle
                IconButton(onClick = { viewModel.autoScrollLogs.value = !autoScroll }) {
                    Icon(
                        imageVector = Icons.Default.VerticalAlignBottom,
                        contentDescription = "Auto-scroll",
                        tint = if (autoScroll) colors.accent else colors.textDis
                    )
                }

                // Pause toggle
                IconButton(onClick = { isPaused = !isPaused }) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = if (isPaused) colors.warn else colors.textMid
                    )
                }

                // Share
                IconButton(onClick = {
                    val exportText = displayedLogs.joinToString("\n") {
                        "[${timeFormatter.format(Date(it.timestamp))}] [${it.level}] ${it.message}"
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Deep Current Logs")
                        putExtra(Intent.EXTRA_TEXT, exportText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Logs"))
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = colors.textMid
                    )
                }

                // Clear
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear",
                        tint = colors.err
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))

        // Filter chips: All · Error · Warn · Info · Debug
        val filters = listOf("All", "Error", "Warn", "Info", "Debug")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = activeFilter == filter
                val bg = if (isSelected) colors.accent else colors.bgSurface
                val textColor = if (isSelected) colors.bgBase else colors.textMid

                Box(
                    modifier = Modifier
                        .clip(DeepCurrentTheme.radius.pill)
                        .background(bg)
                        .border(1.dp, if (isSelected) colors.accent else colors.borderSubtle, DeepCurrentTheme.radius.pill)
                        .clickable { viewModel.logFilter.value = filter }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter,
                        style = DeepCurrentTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = textColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))

        // Sticky Search bar under filters
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(DeepCurrentTheme.radius.pill),
            placeholder = {
                Text(
                    text = "Filter log lines in real-time...",
                    style = DeepCurrentMonoStyle.copy(fontSize = 12.sp, color = colors.textLo)
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = colors.textMid, modifier = Modifier.size(16.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = colors.textMid, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            textStyle = DeepCurrentMonoStyle.copy(fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.borderSubtle,
                focusedContainerColor = colors.bgSurface,
                unfocusedContainerColor = colors.bgSurface,
                cursorColor = colors.accent
            ),
            shape = DeepCurrentTheme.radius.pill
        )

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x2))

        // Terminal Output List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(DeepCurrentTheme.radius.card)
                .background(colors.bgSurface)
                .border(1.dp, colors.borderSubtle, DeepCurrentTheme.radius.card)
                .padding(8.dp)
        ) {
            if (displayedLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No logs recorded",
                        style = DeepCurrentMonoStyle.copy(fontSize = 12.sp, color = colors.textLo)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedLogs, key = { it.id }) { log ->
                        LogLineItem(log = log, timeFormatter = timeFormatter)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Logs?", color = colors.textHi) },
            text = { Text("This will erase all recorded events from the database.", color = colors.textMid) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLogs()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.err)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = colors.textMid)
                }
            },
            containerColor = colors.bgElevated
        )
    }
}

@Composable
private fun LogLineItem(
    log: LogEntryEntity,
    timeFormatter: SimpleDateFormat
) {
    val colors = DeepCurrentTheme.colors
    val (badgeBg, badgeText) = when (log.level) {
        "ERR" -> colors.err.copy(alpha = 0.2f) to colors.err
        "WRN" -> colors.warn.copy(alpha = 0.2f) to colors.warn
        "INF" -> colors.accent.copy(alpha = 0.2f) to colors.accent
        "DBG" -> colors.bgElevated to colors.textLo
        else -> colors.bgElevated to colors.textMid
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = timeFormatter.format(Date(log.timestamp)),
            style = DeepCurrentMonoStyle.copy(fontSize = 11.sp),
            color = colors.textLo,
            modifier = Modifier.width(76.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Level badge (3 chars)
        Box(
            modifier = Modifier
                .clip(DeepCurrentTheme.radius.chip)
                .background(badgeBg)
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = log.level,
                style = DeepCurrentMonoStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = badgeText
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Message
        Text(
            text = log.message,
            style = DeepCurrentMonoStyle.copy(fontSize = 12.sp),
            color = colors.textHi,
            modifier = Modifier.weight(1f)
        )
    }
}
