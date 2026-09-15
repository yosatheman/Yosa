package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProfileEntity
import com.example.ui.theme.DeepCurrentMonoStyle
import com.example.ui.theme.DeepCurrentTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProfilesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.filteredProfiles.collectAsState()
    val allProfiles by viewModel.allProfiles.collectAsState()
    val searchQuery by viewModel.profileSearchQuery.collectAsState()
    val testingLatencyId by viewModel.testingLatencyProfileId.collectAsState()
    val killSwitchOn by viewModel.engine.killSwitch.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val colors = DeepCurrentTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBase)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DeepCurrentTheme.spacing.x4)
        ) {
            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x4))

            // Top Title + Count Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Profiles",
                    style = DeepCurrentTheme.typography.displayLarge.copy(fontSize = 28.sp),
                    color = colors.textHi
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(DeepCurrentTheme.radius.pill)
                        .background(colors.bgElevated)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${allProfiles.size}",
                        style = DeepCurrentMonoStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            // Search Field: full width, pill, leading search icon, trailing clear
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.profileSearchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(DeepCurrentTheme.radius.pill)
                    .testTag("search_profiles_input"),
                placeholder = {
                    Text(
                        text = "Search by name, host, or transport...",
                        style = DeepCurrentTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        color = colors.textLo
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textMid,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(onClick = { viewModel.profileSearchQuery.value = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = colors.textMid,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = colors.bgSurface,
                    unfocusedContainerColor = colors.bgSurface,
                    cursorColor = colors.accent
                ),
                shape = DeepCurrentTheme.radius.pill
            )

            Spacer(modifier = Modifier.height(DeepCurrentTheme.spacing.x3))

            // Profile List or Empty State
            if (profiles.isEmpty()) {
                ProfilesEmptyState(
                    searchQuery = searchQuery,
                    onAddClick = { viewModel.startNewProfile() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(DeepCurrentTheme.spacing.x2),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(
                        items = profiles,
                        key = { it.id }
                    ) { profile ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        // Swipe-left: delete
                                        if (killSwitchOn) {
                                            viewModel.deleteConfirmProfile.value = profile
                                        } else {
                                            viewModel.deleteProfile(profile)
                                        }
                                        true
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        // Swipe-right: set as active
                                        viewModel.setActiveProfile(profile)
                                        false
                                    }
                                    SwipeToDismissBoxValue.Settled -> false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val direction = dismissState.dismissDirection
                                val isRight = direction == SwipeToDismissBoxValue.StartToEnd
                                val bg = if (isRight) colors.accent.copy(alpha = 0.25f) else colors.err.copy(alpha = 0.25f)
                                val icon = if (isRight) Icons.Default.Check else Icons.Default.Delete
                                val tint = if (isRight) colors.accent else colors.err

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(DeepCurrentTheme.radius.card)
                                        .background(bg)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = if (isRight) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        ) {
                            ProfileCard(
                                profile = profile,
                                isTestingLatency = testingLatencyId == profile.id,
                                onSelect = { viewModel.setActiveProfile(profile) },
                                onLongPress = { viewModel.profileContextMenuTarget.value = profile },
                                onTestLatency = { viewModel.testProfileLatency(profile) }
                            )
                        }
                    }
                }
            }
        }

        // FAB: Extended "New profile" pill, elevated above nav by 16dp
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = DeepCurrentTheme.spacing.x4)
                .clip(DeepCurrentTheme.radius.pill)
                .background(colors.accent)
                .clickable { viewModel.startNewProfile() }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("new_profile_fab"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New profile",
                tint = colors.bgBase,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New profile",
                style = DeepCurrentTheme.typography.labelMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.bgBase
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileCard(
    profile: ProfileEntity,
    isTestingLatency: Boolean,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
    onTestLatency: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DeepCurrentTheme.colors
    val monogram = extractMonogram(profile.name)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(DeepCurrentTheme.radius.card)
            .background(colors.bgSurface)
            .border(
                width = 1.dp,
                color = if (profile.isActive) colors.accent.copy(alpha = 0.5f) else colors.borderSubtle,
                shape = DeepCurrentTheme.radius.card
            )
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongPress
            )
            .testTag("profile_item_${profile.id}")
    ) {
        // Active Profile: left edge 3dp accent stripe
        if (profile.isActive) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxSize()
                    .background(colors.accent)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: 44dp circle avatar — accent-tinted, monogram
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.bgElevated)
                    .border(
                        width = if (profile.isActive) 1.5.dp else 1.dp,
                        color = if (profile.isActive) colors.accent else colors.borderSubtle,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monogram,
                    style = DeepCurrentTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (profile.isActive) colors.accent else colors.textMid
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center: name (Title 16 w600), host:port (Body 13, text_mid, mono)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.flagEmoji,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = profile.name,
                        style = DeepCurrentTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textHi,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${profile.host}:${profile.port}",
                    style = DeepCurrentMonoStyle.copy(
                        fontSize = 12.sp,
                        color = colors.textMid
                    ),
                    maxLines = 1
                )
            }

            // Right: Latency chip & Transport chip
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Latency chip
                Box(
                    modifier = Modifier
                        .clip(DeepCurrentTheme.radius.chip)
                        .background(colors.bgElevated)
                        .combinedClickable(onClick = onTestLatency)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    if (isTestingLatency) {
                        Text(
                            text = "pinging...",
                            style = DeepCurrentMonoStyle.copy(fontSize = 10.sp),
                            color = colors.accent
                        )
                    } else if (profile.lastTestedLatencyAvg != null) {
                        val latency = profile.lastTestedLatencyAvg
                        val color = if (latency < 80) colors.ok else if (latency < 200) colors.warn else colors.err
                        Text(
                            text = "${latency}ms",
                            style = DeepCurrentMonoStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = color
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Ping",
                                tint = colors.textLo,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "test",
                                style = DeepCurrentMonoStyle.copy(fontSize = 10.sp),
                                color = colors.textLo
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Transport chip
                Box(
                    modifier = Modifier
                        .clip(DeepCurrentTheme.radius.chip)
                        .background(colors.bgTint)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = profile.transport,
                        style = DeepCurrentMonoStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.accent
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilesEmptyState(
    searchQuery: String,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DeepCurrentTheme.colors

    Column(
        modifier = modifier.padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Line-art illustration of server stack
        Canvas(modifier = Modifier.size(72.dp)) {
            val stroke = Stroke(width = 2.dp.toPx())
            // Server 1
            drawRoundRect(
                color = colors.accent,
                topLeft = androidx.compose.ui.geometry.Offset(10.dp.toPx(), 10.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width - 20.dp.toPx(), 18.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                style = stroke
            )
            // Server 2
            drawRoundRect(
                color = colors.accent,
                topLeft = androidx.compose.ui.geometry.Offset(10.dp.toPx(), 34.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width - 20.dp.toPx(), 18.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                style = stroke
            )
            // Indicator dots
            drawCircle(color = colors.ok, radius = 2.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(20.dp.toPx(), 19.dp.toPx()))
            drawCircle(color = colors.ok, radius = 2.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(20.dp.toPx(), 43.dp.toPx()))
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (searchQuery.isNotEmpty()) "No matching profiles" else "No profiles yet",
            style = DeepCurrentTheme.typography.titleMedium.copy(fontSize = 18.sp),
            color = colors.textHi
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (searchQuery.isNotEmpty()) "Try clearing search filters" else "Add or import a server configuration to connect",
            style = DeepCurrentTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            color = colors.textMid
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.bgBase
            ),
            shape = DeepCurrentTheme.radius.pill
        ) {
            Text(
                text = "Add one",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun extractMonogram(name: String): String {
    val words = name.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "DC"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}
