package com.lucid.app.ui.screens

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lucid.app.data.BriefingItem
import com.lucid.app.data.Priority
import com.lucid.app.ui.theme.ImportantColor
import com.lucid.app.ui.theme.LowPriorityColor
import com.lucid.app.ui.theme.UrgentColor
import java.text.SimpleDateFormat
import java.util.*

/**
 * The Communication Synthesizer - The Briefing
 *
 * "3 urgent matters from family. 4 newsletters (summarized). 12 low-priority alerts (archived)."
 *
 * It doesn't buzz you. It waits.
 * When you decide to check, it presents a synthesis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefingScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotificationAccess by remember { mutableStateOf(false) }

    // Check notification access
    LaunchedEffect(Unit) {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        hasNotificationAccess = enabledListeners?.contains(context.packageName) == true
    }

    // Demo briefing data - shows what the feature will look like
    val briefingItems = remember {
        listOf(
            BriefingItem(
                id = "1",
                source = "Messages",
                sender = "Mom",
                preview = "Don't forget dinner on Sunday!",
                timestamp = System.currentTimeMillis() - 3600000,
                priority = Priority.IMPORTANT
            ),
            BriefingItem(
                id = "2",
                source = "Email",
                sender = "Work",
                preview = "Meeting rescheduled to 3pm",
                timestamp = System.currentTimeMillis() - 5400000,
                priority = Priority.NORMAL
            ),
            BriefingItem(
                id = "3",
                source = "Messages",
                sender = "Alex",
                preview = "Did you see the game last night?",
                timestamp = System.currentTimeMillis() - 7200000,
                priority = Priority.NORMAL
            ),
            BriefingItem(
                id = "4",
                source = "Newsletter",
                sender = "Tech Weekly",
                preview = "5 articles summarized",
                timestamp = System.currentTimeMillis() - 86400000,
                priority = Priority.LOW
            ),
            BriefingItem(
                id = "5",
                source = "Promotions",
                sender = "Store",
                preview = "20% off this weekend only",
                timestamp = System.currentTimeMillis() - 172800000,
                priority = Priority.LOW
            )
        )
    }

    val groupedItems = briefingItems.groupBy { it.priority }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Briefing",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Notification access notice
            if (!hasNotificationAccess) {
                item {
                    NotificationAccessCard(
                        onRequestAccess = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Text(
                        text = "Preview Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Summary header
            item {
                BriefingSummary(
                    urgent = groupedItems[Priority.URGENT]?.size ?: 0,
                    important = groupedItems[Priority.IMPORTANT]?.size ?: 0,
                    normal = groupedItems[Priority.NORMAL]?.size ?: 0,
                    low = groupedItems[Priority.LOW]?.size ?: 0
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Urgent items
            groupedItems[Priority.URGENT]?.let { items ->
                if (items.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Urgent",
                            color = UrgentColor
                        )
                    }
                    items(items) { item ->
                        BriefingCard(item = item)
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            // Important items
            groupedItems[Priority.IMPORTANT]?.let { items ->
                if (items.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Important",
                            color = ImportantColor
                        )
                    }
                    items(items) { item ->
                        BriefingCard(item = item)
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            // Normal items
            groupedItems[Priority.NORMAL]?.let { items ->
                if (items.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Normal",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(items) { item ->
                        BriefingCard(item = item)
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            // Low priority (collapsed summary)
            groupedItems[Priority.LOW]?.let { items ->
                if (items.isNotEmpty()) {
                    item {
                        LowPrioritySummary(count = items.size)
                    }
                }
            }

            // Empty state
            if (briefingItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Silence.",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nothing demands your attention.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefingSummary(
    urgent: Int,
    important: Int,
    normal: Int,
    low: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val total = urgent + important + normal + low
        Text(
            text = buildString {
                if (urgent > 0) append("$urgent urgent. ")
                if (important > 0) append("$important important. ")
                if (normal > 0) append("$normal normal. ")
                if (low > 0) append("$low archived.")
                if (total == 0) append("Your inbox is clear.")
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BriefingCard(
    item: BriefingItem,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (item.source) {
        "Messages" -> Icons.Outlined.Message
        "Email" -> Icons.Outlined.Email
        else -> Icons.Outlined.Notifications
    }

    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.sender,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timeFormat.format(Date(item.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LowPrioritySummary(count: Int) {
    Text(
        text = "$count low-priority items archived",
        style = MaterialTheme.typography.bodySmall,
        color = LowPriorityColor,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * Card prompting user to enable notification access
 */
@Composable
private fun NotificationAccessCard(
    onRequestAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Enable Notification Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "LUCID can synthesize your notifications into a calm briefing. Grant access to see your real messages, emails, and alerts organized by priority.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onRequestAccess,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text("Grant Access")
            }
        }
    }
}
